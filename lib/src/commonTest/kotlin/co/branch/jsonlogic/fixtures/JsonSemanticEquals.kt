package co.branch.jsonlogic.fixtures

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Compares two [JsonElement] values by JSON meaning rather than by kotlinx.serialization's
 * textual [JsonPrimitive] equality, under which `JsonPrimitive(1) != JsonPrimitive(1.0)`. Fixture
 * expectations and engine output can differ only in numeric formatting (`1` vs `1.0`), which this
 * comparator treats as equal; a string is never equal to a non-string primitive.
 */
fun jsonSemanticEquals(a: JsonElement, b: JsonElement): Boolean = when {
    a is JsonNull && b is JsonNull -> true
    a is JsonNull || b is JsonNull -> false
    a is JsonArray && b is JsonArray ->
        a.size == b.size && a.indices.all { jsonSemanticEquals(a[it], b[it]) }
    a is JsonObject && b is JsonObject ->
        a.keys == b.keys && a.keys.all { jsonSemanticEquals(a.getValue(it), b.getValue(it)) }
    a is JsonPrimitive && b is JsonPrimitive -> primitiveEquals(a, b)
    else -> false
}

private fun primitiveEquals(a: JsonPrimitive, b: JsonPrimitive): Boolean {
    if (a.isString || b.isString) {
        return a.isString && b.isString && a.content == b.content
    }
    val aDouble = a.content.toDoubleOrNull()
    val bDouble = b.content.toDoubleOrNull()
    if (aDouble != null && bDouble != null) {
        return aDouble == bDouble
    }
    return a.content == b.content
}
