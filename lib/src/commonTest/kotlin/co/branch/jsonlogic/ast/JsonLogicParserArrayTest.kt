package co.branch.jsonlogic.ast

import kotlin.test.Test
import kotlin.test.assertEquals

class JsonLogicParserArrayTest {
    @Test
    fun parsesEmptyArray() {
        assertEquals(JsonLogicArray(emptyList()), JsonLogicParser.parse("[]"))
    }

    @Test
    fun parsesFlatArray() {
        assertEquals(
            JsonLogicArray(listOf(JsonLogicNumber(1.0), JsonLogicNumber(2.0), JsonLogicNumber(3.0))),
            JsonLogicParser.parse("[1, 2, 3]")
        )
    }

    @Test
    fun parsesNestedArrays() {
        val expected = JsonLogicArray(
            listOf(
                JsonLogicNumber(1.0),
                JsonLogicArray(listOf(JsonLogicNumber(2.0), JsonLogicArray(listOf(JsonLogicNumber(3.0))))),
                JsonLogicString("x")
            )
        )

        assertEquals(expected, JsonLogicParser.parse("""[1, [2, [3]], "x"]"""))
    }

    @Test
    fun parsesMixedTypeArray() {
        val expected = JsonLogicArray(
            listOf(JsonLogicString("a"), JsonLogicNumber(1.0), JsonLogicBoolean.TRUE, JsonLogicNull)
        )

        assertEquals(expected, JsonLogicParser.parse("""["a", 1, true, null]"""))
    }
}
