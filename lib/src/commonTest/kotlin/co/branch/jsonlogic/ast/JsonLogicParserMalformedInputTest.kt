package co.branch.jsonlogic.ast

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

@OptIn(ExperimentalSerializationApi::class)
class JsonLogicParserMalformedInputTest {
    @Test
    fun unparseableJsonThrowsParseExceptionAtRootPath() {
        val exception = assertFailsWith<JsonLogicParseException> {
            JsonLogicParser.parse("{not valid json")
        }

        assertEquals("$", exception.jsonPath)
        assertNotNull(exception.cause)
    }

    @Test
    fun emptyInputThrowsParseExceptionAtRootPath() {
        val exception = assertFailsWith<JsonLogicParseException> {
            JsonLogicParser.parse("")
        }

        assertEquals("$", exception.jsonPath)
        assertNotNull(exception.cause)
    }

    @Test
    fun truncatedArrayThrowsParseExceptionAtRootPath() {
        val exception = assertFailsWith<JsonLogicParseException> {
            JsonLogicParser.parse("[1, 2")
        }

        assertEquals("$", exception.jsonPath)
        assertNotNull(exception.cause)
    }

    /**
     * An unquoted literal is the one primitive a caller can hand [JsonLogicParser.parse] holding text
     * that is neither a boolean nor a number, since nothing validates a `JsonElement` assembled by
     * hand the way the JSON parser validates text.
     */
    @Test
    fun anUnquotedLiteralThatIsNotANumberThrowsParseException() {
        val exception = assertFailsWith<JsonLogicParseException> {
            JsonLogicParser.parse(JsonUnquotedLiteral("abc"))
        }

        assertEquals("$", exception.jsonPath)
        assertEquals("not a JSON boolean or number: abc", exception.message)
    }

    @Test
    fun anUnquotedLiteralReportsWhereInTheRuleItSits() {
        val rule = JsonObject(mapOf("+" to JsonArray(listOf(JsonUnquotedLiteral("1"), JsonUnquotedLiteral("oops")))))

        val exception = assertFailsWith<JsonLogicParseException> {
            JsonLogicParser.parse(rule)
        }

        assertEquals("$.+[1]", exception.jsonPath)
    }

    /**
     * `Infinity` and `NaN` are the literals a result carries when it has no JSON form, so a result fed
     * back in as a rule reaches the same branch and has to keep parsing.
     */
    @Test
    fun theLiteralsAResultCarriesForItsNonFiniteNumbersStillParse() {
        assertEquals(JsonLogicNumber(Double.POSITIVE_INFINITY), JsonLogicParser.parse(JsonUnquotedLiteral("Infinity")))
        assertEquals(JsonLogicNumber(Double.NEGATIVE_INFINITY), JsonLogicParser.parse(JsonUnquotedLiteral("-Infinity")))

        val nan = JsonLogicParser.parse(JsonUnquotedLiteral("NaN"))
        assertEquals(true, (nan as JsonLogicNumber).value.isNaN())
    }
}
