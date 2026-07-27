package co.branch.jsonlogic.ast

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Parses JSON into a [JsonLogicNode] tree, applying the `var` and single-key-operation sugar. */
object JsonLogicParser {
    fun parse(json: String): JsonLogicNode {
        val element = try {
            Json.parseToJsonElement(json)
        } catch (e: SerializationException) {
            throw JsonLogicParseException(e, "$")
        }

        return parse(element)
    }

    fun parse(element: JsonElement): JsonLogicNode = parse(element, "$")

    private fun parse(element: JsonElement, jsonPath: String): JsonLogicNode {
        // JsonNull is itself a JsonPrimitive, so it must be checked before the primitive branch below.
        if (element is JsonNull) {
            return JsonLogicNull
        }

        if (element is JsonPrimitive) {
            if (element.isString) {
                return JsonLogicString(element.content)
            }

            return when (element.content) {
                "true" -> JsonLogicBoolean.TRUE
                "false" -> JsonLogicBoolean.FALSE
                else -> JsonLogicNumber(element.content.toDouble())
            }
        }

        if (element is JsonArray) {
            val elements = element.mapIndexed { index, item -> parse(item, "$jsonPath[$index]") }
            return JsonLogicArray(elements)
        }

        val obj = element as JsonObject

        if (obj.size != 1) {
            throw JsonLogicParseException("objects must have exactly 1 key defined, found ${obj.size}", jsonPath)
        }

        val (key, value) = obj.entries.first()
        val argumentNode = parse(value, "$jsonPath.$key")

        // Always coerce single-argument operations into a JsonLogicArray with a single element.
        val arguments = if (argumentNode is JsonLogicArray) argumentNode else JsonLogicArray(listOf(argumentNode))

        if (key == "var") {
            val keyNode = if (arguments.elements.isEmpty()) JsonLogicNull else arguments.elements[0]
            val defaultValue = if (arguments.elements.size > 1) arguments.elements[1] else JsonLogicNull
            return JsonLogicVariable(keyNode, defaultValue)
        }

        return JsonLogicOperation(key, arguments)
    }
}
