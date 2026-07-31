package co.branch.jsonlogic.ast

import co.branch.jsonlogic.internal.parseJavaDouble
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Parses JSON into a [JsonLogicNode] tree, applying the `var` and single-key-operation sugar.
 *
 * Every overload rejects a rule nested deeper than [DEFAULT_MAX_DEPTH] containers, since parsing and
 * evaluating a rule both recurse and a rule can arrive from anywhere. Pass an explicit `maxDepth` to
 * raise or lower that.
 */
object JsonLogicParser {

    /**
     * The nesting a rule may reach before [parse] rejects it, counted in objects and arrays — so
     * `{"+": [1, 2]}` is 2 levels deep, and every operator around it adds another 2.
     *
     * Generous enough that no rule written by hand comes close, and small enough that neither parsing
     * nor evaluating what it admits can exhaust the stack on any supported target.
     */
    const val DEFAULT_MAX_DEPTH: Int = 128

    fun parse(json: String): JsonLogicNode = parse(json, DEFAULT_MAX_DEPTH)

    fun parse(json: String, maxDepth: Int): JsonLogicNode {
        // Ahead of the JSON parser, not after it: kotlinx switches to a heap-allocated stack for deeply
        // nested objects, but not for the object-inside-array alternation a JsonLogic rule is built
        // from, so building the tree at all is what overflows first — and a stack overflow is not
        // something a caller can catch on every target.
        requireNestingWithin(json, maxDepth)

        val element = try {
            Json.parseToJsonElement(json)
        } catch (e: SerializationException) {
            throw JsonLogicParseException(e, "$")
        }

        return parse(element, maxDepth)
    }

    fun parse(element: JsonElement): JsonLogicNode = parse(element, DEFAULT_MAX_DEPTH)

    fun parse(element: JsonElement, maxDepth: Int): JsonLogicNode = parse(element, "$", 0, maxDepth)

    private fun parse(element: JsonElement, jsonPath: String, depth: Int, maxDepth: Int): JsonLogicNode {
        // JsonNull is itself a JsonPrimitive, so it must be checked before the primitive branch below.
        if (element is JsonNull) {
            return JsonLogicNull
        }

        if (element is JsonPrimitive) {
            if (element.isString) {
                return JsonLogicString(element.content)
            }

            return when (val content = element.content) {
                "true" -> JsonLogicBoolean.TRUE
                "false" -> JsonLogicBoolean.FALSE
                // Through the same hand-written parser the data side uses rather than `toDouble`, whose
                // result the stdlib documents as platform-dependent: a rule literal is the one input a
                // caller can count on being read identically on every target. It returns null instead
                // of throwing, which is what keeps a malformed literal inside this parser's contract.
                else -> JsonLogicNumber(
                    parseJavaDouble(content) ?: throw JsonLogicParseException(
                        "not a JSON boolean or number: $content",
                        jsonPath,
                    ),
                )
            }
        }

        if (depth >= maxDepth) {
            throw JsonLogicParseException("rule nests deeper than $maxDepth levels", jsonPath)
        }

        if (element is JsonArray) {
            val elements = element.mapIndexed { index, item ->
                parse(item, "$jsonPath[$index]", depth + 1, maxDepth)
            }
            return JsonLogicArray(elements)
        }

        val obj = element as JsonObject

        if (obj.size != 1) {
            throw JsonLogicParseException("objects must have exactly 1 key defined, found ${obj.size}", jsonPath)
        }

        val (key, value) = obj.entries.first()
        val argumentNode = parse(value, "$jsonPath.$key", depth + 1, maxDepth)

        // Always coerce single-argument operations into a JsonLogicArray with a single element.
        val arguments = if (argumentNode is JsonLogicArray) argumentNode else JsonLogicArray(listOf(argumentNode))

        if (key == "var") {
            val keyNode = if (arguments.elements.isEmpty()) JsonLogicNull else arguments.elements[0]
            val defaultValue = if (arguments.elements.size > 1) arguments.elements[1] else JsonLogicNull
            return JsonLogicVariable(keyNode, defaultValue)
        }

        return JsonLogicOperation(key, arguments)
    }

    /**
     * Counts the brackets in [json] without recursing, skipping those inside string literals. Only the
     * nesting is checked; anything else wrong with the text is left for the JSON parser to report,
     * except that text too malformed to balance its brackets can be rejected here for being too deep
     * rather than there for being unparseable.
     */
    private fun requireNestingWithin(json: String, maxDepth: Int) {
        var depth = 0
        var inString = false
        var index = 0

        while (index < json.length) {
            val character = json[index]

            if (inString) {
                when (character) {
                    '\\' -> index++
                    '"' -> inString = false
                }
            } else {
                when (character) {
                    '"' -> inString = true
                    '{', '[' -> {
                        depth++
                        if (depth > maxDepth) {
                            throw JsonLogicParseException("rule nests deeper than $maxDepth levels", "$")
                        }
                    }
                    '}', ']' -> depth--
                }
            }

            index++
        }
    }
}
