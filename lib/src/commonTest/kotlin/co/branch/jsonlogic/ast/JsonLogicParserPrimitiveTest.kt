package co.branch.jsonlogic.ast

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonLogicParserPrimitiveTest {
    @Test
    fun parsesString() {
        assertEquals(JsonLogicString("hello"), JsonLogicParser.parse("\"hello\""))
    }

    @Test
    fun parsesEmptyString() {
        assertEquals(JsonLogicString(""), JsonLogicParser.parse("\"\""))
    }

    @Test
    fun parsesIntegerNumberAsDouble() {
        assertEquals(JsonLogicNumber(42.0), JsonLogicParser.parse("42"))
    }

    @Test
    fun parsesNegativeIntegerNumber() {
        assertEquals(JsonLogicNumber(-7.0), JsonLogicParser.parse("-7"))
    }

    @Test
    fun parsesFloatNumber() {
        assertEquals(JsonLogicNumber(3.14), JsonLogicParser.parse("3.14"))
    }

    @Test
    fun parsesExponentNotationNumber() {
        assertEquals(JsonLogicNumber(1.5e3), JsonLogicParser.parse("1.5e3"))
    }

    @Test
    fun parsesTrue() {
        assertEquals(JsonLogicBoolean.TRUE, JsonLogicParser.parse("true"))
    }

    @Test
    fun parsesFalse() {
        assertEquals(JsonLogicBoolean.FALSE, JsonLogicParser.parse("false"))
    }

    @Test
    fun parsesNull() {
        assertEquals(JsonLogicNull, JsonLogicParser.parse("null"))
    }

    @Test
    fun distinguishesNumericStringFromNumber() {
        // "42" (quoted) must parse as a string, not a number: JsonPrimitive.isString is the discriminator.
        assertEquals(JsonLogicString("42"), JsonLogicParser.parse("\"42\""))
        assertEquals(JsonLogicNumber(42.0), JsonLogicParser.parse("42"))
    }

    @Test
    fun parsesFromJsonElementDirectly() {
        val element = Json.parseToJsonElement("""{"cat": ["a", "b"]}""")
        assertEquals(
            JsonLogicOperation("cat", JsonLogicArray(listOf(JsonLogicString("a"), JsonLogicString("b")))),
            JsonLogicParser.parse(element)
        )
    }
}
