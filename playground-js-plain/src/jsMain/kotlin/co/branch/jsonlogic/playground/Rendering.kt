package co.branch.jsonlogic.playground

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@OptIn(ExperimentalSerializationApi::class)
private val PrettyJson = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
}

/**
 * Infinity and NaN arrive as unquoted literals, since JSON has no token for them, and are printed
 * as-is because that is what the engine returned.
 */
fun prettyPrint(value: JsonElement): String =
    try {
        PrettyJson.encodeToString(JsonElement.serializer(), value)
    } catch (e: Throwable) {
        value.toString()
    }

/**
 * The JSON type name for a result. kotlinx models an unquoted primitive by its text alone, so a
 * boolean is recognized by its content rather than by its type.
 */
fun JsonElement.typeName(): String = when (this) {
    JsonNull -> "null"
    is JsonObject -> "object"
    is JsonArray -> "array"
    is JsonPrimitive -> when {
        isString -> "string"
        content == "true" || content == "false" -> "boolean"
        else -> "number"
    }
}
