package co.branch.jsonlogic.evaluator

import co.branch.jsonlogic.JsonLogicException
import co.branch.jsonlogic.fixtures.ErrorOutcome
import co.branch.jsonlogic.fixtures.FixtureLoader
import co.branch.jsonlogic.fixtures.FixtureReplay
import kotlinx.serialization.json.JsonElement
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Replays the fixture cases the array operations complete on their own: the operators of this
 * workstream plus `missing`/`missing_some`, and — since the filter admits any rule whose operators it
 * covers, including none — every literal and `var` case as well.
 *
 * Many array cases in the corpus pair an array operation with a comparison (`{"all": [..., {">=":
 * [...]}]}`), which no filter can admit until those operators are registered too.
 */
class ArrayOperationFixtureSubsetTest {

    private val supportedOps = setOf(
        "map", "filter", "reduce", "all", "some", "none", "merge", "in", "missing", "missing_some",
    )
    private val replay = FixtureReplay { rule, data -> evaluateJson(rule, data, arrayExpressions) }

    @Test
    fun valueFixturesPass() {
        val result = replay.runValues(FixtureLoader.loadValueFixtures(), supportedOps)

        assertEquals(75, result.ran, "the array-operation/missing/var/literal subset should be 75 cases")
        assertEquals(result.ran, result.passed, result.failures.joinToString("\n"))
    }

    @Test
    fun errorFixturesPass() {
        val result = replay.runErrors(FixtureLoader.loadErrorFixtures(), ::errorOutcome, supportedOps)

        assertEquals(15, result.ran, "the array-operation/missing/var/literal subset should be 15 cases")
        assertEquals(result.ran, result.passed, result.failures.joinToString("\n"))
    }

    private fun errorOutcome(rule: JsonElement, data: JsonElement): ErrorOutcome? = try {
        evaluateJson(rule, data, arrayExpressions)
        null
    } catch (exception: JsonLogicException) {
        ErrorOutcome(exception.message ?: "", exception.jsonPath)
    }
}
