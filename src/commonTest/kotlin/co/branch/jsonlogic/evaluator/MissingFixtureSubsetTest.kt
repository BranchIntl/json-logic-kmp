package co.branch.jsonlogic.evaluator

import co.branch.jsonlogic.JsonLogicException
import co.branch.jsonlogic.fixtures.ErrorOutcome
import co.branch.jsonlogic.fixtures.FixtureLoader
import co.branch.jsonlogic.fixtures.FixtureReplay
import kotlinx.serialization.json.JsonElement
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Replays the fixture cases this workstream's operator table can already run: `missing`,
 * `missing_some`, and — since the filter admits any rule whose operators it covers, including none —
 * every literal and `var` case as well.
 */
class MissingFixtureSubsetTest {

    private val supportedOps = setOf("missing", "missing_some")
    private val replay = FixtureReplay { rule, data -> evaluateJson(rule, data) }

    @Test
    fun valueFixturesPass() {
        val result = replay.runValues(FixtureLoader.loadValueFixtures(), supportedOps)

        assertEquals(51, result.ran, "the missing/missing_some/var/literal subset should be 51 cases")
        assertEquals(result.ran, result.passed, result.failures.joinToString("\n"))
    }

    @Test
    fun errorFixturesPass() {
        val result = replay.runErrors(FixtureLoader.loadErrorFixtures(), ::errorOutcome, supportedOps)

        assertEquals(5, result.ran, "the missing/missing_some/var/literal subset should be 5 cases")
        assertEquals(result.ran, result.passed, result.failures.joinToString("\n"))
    }

    private fun errorOutcome(rule: JsonElement, data: JsonElement): ErrorOutcome? = try {
        evaluateJson(rule, data)
        null
    } catch (exception: JsonLogicException) {
        ErrorOutcome(exception.message ?: "", exception.jsonPath)
    }
}
