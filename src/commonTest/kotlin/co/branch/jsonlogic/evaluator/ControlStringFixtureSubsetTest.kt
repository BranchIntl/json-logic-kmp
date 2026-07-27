package co.branch.jsonlogic.evaluator

import co.branch.jsonlogic.JsonLogicException
import co.branch.jsonlogic.fixtures.ErrorOutcome
import co.branch.jsonlogic.fixtures.FixtureLoader
import co.branch.jsonlogic.fixtures.FixtureReplay
import kotlinx.serialization.json.JsonElement
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Replays the fixture cases this workstream's operator table can already run: `if`, `?:`, `and`,
 * `or`, `!`, `!!`, `log`, `cat`, `substr`, `missing`, and `missing_some` — plus, since the filter
 * admits any rule whose operators it covers, including none, every literal and `var` case as well.
 */
class ControlStringFixtureSubsetTest {

    private val supportedOps = setOf(
        "if", "?:", "and", "or", "!", "!!", "log", "cat", "substr", "missing", "missing_some",
    )
    private val replay = FixtureReplay { rule, data -> evaluateJson(rule, data, controlStringExpressions) }

    @Test
    fun valueFixturesPass() {
        val result = replay.runValues(FixtureLoader.loadValueFixtures(), supportedOps)

        assertEquals(150, result.ran, "the control/string/missing/var/literal subset should be 150 cases")
        assertEquals(result.ran, result.passed, result.failures.joinToString("\n"))
    }

    @Test
    fun errorFixturesPass() {
        val result = replay.runErrors(FixtureLoader.loadErrorFixtures(), ::errorOutcome, supportedOps)

        assertEquals(18, result.ran, "the control/string/missing/var/literal subset should be 18 cases")
        assertEquals(result.ran, result.passed, result.failures.joinToString("\n"))
    }

    private fun errorOutcome(rule: JsonElement, data: JsonElement): ErrorOutcome? = try {
        evaluateJson(rule, data, controlStringExpressions)
        null
    } catch (exception: JsonLogicException) {
        ErrorOutcome(exception.message ?: "", exception.jsonPath)
    }
}
