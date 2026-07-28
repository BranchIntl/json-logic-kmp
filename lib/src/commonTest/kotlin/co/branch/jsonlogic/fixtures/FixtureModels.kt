package co.branch.jsonlogic.fixtures

import kotlinx.serialization.json.JsonElement

/**
 * One `[rule, data, expected]` entry from fixtures.json, tagged with its index in the source
 * array and the most recent section header string that preceded it (empty if none yet).
 */
data class ValueFixtureCase(
    val index: Int,
    val section: String,
    val rule: JsonElement,
    val data: JsonElement,
    val expected: JsonElement,
)

/** One `[rule, data, expectedJsonPath, expectedMessage]` entry from error-fixtures.json. */
data class ErrorFixtureCase(
    val index: Int,
    val rule: JsonElement,
    val data: JsonElement,
    val expectedJsonPath: String,
    val expectedMessage: String,
)
