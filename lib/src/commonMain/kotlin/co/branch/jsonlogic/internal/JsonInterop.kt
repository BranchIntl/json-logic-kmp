package co.branch.jsonlogic.internal

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral

/*
 * The single crossing between kotlinx.serialization's JSON tree and the value domain the evaluator
 * and the expressions work in — Double, String, Boolean, null, List and Map. Keeping the tree out of
 * the engine keeps the platform-specific hazards of JsonPrimitive (textual equality, quoting, and
 * platform-dependent number rendering) confined to this file.
 */

/**
 * Converts a JSON tree into the evaluator's value domain: every number becomes a [Double], objects
 * become maps that keep their key order, and JSON null becomes null.
 */
internal fun jsonElementToValue(element: JsonElement): Any? = when (element) {
    is JsonObject -> element.mapValues { (_, value) -> jsonElementToValue(value) }
    is JsonArray -> element.map { jsonElementToValue(it) }
    JsonNull -> null
    is JsonPrimitive -> primitiveToValue(element)
}

/**
 * Converts an evaluation result back into a JSON tree.
 *
 * Numbers are emitted as unquoted literals holding the rendering ECMAScript's `Number::toString`
 * gives them, which is what the JsonLogic reference implementation serializes: a whole number
 * carries no decimal point, so `{"+": [1, 2]}` is the JSON token `3`. Going through a literal rather
 * than `JsonPrimitive(1.0)` keeps a primitive's `content` identical on every target instead of
 * whatever the platform's own formatting produces, and it is also what carries the infinities and
 * NaN, which JSON has no form for but the engine can return.
 */
@OptIn(ExperimentalSerializationApi::class)
internal fun valueToJsonElement(value: Any?): JsonElement = when (value) {
    null -> JsonNull
    is Boolean -> JsonPrimitive(value)
    is String -> JsonPrimitive(value)
    is Number -> JsonUnquotedLiteral(ecmaDoubleToString(value.toDouble()))
    is Map<*, *> -> JsonObject(value.entries.associate { (key, item) -> "$key" to valueToJsonElement(item) })
    is Iterable<*> -> JsonArray(value.map { valueToJsonElement(it) })
    else -> throw IllegalArgumentException("Cannot represent a ${value::class.simpleName} as JSON")
}

private fun primitiveToValue(primitive: JsonPrimitive): Any? {
    // A quoted "42" is a string and a quoted "true" is a string; only unquoted content is coerced.
    if (primitive.isString) {
        return primitive.content
    }

    return when (val content = primitive.content) {
        "true" -> true
        "false" -> false
        else -> parseJavaDouble(content)
            ?: throw IllegalArgumentException("Not a JSON boolean or number: $content")
    }
}
