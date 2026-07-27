package co.branch.jsonlogic.ast

import kotlin.test.Test
import kotlin.test.assertEquals

class JsonLogicParserVariableTest {
    @Test
    fun varWithBareStringKeyHasNullDefault() {
        assertEquals(
            JsonLogicVariable(JsonLogicString("a.b"), JsonLogicNull),
            JsonLogicParser.parse("""{"var": "a.b"}""")
        )
    }

    @Test
    fun varWithEmptyStringKey() {
        assertEquals(
            JsonLogicVariable(JsonLogicString(""), JsonLogicNull),
            JsonLogicParser.parse("""{"var": ""}""")
        )
    }

    @Test
    fun varWithExplicitDefault() {
        assertEquals(
            JsonLogicVariable(JsonLogicString("a.b"), JsonLogicString("fallback")),
            JsonLogicParser.parse("""{"var": ["a.b", "fallback"]}""")
        )
    }

    @Test
    fun varWithNumericKey() {
        assertEquals(
            JsonLogicVariable(JsonLogicNumber(0.0), JsonLogicNull),
            JsonLogicParser.parse("""{"var": 0}""")
        )
    }

    @Test
    fun varWithNumericKeyAndDefault() {
        assertEquals(
            JsonLogicVariable(JsonLogicNumber(1.0), JsonLogicString("missing")),
            JsonLogicParser.parse("""{"var": [1, "missing"]}""")
        )
    }

    @Test
    fun varWithNestedRuleAsKey() {
        val expected = JsonLogicVariable(
            key = JsonLogicOperation("cat", JsonLogicArray(listOf(JsonLogicString("a"), JsonLogicString("b")))),
            defaultValue = JsonLogicNull
        )

        assertEquals(expected, JsonLogicParser.parse("""{"var": {"cat": ["a", "b"]}}"""))
    }

    @Test
    fun varWithEmptyArrayArgumentDefaultsKeyAndValueToNull() {
        // {"var": []} has zero arguments: both key and default fall back to JsonLogicNull.
        assertEquals(
            JsonLogicVariable(JsonLogicNull, JsonLogicNull),
            JsonLogicParser.parse("""{"var": []}""")
        )
    }

    @Test
    fun varArgumentsBeyondKeyAndDefaultAreIgnored() {
        assertEquals(
            JsonLogicVariable(JsonLogicString("a"), JsonLogicString("b")),
            JsonLogicParser.parse("""{"var": ["a", "b", "c"]}""")
        )
    }
}
