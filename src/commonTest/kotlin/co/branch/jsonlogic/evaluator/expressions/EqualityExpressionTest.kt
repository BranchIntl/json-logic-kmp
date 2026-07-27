package co.branch.jsonlogic.evaluator.expressions

import co.branch.jsonlogic.evaluator.JsonLogicEvaluationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Pins `==` row by row through the loose equality matrix, and `!=` as its negation. Every row is
 * asserted in both argument orders, since no row of the matrix depends on which side an operand is
 * on.
 */
class EqualityExpressionTest {

    @Test
    fun numberMeetsNumber() {
        assertLooseEquality(true, "1", "1")
        assertLooseEquality(true, "1", "1.0")
        assertLooseEquality(false, "1", "2")
    }

    @Test
    fun numberMeetsString() {
        assertLooseEquality(true, "1", jsonString("1"))
        assertLooseEquality(true, "1", jsonString("1.0"))
        assertLooseEquality(true, "1", jsonString("01"))
        assertLooseEquality(true, "1", jsonString(" 1 "))
        assertLooseEquality(false, "1", jsonString("x"))
        assertLooseEquality(false, "1", jsonString(""))
    }

    @Test
    fun aBlankStringMeetsZero() {
        assertLooseEquality(true, "0", jsonString(""))
        assertLooseEquality(true, "0", jsonString(" "))
        assertLooseEquality(true, "0", jsonString("""\t\n"""))
        // Blank means every character at or below ' ', so a control character counts as blank while
        // a non-breaking space does not, leaving a string no number can be read out of.
        assertEquals(true, evaluateRule("""{"==": [0, {"var": "s"}]}""", mapOf("s" to "\u0001")))
        assertEquals(false, evaluateRule("""{"==": [0, {"var": "s"}]}""", mapOf("s" to "\u00a0")))
    }

    @Test
    fun numberMeetsBoolean() {
        assertLooseEquality(true, "1", "true")
        assertLooseEquality(true, "0", "false")
        assertLooseEquality(false, "2", "true")
        assertLooseEquality(false, "1", "false")
        assertLooseEquality(false, "0", "true")
    }

    @Test
    fun stringMeetsString() {
        assertLooseEquality(true, jsonString("a"), jsonString("a"))
        assertLooseEquality(false, jsonString("a"), jsonString("b"))
        // Two strings are compared as text, so no number is read out of either.
        assertLooseEquality(false, jsonString("1"), jsonString("1.0"))
    }

    @Test
    fun stringMeetsBooleanThroughItsTruthiness() {
        assertLooseEquality(true, jsonString("true"), "true")
        assertLooseEquality(true, jsonString("false"), "true")
        assertLooseEquality(true, jsonString("0"), "true")
        assertLooseEquality(true, jsonString(" "), "true")
        assertLooseEquality(true, jsonString(""), "false")
        assertLooseEquality(false, jsonString("false"), "false")
        assertLooseEquality(false, jsonString("0"), "false")
        assertLooseEquality(false, jsonString(" "), "false")
    }

    @Test
    fun booleanMeetsBoolean() {
        assertLooseEquality(true, "true", "true")
        assertLooseEquality(true, "false", "false")
        assertLooseEquality(false, "true", "false")
    }

    @Test
    fun nullMeetsOnlyNull() {
        assertLooseEquality(true, "null", "null")
        assertLooseEquality(false, "null", "0")
        assertLooseEquality(false, "null", "false")
        assertLooseEquality(false, "null", jsonString(""))
        assertLooseEquality(false, "null", "[]")
    }

    @Test
    fun operandsTheMatrixHasNoRowForAreEqualWhenBothAreFalsy() {
        assertLooseEquality(true, "[]", "[]")
        assertLooseEquality(true, "[]", "false")
        assertLooseEquality(true, "[]", jsonString(""))
        assertLooseEquality(true, "[]", "0")
        assertLooseEquality(false, "[1, 2]", "[1, 2]")
        assertLooseEquality(false, "[0]", "false")
        assertLooseEquality(false, """[""]""", "false")

        // A map is truthy whether or not it holds anything, so no two maps are ever equal.
        val maps = mapOf("a" to emptyMap<String, Any?>(), "b" to emptyMap<String, Any?>())
        assertEquals(false, evaluateRule("""{"==": [{"var": "a"}, {"var": "b"}]}""", maps))
        assertEquals(false, evaluateRule("""{"==": [{"var": ""}, {"var": ""}]}""", maps))
    }

    @Test
    fun twoNumbersAreComparedByBits() {
        assertLooseEquality(true, nanRule, nanRule)
        assertLooseEquality(false, negativeZeroRule, "0")
        assertLooseEquality(true, infinityRule, infinityRule)
    }

    @Test
    fun aNumberAndAStringAreComparedAsPrimitives() {
        assertLooseEquality(false, nanRule, jsonString("NaN"))
        assertLooseEquality(true, negativeZeroRule, jsonString("0"))
        assertLooseEquality(true, negativeZeroRule, jsonString("-0"))
        assertLooseEquality(true, "0", jsonString("-0"))
        assertLooseEquality(true, infinityRule, jsonString("Infinity"))
    }

    @Test
    fun nanIsNeitherTrueNorFalse() {
        assertLooseEquality(false, nanRule, "true")
        assertLooseEquality(false, nanRule, "false")
    }

    @Test
    fun aSoleListArgumentIsSpreadIntoArguments() {
        assertEquals(true, evaluateRule("""{"==": [[1, 1]]}"""))
        assertEquals(false, evaluateRule("""{"==": [[1, 2]]}"""))
    }

    @Test
    fun inequalityNegatesEquality() {
        assertEquals(false, evaluateRule("""{"!=": [1, "1"]}"""))
        assertEquals(true, evaluateRule("""{"!=": [1, 2]}"""))
        assertEquals(false, evaluateRule("""{"!=": [null, null]}"""))
        assertEquals(true, evaluateRule("""{"!=": [[1, 2], [1, 2]]}"""))
        assertEquals(true, evaluateRule("""{"!=": [[1, 2]]}"""))
    }

    @Test
    fun anythingButTwoArgumentsIsAnError() {
        for (key in listOf("==", "!=")) {
            val failure = assertFailsWith<JsonLogicEvaluationException> { evaluateRule("""{"$key": [0]}""") }

            assertEquals("equality expressions expect exactly 2 arguments", failure.message)
            assertEquals("$.$key", failure.jsonPath)
        }

        assertFailsWith<JsonLogicEvaluationException> { evaluateRule("""{"==": []}""") }
        assertFailsWith<JsonLogicEvaluationException> { evaluateRule("""{"==": [1, 2, 3]}""") }
    }

    private fun assertLooseEquality(expected: Boolean, left: String, right: String) {
        assertEquals(expected, evaluateRule("""{"==": [$left, $right]}"""), "$left == $right")
        assertEquals(expected, evaluateRule("""{"==": [$right, $left]}"""), "$right == $left")
    }
}
