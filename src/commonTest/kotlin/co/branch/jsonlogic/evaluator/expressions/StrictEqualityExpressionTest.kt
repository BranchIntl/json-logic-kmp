package co.branch.jsonlogic.evaluator.expressions

import co.branch.jsonlogic.evaluator.JsonLogicEvaluationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Pins `===` and `!==`, including the two numeric comparisons that read the opposite way from `==`:
 * NaN is not strictly equal to itself, and the two zeros are strictly equal.
 */
class StrictEqualityExpressionTest {

    @Test
    fun operandsOfDifferentKindsAreNeverEqual() {
        assertStrictEquality(false, "1", jsonString("1"))
        assertStrictEquality(false, "0", jsonString("0"))
        assertStrictEquality(false, "1", "true")
        assertStrictEquality(false, "0", "false")
        assertStrictEquality(false, "null", "0")
        assertStrictEquality(false, "null", "false")
        assertStrictEquality(false, "null", jsonString(""))
        assertStrictEquality(false, "[]", "false")
        assertStrictEquality(false, "[]", jsonString(""))
    }

    @Test
    fun operandsOfTheSameKindAreEqualWhenTheyMatch() {
        assertStrictEquality(true, "1", "1")
        assertStrictEquality(true, "1", "1.0")
        assertStrictEquality(false, "1", "2")
        assertStrictEquality(true, jsonString("a"), jsonString("a"))
        assertStrictEquality(false, jsonString("a"), jsonString("b"))
        assertStrictEquality(true, "true", "true")
        assertStrictEquality(false, "true", "false")
        assertStrictEquality(true, "null", "null")
    }

    @Test
    fun twoNumbersAreComparedAsPrimitives() {
        assertStrictEquality(false, nanRule, nanRule)
        assertStrictEquality(true, negativeZeroRule, "0")
        assertStrictEquality(true, infinityRule, infinityRule)
        assertEquals(true, evaluateRule("""{"!==": [$nanRule, $nanRule]}"""))
        assertEquals(false, evaluateRule("""{"!==": [$negativeZeroRule, 0]}"""))
    }

    @Test
    fun twoListsAreEqualWhenTheirElementsAre() {
        assertStrictEquality(true, "[1, 2]", "[1, 2]")
        assertStrictEquality(true, "[]", "[]")
        assertStrictEquality(true, "[[1], 2]", "[[1], 2]")
        assertStrictEquality(false, "[1, 2]", "[3, 4]")
        assertStrictEquality(false, "[1]", "[1, 2]")
        assertStrictEquality(false, "[1, 2]", "[2, 1]")
    }

    @Test
    fun aListsElementsAreComparedByBits() {
        // Inside a list the elements meet as boxed values, the way `==` compares two numbers, so a
        // list holding NaN equals itself and one holding -0.0 does not equal one holding 0.0.
        assertEquals(true, strictlyEqual(listOf(Double.NaN), listOf(Double.NaN)))
        assertEquals(false, strictlyEqual(listOf(-0.0), listOf(0.0)))
        assertEquals(true, strictlyEqual(listOf(null), listOf(null)))
        assertEquals(true, strictlyEqual(listOf(1.0), listOf(1.0)))
    }

    @Test
    fun twoMapsAreEqualWhenTheirEntriesAre() {
        assertEquals(true, strictlyEqual(emptyMap<String, Any?>(), emptyMap<String, Any?>()))
        assertEquals(true, strictlyEqual(mapOf("x" to 1.0), mapOf("x" to 1.0)))
        assertEquals(true, strictlyEqual(mapOf("x" to listOf(1.0)), mapOf("x" to listOf(1.0))))
        assertEquals(false, strictlyEqual(mapOf("x" to 1.0), mapOf("x" to 2.0)))
        assertEquals(false, strictlyEqual(mapOf("x" to 1.0), mapOf("y" to 1.0)))
    }

    @Test
    fun anOperandIsEqualToItself() {
        val data = mapOf("a" to listOf(1.0, 2.0))

        assertEquals(true, evaluateRule("""{"===": [{"var": "a"}, {"var": "a"}]}""", data))
        assertEquals(true, evaluateRule("""{"===": [{"var": ""}, {"var": ""}]}""", data))
    }

    @Test
    fun aSoleListArgumentIsSpreadIntoArguments() {
        assertEquals(true, evaluateRule("""{"===": [[1, 1]]}"""))
        assertEquals(false, evaluateRule("""{"===": [[1, 2]]}"""))
        assertEquals(false, evaluateRule("""{"!==": [[1, 1]]}"""))
    }

    @Test
    fun anythingButTwoArgumentsIsAnError() {
        for (key in listOf("===", "!==")) {
            val failure = assertFailsWith<JsonLogicEvaluationException> { evaluateRule("""{"$key": [1]}""") }

            assertEquals("equality expressions expect exactly 2 arguments", failure.message)
            assertEquals("$.$key", failure.jsonPath)
        }

        assertFailsWith<JsonLogicEvaluationException> { evaluateRule("""{"===": []}""") }
        assertFailsWith<JsonLogicEvaluationException> { evaluateRule("""{"===": [1, 2, 3]}""") }
    }

    private fun assertStrictEquality(expected: Boolean, left: String, right: String) {
        assertEquals(expected, evaluateRule("""{"===": [$left, $right]}"""), "$left === $right")
        assertEquals(expected, evaluateRule("""{"===": [$right, $left]}"""), "$right === $left")
        assertEquals(!expected, evaluateRule("""{"!==": [$left, $right]}"""), "$left !== $right")
    }

    /** Compares two values the rule cannot spell out, handing them over as data. */
    private fun strictlyEqual(left: Any?, right: Any?): Any? =
        evaluateRule("""{"===": [{"var": "left"}, {"var": "right"}]}""", mapOf("left" to left, "right" to right))
}
