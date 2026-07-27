package co.branch.jsonlogic.parity

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Decides whether a result from the Java engine's value domain (`Object`: null, Boolean, Double,
 * String, List, Map) means the same thing as the corresponding [JsonElement] from this library.
 *
 * The two domains are compared by JSON meaning, not by rendering:
 * - Java `null` matches [JsonNull], and nothing else does.
 * - A number matches an unquoted primitive whose text reads back as the identical double. The
 *   comparison is on `Double.toBits`, so NaN matches NaN (any payload) while `-0.0` and `0.0` are
 *   held apart — two engines computing the same double agree on the sign of zero, and the port
 *   renders the two differently, so treating them as equal would hide a divergence.
 *   `NaN`, `Infinity` and `-Infinity` reach here as the bare literals this library emits for them,
 *   which is what `Double.parseDouble` accepts, so they need no special case.
 * - A string matches only a quoted primitive with identical content: a number never equals a string.
 * - Lists match arrays element-wise, maps match objects by key set and by value per key, so object
 *   key order is irrelevant.
 * - Anything else the Java engine could hand back (an array, an arbitrary object) matches nothing;
 *   the diagnostic names its runtime type.
 */
internal object ParityComparator {

    fun matches(javaValue: Any?, kotlinValue: JsonElement): Boolean = when (javaValue) {
        null -> kotlinValue is JsonNull
        is Boolean -> booleanOf(kotlinValue) == javaValue
        is String -> stringOf(kotlinValue) == javaValue
        is Number -> matchesNumber(javaValue.toDouble(), kotlinValue)
        is Map<*, *> -> matchesMap(javaValue, kotlinValue)
        is Iterable<*> -> matchesList(javaValue.toList(), kotlinValue)
        else -> false
    }

    private fun matchesNumber(javaValue: Double, kotlinValue: JsonElement): Boolean {
        val primitive = kotlinValue as? JsonPrimitive ?: return false
        if (primitive.isString) return false
        val ported = primitive.content.toDoubleOrNull() ?: return false

        return javaValue.toBits() == ported.toBits()
    }

    private fun matchesMap(javaValue: Map<*, *>, kotlinValue: JsonElement): Boolean {
        val obj = kotlinValue as? JsonObject ?: return false
        val keys = javaValue.keys.map { "$it" }.toSet()
        if (keys != obj.keys) return false

        return javaValue.entries.all { (key, value) -> matches(value, obj.getValue("$key")) }
    }

    private fun matchesList(javaValue: List<*>, kotlinValue: JsonElement): Boolean {
        val array = kotlinValue as? JsonArray ?: return false
        if (javaValue.size != array.size) return false

        return javaValue.indices.all { matches(javaValue[it], array[it]) }
    }

    private fun booleanOf(element: JsonElement): Boolean? {
        val primitive = element as? JsonPrimitive ?: return null
        if (primitive.isString) return null

        return when (primitive.content) {
            "true" -> true
            "false" -> false
            else -> null
        }
    }

    private fun stringOf(element: JsonElement): String? {
        val primitive = element as? JsonPrimitive ?: return null

        return if (primitive.isString) primitive.content else null
    }
}

/** Renders a Java-side value as JSON-ish text, tagged with the runtime type of its root. */
internal fun describeJavaValue(value: Any?): String {
    val rendered = renderJavaValue(value)

    return if (value == null) rendered else "$rendered (${value.javaClass.simpleName})"
}

private fun renderJavaValue(value: Any?): String = when (value) {
    null -> "null"
    is String -> "\"$value\""
    is Boolean, is Number -> "$value"
    is Map<*, *> -> value.entries.joinToString(", ", "{", "}") { "\"${it.key}\": ${renderJavaValue(it.value)}" }
    is Iterable<*> -> value.joinToString(", ", "[", "]") { renderJavaValue(it) }
    else -> "$value"
}
