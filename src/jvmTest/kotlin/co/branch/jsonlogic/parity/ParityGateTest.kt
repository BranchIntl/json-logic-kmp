package co.branch.jsonlogic.parity

import co.branch.jsonlogic.fixtures.FixtureLoader
import kotlinx.serialization.json.JsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import co.branch.jsonlogic.JsonLogic as PortedJsonLogic
import io.github.jamsesso.jsonlogic.JsonLogic as OracleJsonLogic

/**
 * The acceptance gate for the port: every fixture case is evaluated by the frozen Java engine and by
 * this library **in the same JVM**, and the two outcomes are diffed case by case. Agreement means
 * both returned values that mean the same thing, or both failed with the same exception type, message
 * and jsonPath; anything else — including one engine throwing where the other returns — is a
 * disagreement, reported with the rule, the data and both outcomes.
 *
 * The corpus is read twice, once per engine, each through its own loader; the two orderings are
 * checked to line up before anything is diffed, so a disagreement can never be an artefact of two
 * different cases being compared.
 */
class ParityGateTest {

    private val oracle = OracleJsonLogic()
    private val ported = PortedJsonLogic()

    @Test
    fun theEnginesAgreeOnEveryValueFixture() {
        val oracleCases = OracleFixtures.valueCases()
        val portedCases = FixtureLoader.loadValueFixtures()
        assertEquals(VALUE_CASE_COUNT, oracleCases.size, "the Java engine's loader read a different corpus")
        assertEquals(VALUE_CASE_COUNT, portedCases.size, "this library's loader read a different corpus")

        val disagreements = mutableListOf<String>()
        var compared = 0

        for ((ordinal, case) in portedCases.withIndex()) {
            val oracleCase = oracleCases[ordinal]
            val label = "value fixture #${case.index} (section \"${case.section}\")"
            assertAligned(label, oracleCase, case.rule, case.data)

            val oracleOutcome = outcomeOf { oracle.apply(oracleCase.rule, oracleCase.data) }
            val portedOutcome = outcomeOf { ported.apply(case.rule, case.data) }
            compared++

            val kind = disagreementBetween(oracleOutcome, portedOutcome) ?: continue
            disagreements += disagreementReport(
                label = label,
                rule = oracleCase.rule,
                data = "${case.data}",
                oracle = oracleOutcome,
                ported = portedOutcome,
                kind = kind,
            )
        }

        assertEquals(VALUE_CASE_COUNT, compared, "not every value fixture was diffed")
        println("parity gate: $compared value cases compared, ${disagreements.size} disagreements")
        if (disagreements.isNotEmpty()) {
            fail("${disagreements.size} of $compared value cases disagree:\n${disagreements.joinToString("\n")}")
        }
    }

    @Test
    fun theEnginesAgreeOnEveryErrorFixture() {
        val oracleCases = OracleFixtures.errorCases()
        val portedCases = FixtureLoader.loadErrorFixtures()
        assertEquals(ERROR_CASE_COUNT, oracleCases.size, "the Java engine's loader read a different corpus")
        assertEquals(ERROR_CASE_COUNT, portedCases.size, "this library's loader read a different corpus")

        val disagreements = mutableListOf<String>()
        val corpusMismatches = mutableListOf<String>()
        var compared = 0

        for ((ordinal, case) in portedCases.withIndex()) {
            val oracleCase = oracleCases[ordinal]
            val label = "error fixture #${case.index}"
            assertAligned(label, oracleCase, case.rule, case.data)

            val oracleOutcome = outcomeOf { oracle.apply(oracleCase.rule, oracleCase.data) }
            val portedOutcome = outcomeOf { ported.apply(case.rule, case.data) }
            compared++

            val kind = disagreementBetween(oracleOutcome, portedOutcome)
            if (kind != null) {
                disagreements += disagreementReport(
                    label = label,
                    rule = oracleCase.rule,
                    data = "${case.data}",
                    oracle = oracleOutcome,
                    ported = portedOutcome,
                    kind = kind,
                )
                continue
            }

            // The engines agree. For an error case that has to mean they agree on failing, and on
            // failing the way the corpus says — otherwise the case is no longer testing what it
            // claims to, whichever engine is at fault.
            if (oracleOutcome !is Outcome.Threw) {
                corpusMismatches += "$label — neither engine threw: rule=${oracleCase.rule} data=${case.data} " +
                    "expected \"${case.expectedMessage}\" at \"${case.expectedJsonPath}\""
                continue
            }

            val signature = oracleErrorSignature(oracleOutcome.error)
            if (signature.message != case.expectedMessage || signature.jsonPath != case.expectedJsonPath) {
                corpusMismatches += "$label — both engines failed alike but not as the corpus says: " +
                    "rule=${oracleCase.rule} data=${case.data} expected \"${case.expectedMessage}\" " +
                    "at \"${case.expectedJsonPath}\", both engines gave $signature"
            }
        }

        assertEquals(ERROR_CASE_COUNT, compared, "not every error fixture was diffed")
        println("parity gate: $compared error cases compared, ${disagreements.size} disagreements")
        if (disagreements.isNotEmpty()) {
            fail("${disagreements.size} of $compared error cases disagree:\n${disagreements.joinToString("\n")}")
        }
        if (corpusMismatches.isNotEmpty()) {
            fail(
                "${corpusMismatches.size} of $compared error cases no longer match the corpus:\n" +
                    corpusMismatches.joinToString("\n"),
            )
        }
    }

    /**
     * Fails the whole gate — not the single case — when the two loaders' ordinals do not describe the
     * same case, since from that point on nothing being compared is meaningful. The rules are
     * compared as Gson trees and the data through the parity comparator itself.
     */
    private fun assertAligned(label: String, oracleCase: OracleCase, rule: JsonElement, data: JsonElement) {
        assertEquals(
            gsonParse("$rule"),
            oracleCase.ruleElement,
            "$label: the two loaders disagree about which rule this case is",
        )
        assertTrue(
            ParityComparator.matches(oracleCase.data, data),
            "$label: the two loaders disagree about this case's data " +
                "(java=${describeJavaValue(oracleCase.data)}, kotlin=$data)",
        )
    }

    private companion object {
        const val VALUE_CASE_COUNT = 289
        const val ERROR_CASE_COUNT = 46
    }
}
