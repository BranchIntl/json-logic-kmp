package co.branch.jsonlogic.evaluator

import co.branch.jsonlogic.fixtures.jsonSemanticEquals
import co.branch.jsonlogic.internal.jsonElementToValue
import co.branch.jsonlogic.internal.valueToJsonElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Pins the JSON boundary: what the domain receives, and what numeric results look like coming back. */
class JsonInteropTest {

    @Test
    fun jsonScalarsBecomeDomainScalars() {
        assertEquals(1.0, jsonElementToValue(JsonPrimitive(1)))
        assertEquals(1.5, jsonElementToValue(JsonPrimitive(1.5)))
        assertEquals(true, jsonElementToValue(JsonPrimitive(true)))
        assertEquals(false, jsonElementToValue(JsonPrimitive(false)))
        assertEquals("apple", jsonElementToValue(JsonPrimitive("apple")))
        assertNull(jsonElementToValue(JsonNull))
    }

    @Test
    fun quotedScalarsStayStrings() {
        assertEquals("42", jsonElementToValue(JsonPrimitive("42")))
        assertEquals("true", jsonElementToValue(JsonPrimitive("true")))
    }

    @Test
    fun everyJsonNumberBecomesADouble() {
        val values = jsonElementToValue(Json.parseToJsonElement("""[0, -7, 1e3, 9007199254740993]"""))

        assertEquals(listOf(0.0, -7.0, 1000.0, 9007199254740992.0), values)
    }

    @Test
    fun objectsBecomeMapsThatKeepTheirKeyOrder() {
        val value = jsonElementToValue(Json.parseToJsonElement("""{"b": 1, "a": {"c": null}}"""))

        assertEquals(listOf("b", "a"), (value as Map<*, *>).keys.toList())
        assertEquals(mapOf("b" to 1.0, "a" to mapOf("c" to null)), value)
    }

    @Test
    fun numericResultsCarryTheEcmaScriptRenderingAsTheirContent() {
        assertEquals("1", valueToJsonElement(1.0).jsonPrimitive.content)
        assertEquals("1", valueToJsonElement(1).jsonPrimitive.content)
        assertEquals("1e+21", valueToJsonElement(1e21).jsonPrimitive.content)
        assertEquals("1e-7", valueToJsonElement(1e-7).jsonPrimitive.content)
        assertEquals("0", valueToJsonElement(-0.0).jsonPrimitive.content)
        assertEquals("1.5", valueToJsonElement(1.5).jsonPrimitive.content)
        assertFalse(valueToJsonElement(1.0).jsonPrimitive.isString)
    }

    @Test
    fun nonFiniteResultsEscapeAsUnquotedLiterals() {
        for ((value, rendering) in listOf(
            Double.POSITIVE_INFINITY to "Infinity",
            Double.NEGATIVE_INFINITY to "-Infinity",
            Double.NaN to "NaN",
        )) {
            val primitive = valueToJsonElement(value).jsonPrimitive

            assertEquals(rendering, primitive.content)
            assertFalse(primitive.isString, "$rendering must not be quoted")
        }
    }

    @Test
    fun stringResultsStayQuoted() {
        val primitive = valueToJsonElement("1").jsonPrimitive

        assertEquals("1", primitive.content)
        assertTrue(primitive.isString)
    }

    @Test
    fun treesSurviveTheRoundTrip() {
        val tree = Json.parseToJsonElement(
            """{"a": [1, 2.5, "3", true, null, {"b": []}], "c": {}, "d": "", "e": -0.5}""",
        )

        val roundTripped = valueToJsonElement(jsonElementToValue(tree))

        assertTrue(jsonSemanticEquals(roundTripped, tree), "round trip produced $roundTripped")
    }

    @Test
    fun unrepresentableValuesAreRejected() {
        val failure = kotlin.runCatching { valueToJsonElement(Regex("x")) }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException, "expected a rejection, got $failure")
    }
}
