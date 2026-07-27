package co.branch.jsonlogic.parity

import kotlinx.serialization.json.JsonElement
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.fail
import co.branch.jsonlogic.JsonLogic as PortedJsonLogic
import io.github.jamsesso.jsonlogic.JsonLogic as OracleJsonLogic

/**
 * A structured differential fuzzer over the operator grammar, beyond the fixture corpus: it generates
 * rule/data pairs from a fixed seed, runs both engines over each, and diffs the outcomes with the same
 * comparator [ParityGateTest] uses.
 *
 * This is not the acceptance gate — [ParityGateTest] is, and the fixture corpus is what the port is
 * contractually held to. This test searches for parity holes the corpus does not reach, so a failure
 * here means one of three things, in this order: a port bug, an upstream quirk nobody has documented
 * yet (in which case it belongs in the tracking doc's deviations and in [FuzzGenerator]'s exclusions),
 * or a generator that produced something the fuzzer should not be generating.
 *
 * The seed is fixed so a failure is reproducible: the reported case number is the number of
 * [FuzzGenerator.nextCase] calls that preceded it, so re-running reproduces the same case.
 */
class ParityFuzzTest {

    @Test
    fun generatedRulesDoNotDiverge() {
        val generator = FuzzGenerator(SEED)
        val oracle = OracleJsonLogic()
        val ported = PortedJsonLogic()

        val counts = mutableMapOf<DisagreementKind, Int>()
        val reports = mutableListOf<String>()
        var returned = 0
        var threw = 0
        var nullPointerText = 0

        val elapsed = measureTimeMillis {
            repeat(CASE_COUNT) { index ->
                val case = generator.nextCase()
                val oracleCase = oracleCaseOf(case.rule, case.data)
                val oracleOutcome = outcomeOf { oracle.apply(oracleCase.rule, oracleCase.data) }
                val portedOutcome = outcomeOf { ported.apply(case.rule, case.data) }
                if (oracleOutcome is Outcome.Threw) threw++ else returned++

                val kind = disagreementBetween(oracleOutcome, portedOutcome) ?: return@repeat
                if (differsOnlyInNullPointerText(oracleOutcome, portedOutcome)) {
                    nullPointerText++
                    return@repeat
                }
                counts[kind] = (counts[kind] ?: 0) + 1
                if (reports.size < REPORTED_DIVERGENCES) {
                    reports += disagreementReport(
                        label = "fuzz case #$index (seed $SEED)",
                        rule = oracleCase.rule,
                        data = "${case.data}",
                        oracle = oracleOutcome,
                        ported = portedOutcome,
                        kind = kind,
                    )
                }
            }
        }

        val divergences = counts.values.sum()
        println(
            "parity fuzz: $CASE_COUNT generated cases in ${elapsed}ms " +
                "(the Java engine returned on $returned, threw on $threw), " +
                "$nullPointerText tolerated NullPointerException texts, $divergences divergences" +
                counts.entries.joinToString("") { "\n  ${it.value}× ${it.key.description}" },
        )
        if (divergences > 0) {
            fail(
                "$divergences of $CASE_COUNT generated cases diverge " +
                    "(showing the first ${reports.size}):\n${reports.joinToString("\n")}",
            )
        }
    }

    /**
     * The single difference this fuzzer tolerates, and only because it is not a property of either
     * engine: both threw `java.lang.NullPointerException` from the same operator on the same input —
     * `substr` renders its first argument without a null check, deliberately in both — but the Java
     * engine's exception sometimes carries a message the JVM synthesizes from *its* bytecode ("the
     * return value of java.util.List.get(int) is null"), which the port's null check cannot produce.
     *
     * Nothing in either engine authors that text, and the JVM does not attach it consistently: it
     * stops appearing once the throw site is hot, so the identical case reports it at the start of a
     * run and reports no message at all later in the same run — a sweep of eight generator seeds
     * produced around 100 of these per 20 000 cases for whichever seeds ran first and none for the
     * rest, following the position in the run rather than the seed. Running the JVM with
     * `-XX:-ShowCodeDetailsInExceptionMessages` removes the text entirely, leaving both engines
     * reporting null.
     *
     * What the engines do author — the type, message and jsonPath of a JsonLogicException — is
     * compared in full, as is every other exception both raise out of the same JVM call, such as the
     * `StringIndexOutOfBoundsException` from `substr`'s range arithmetic. No fixture case reaches
     * this path, so the acceptance gate is unaffected.
     */
    private fun differsOnlyInNullPointerText(oracle: Outcome<Any?>, ported: Outcome<JsonElement>): Boolean {
        val oracleError = (oracle as? Outcome.Threw)?.error ?: return false
        val portedError = (ported as? Outcome.Threw)?.error ?: return false

        return oracleError is NullPointerException && oracleError.javaClass == portedError.javaClass
    }

    private companion object {
        const val SEED = 0x5EEDB9A11L
        const val CASE_COUNT = 20_000
        const val REPORTED_DIVERGENCES = 15
    }
}
