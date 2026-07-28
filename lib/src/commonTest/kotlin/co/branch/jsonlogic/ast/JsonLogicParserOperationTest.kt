package co.branch.jsonlogic.ast

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JsonLogicParserOperationTest {
    @Test
    fun singleKeyOperationWithArrayArguments() {
        val expected = JsonLogicOperation(
            "+",
            JsonLogicArray(listOf(JsonLogicNumber(1.0), JsonLogicNumber(2.0), JsonLogicNumber(3.0)))
        )

        assertEquals(expected, JsonLogicParser.parse("""{"+": [1, 2, 3]}"""))
    }

    @Test
    fun singleNonArrayArgumentIsNormalizedToASingletonArray() {
        // A non-array argument value (here a bare boolean) is coerced into a 1-element JsonLogicArray.
        assertEquals(
            JsonLogicOperation("!", JsonLogicArray(listOf(JsonLogicBoolean.TRUE))),
            JsonLogicParser.parse("""{"!": true}""")
        )
    }

    @Test
    fun singleObjectArgumentIsNormalizedToASingletonArray() {
        // The argument value is itself a single-key operation object, not an array, so it is still
        // wrapped in a singleton JsonLogicArray rather than being spread into arguments.
        val expected = JsonLogicOperation(
            "+",
            JsonLogicArray(
                listOf(
                    JsonLogicOperation(
                        "merge",
                        JsonLogicArray(listOf(JsonLogicNumber(1.0), JsonLogicArray(listOf(JsonLogicNumber(2.0)))))
                    )
                )
            )
        )

        assertEquals(expected, JsonLogicParser.parse("""{"+": {"merge": [1, [2]]}}"""))
    }

    @Test
    fun zeroKeyObjectThrowsWithRootJsonPath() {
        val exception = assertFailsWith<JsonLogicParseException> {
            JsonLogicParser.parse("{}")
        }

        assertEquals("objects must have exactly 1 key defined, found 0", exception.message)
        assertEquals("$", exception.jsonPath)
    }

    @Test
    fun multiKeyObjectThrowsWithRootJsonPath() {
        val exception = assertFailsWith<JsonLogicParseException> {
            JsonLogicParser.parse("""{"a": 1, "b": 2}""")
        }

        assertEquals("objects must have exactly 1 key defined, found 2", exception.message)
        assertEquals("$", exception.jsonPath)
    }

    @Test
    fun zeroKeyObjectNestedInOperationArgumentsThrowsWithPreciseJsonPath() {
        // Ported from the upstream error fixture: {"cat": ["foo", {}]} -> "$.cat[1]".
        val exception = assertFailsWith<JsonLogicParseException> {
            JsonLogicParser.parse("""{"cat": ["foo", {}]}""")
        }

        assertEquals("objects must have exactly 1 key defined, found 0", exception.message)
        assertEquals("$.cat[1]", exception.jsonPath)
    }

    @Test
    fun deeplyNestedRealisticRule() {
        // Ported from the upstream fixture: {"missing": {"merge": ["vin", {"if": [{"var": "financing"}, ["apr"], []]}]}}
        val expected = JsonLogicOperation(
            "missing",
            JsonLogicArray(
                listOf(
                    JsonLogicOperation(
                        "merge",
                        JsonLogicArray(
                            listOf(
                                JsonLogicString("vin"),
                                JsonLogicOperation(
                                    "if",
                                    JsonLogicArray(
                                        listOf(
                                            JsonLogicVariable(JsonLogicString("financing"), JsonLogicNull),
                                            JsonLogicArray(listOf(JsonLogicString("apr"))),
                                            JsonLogicArray(emptyList())
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        val json = """{"missing": {"merge": ["vin", {"if": [{"var": "financing"}, ["apr"], []]}]}}"""
        assertEquals(expected, JsonLogicParser.parse(json))
    }
}
