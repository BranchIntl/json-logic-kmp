package co.branch.jsonlogic.ast

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Pins the nesting bound: what it counts, that it holds on both the string and the [JsonElement]
 * entry point, and that it holds for a rule far deeper than the stack could survive.
 */
class JsonLogicParserDepthTest {

    /** A rule nested [containers] containers deep: each `{"+": [ … ]}` wrapper is one object and one array. */
    private fun nested(containers: Int): String = buildString {
        repeat(containers / 2) { append("""{"+":[1,""") }
        append("1")
        repeat(containers / 2) { append("]}") }
    }

    @Test
    fun aRuleAtTheBoundParses() {
        val rule = nested(JsonLogicParser.DEFAULT_MAX_DEPTH)

        assertTrue(JsonLogicParser.parse(rule) is JsonLogicOperation)
        assertTrue(JsonLogicParser.parse(Json.parseToJsonElement(rule)) is JsonLogicOperation)
    }

    @Test
    fun aRulePastTheBoundIsRejected() {
        val rule = nested(JsonLogicParser.DEFAULT_MAX_DEPTH + 2)

        for (parse in listOf({ JsonLogicParser.parse(rule) }, { JsonLogicParser.parse(Json.parseToJsonElement(rule)) })) {
            val exception = assertFailsWith<JsonLogicParseException> { parse() }

            assertEquals("rule nests deeper than ${JsonLogicParser.DEFAULT_MAX_DEPTH} levels", exception.message)
        }
    }

    @Test
    fun anExplicitBoundReplacesTheDefault() {
        assertTrue(JsonLogicParser.parse(nested(4), maxDepth = 4) is JsonLogicOperation)
        assertFailsWith<JsonLogicParseException> { JsonLogicParser.parse(nested(6), maxDepth = 4) }
        assertFailsWith<JsonLogicParseException> {
            JsonLogicParser.parse(Json.parseToJsonElement(nested(6)), maxDepth = 4)
        }
    }

    /** The bound counts containers, so the shallowest rules are nowhere near it. */
    @Test
    fun ordinaryRulesAreUnaffected() {
        assertTrue(JsonLogicParser.parse("1", maxDepth = 1) is JsonLogicNumber)
        assertTrue(JsonLogicParser.parse("""{"var": "a"}""", maxDepth = 1) is JsonLogicVariable)
        assertTrue(JsonLogicParser.parse("""{"+": [1, 2]}""", maxDepth = 2) is JsonLogicOperation)
    }

    @Test
    fun theReportedPathNamesWhereTheRuleGotTooDeep() {
        val exception = assertFailsWith<JsonLogicParseException> {
            JsonLogicParser.parse(Json.parseToJsonElement("""{"+":[1,{"+":[1,1]}]}"""), maxDepth = 2)
        }

        assertEquals("$.+[1]", exception.jsonPath)
    }

    /** Brackets inside a string are text, not nesting. */
    @Test
    fun bracketsInsideStringLiteralsDoNotCount() {
        val rule = """{"cat": ["[[[[[[", "]]]]]]", "\"[[[", "{{{"]}"""

        assertTrue(JsonLogicParser.parse(rule, maxDepth = 2) is JsonLogicOperation)
    }

    /**
     * Far past what building the tree could survive: kotlinx's own object/array recursion overflows the
     * stack somewhere between 2,000 and 5,000 levels, and an overflow is not catchable on every target,
     * so the bound has to be applied to the text before the JSON parser sees it.
     */
    @Test
    fun aRuleTooDeepToParseAtAllIsRejectedCleanly() {
        val rule = nested(50_000)

        val exception = assertFailsWith<JsonLogicParseException> { JsonLogicParser.parse(rule) }

        assertEquals("rule nests deeper than ${JsonLogicParser.DEFAULT_MAX_DEPTH} levels", exception.message)
    }
}
