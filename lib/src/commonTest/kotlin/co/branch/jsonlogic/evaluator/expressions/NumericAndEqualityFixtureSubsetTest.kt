package co.branch.jsonlogic.evaluator.expressions

import co.branch.jsonlogic.JsonLogicException
import co.branch.jsonlogic.evaluator.evaluateJson
import co.branch.jsonlogic.fixtures.ErrorOutcome
import co.branch.jsonlogic.fixtures.FixtureLoader
import co.branch.jsonlogic.fixtures.FixtureReplay
import kotlinx.serialization.json.JsonElement
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Replays the fixture cases this workstream's operator table can already run: the arithmetic,
 * comparison and equality operators, `missing` and `missing_some`, and — since the filter admits any
 * rule whose operators it covers, including none — every literal and `var` case as well.
 */
class NumericAndEqualityFixtureSubsetTest {

    private val supportedOps = setOf(
        "+", "-", "*", "/", "%", "min", "max",
        ">", ">=", "<", "<=",
        "==", "!=", "===", "!==",
        "missing", "missing_some",
    )
    private val replay = FixtureReplay { rule, data -> evaluateJson(rule, data, numericAndEqualityExpressions) }

    @Test
    fun valueFixturesPass() {
        val result = replay.runValues(FixtureLoader.loadValueFixtures(), supportedOps)

        assertEquals(119, result.ran, "the arithmetic/comparison/equality/missing/var/literal subset should be 119 cases")
        assertEquals(result.ran, result.passed, result.failures.joinToString("\n"))
    }

    @Test
    fun errorFixturesPass() {
        val result = replay.runErrors(FixtureLoader.loadErrorFixtures(), ::errorOutcome, supportedOps)

        assertEquals(13, result.ran, "the arithmetic/comparison/equality/missing/var/literal subset should be 13 cases")
        assertEquals(result.ran, result.passed, result.failures.joinToString("\n"))
    }

    private fun errorOutcome(rule: JsonElement, data: JsonElement): ErrorOutcome? = try {
        evaluateJson(rule, data, numericAndEqualityExpressions)
        null
    } catch (exception: JsonLogicException) {
        ErrorOutcome(exception.message ?: "", exception.jsonPath)
    }
}
