package co.branch.jsonlogic.parity

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
 *
 * One difference is counted and not reported, [NullPointerPairing.SYNTHESIZED_TEXT_ONLY]: a null
 * dereference that both engines make at the same expression, where the JVM described the Java engine's
 * and left the port's bare. Which expression each failure came out of is established first, so a pair
 * that only looks alike is a divergence.
 */
class ParityFuzzTest {

    @Test
    fun generatedRulesDoNotDiverge() {
        val generator = FuzzGenerator(SEED)
        val oracle = OracleJsonLogic()
        val ported = PortedJsonLogic()

        val counts = mutableMapOf<DisagreementKind, Int>()
        val tolerated = mutableMapOf<String, Int>()
        val reports = mutableListOf<String>()
        var returned = 0
        var threw = 0

        val elapsed = measureTimeMillis {
            repeat(CASE_COUNT) { index ->
                val case = generator.nextCase()
                val oracleCase = oracleCaseOf(case.rule, case.data)
                val oracleOutcome = outcomeOf { oracle.apply(oracleCase.rule, oracleCase.data) }
                val portedOutcome = outcomeOf { ported.apply(case.rule, case.data) }
                if (oracleOutcome is Outcome.Threw) threw++ else returned++

                val verdict = fuzzVerdict(oracleOutcome, portedOutcome)
                if (verdict is FuzzVerdict.Tolerated) {
                    tolerated[verdict.origin] = (tolerated[verdict.origin] ?: 0) + 1
                }
                if (verdict !is FuzzVerdict.Diverged) return@repeat

                counts[verdict.kind] = (counts[verdict.kind] ?: 0) + 1
                if (reports.size < REPORTED_DIVERGENCES) {
                    reports += disagreementReport(
                        label = "fuzz case #$index (seed $SEED)",
                        rule = oracleCase.rule,
                        data = "${case.data}",
                        oracle = oracleOutcome,
                        ported = portedOutcome,
                        kind = verdict.kind,
                    )
                }
            }
        }

        val divergences = counts.values.sum()
        println(
            "parity fuzz: $CASE_COUNT generated cases in ${elapsed}ms " +
                "(the Java engine returned on $returned, threw on $threw), " +
                "${tolerated.values.sum()} tolerated NullPointerException texts" +
                describeOrigins(tolerated) + ", $divergences divergences" +
                counts.entries.joinToString("") { "\n  ${it.value}× ${it.key.description}" },
        )
        if (divergences > 0) {
            fail(
                "$divergences of $CASE_COUNT generated cases diverge " +
                    "(showing the first ${reports.size}):\n${reports.joinToString("\n")}",
            )
        }
    }

    private fun describeOrigins(tolerated: Map<String, Int>): String = tolerated.entries
        .sortedByDescending { it.value }
        .takeIf { it.isNotEmpty() }
        ?.joinToString(", ", " (", ")") { "${it.value} at ${it.key}" }
        ?: ""

    private companion object {
        const val SEED = 0x5EEDB9A11L
        const val CASE_COUNT = 20_000
        const val REPORTED_DIVERGENCES = 15
    }
}
