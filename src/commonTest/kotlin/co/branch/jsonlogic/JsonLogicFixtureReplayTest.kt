package co.branch.jsonlogic

import co.branch.jsonlogic.fixtures.ErrorOutcome
import co.branch.jsonlogic.fixtures.FixtureLoader
import co.branch.jsonlogic.fixtures.FixtureReplay
import kotlinx.serialization.json.JsonElement
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Replays the entire fixture corpus through a default-configuration [JsonLogic] instance — the
 * acceptance test for the whole migration to date. Every value and error fixture is expected to
 * run (no operator filtering) and to pass, since the default table registers all 34 operations the
 * corpus exercises.
 */
class JsonLogicFixtureReplayTest {

    private val jsonLogic = JsonLogic()
    private val replay = FixtureReplay { rule, data -> jsonLogic.apply(rule, data) }

    @Test
    fun allValueFixturesPass() {
        val result = replay.runValues(FixtureLoader.loadValueFixtures())

        assertEquals(289, result.ran, "expected all 289 value fixtures to run against the default operator table")
        assertEquals(result.ran, result.passed, result.failures.joinToString("\n"))
    }

    @Test
    fun allErrorFixturesPass() {
        val result = replay.runErrors(FixtureLoader.loadErrorFixtures(), ::errorOutcome)

        assertEquals(46, result.ran, "expected all 46 error fixtures to run against the default operator table")
        assertEquals(result.ran, result.passed, result.failures.joinToString("\n"))
    }

    private fun errorOutcome(rule: JsonElement, data: JsonElement): ErrorOutcome? = try {
        jsonLogic.apply(rule, data)
        null
    } catch (exception: JsonLogicException) {
        ErrorOutcome(exception.message ?: "", exception.jsonPath)
    }
}
