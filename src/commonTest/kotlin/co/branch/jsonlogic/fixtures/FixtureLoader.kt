package co.branch.jsonlogic.fixtures

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * Parses the fixture JSON embedded by the `generateFixtures` Gradle task ([EmbeddedFixtures])
 * into the case types the replay harness runs against.
 */
object FixtureLoader {

    fun loadValueFixtures(): List<ValueFixtureCase> {
        var section = ""
        val cases = mutableListOf<ValueFixtureCase>()
        fixturesRoot().forEachIndexed { index, element ->
            when {
                element is JsonPrimitive && element.isString -> section = element.content
                element is JsonArray -> {
                    check(element.size == 3) {
                        "fixtures.json[$index]: expected exactly 3 elements (rule, data, expected), found ${element.size}: $element"
                    }
                    cases += ValueFixtureCase(index, section, element[0], element[1], element[2])
                }
                else -> error("fixtures.json[$index]: expected a string section header or a 3-element array, found $element")
            }
        }
        return cases
    }

    fun countSectionHeaders(): Int =
        fixturesRoot().count { it is JsonPrimitive && it.isString }

    fun loadErrorFixtures(): List<ErrorFixtureCase> =
        errorFixturesRoot().mapIndexed { index, element ->
            val array = element as? JsonArray
                ?: error("error-fixtures.json[$index]: expected a 4-element array, found $element")
            check(array.size == 4) {
                "error-fixtures.json[$index]: expected exactly 4 elements (rule, data, jsonPath, message), found ${array.size}: $array"
            }
            ErrorFixtureCase(
                index = index,
                rule = array[0],
                data = array[1],
                expectedJsonPath = array[2].jsonPrimitive.content,
                expectedMessage = array[3].jsonPrimitive.content,
            )
        }

    private fun fixturesRoot(): JsonArray = parseEmbedded("fixtures.json")

    private fun errorFixturesRoot(): JsonArray = parseEmbedded("error-fixtures.json")

    private fun parseEmbedded(fileName: String): JsonArray {
        val text = EmbeddedFixtures.files[fileName]
            ?: error("Fixture file '$fileName' was not embedded by the generateFixtures Gradle task.")
        return Json.parseToJsonElement(text).jsonArray
    }
}
