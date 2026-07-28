package co.branch.jsonlogic.evaluator

import co.branch.jsonlogic.evaluator.expressions.SubstringExpression
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Pins `substr`'s negative-index arithmetic, its argument-count and type checks, and the clamping
 * that keeps every offset combination in range. Each expectation below is what the reference
 * implementation returns for the same rule.
 */
class SubstringExpressionTest {

    @Test
    fun positiveStartWithNoLengthTakesTheRest() {
        assertEquals("logic", evaluate("""{"substr": ["jsonlogic", 4]}""", null, controlStringExpressions))
    }

    @Test
    fun negativeStartCountsBackFromTheEnd() {
        assertEquals("logic", evaluate("""{"substr": ["jsonlogic", -5]}""", null, controlStringExpressions))
    }

    @Test
    fun positiveStartAndLength() {
        assertEquals("j", evaluate("""{"substr": ["jsonlogic", 0, 1]}""", null, controlStringExpressions))
        assertEquals("logic", evaluate("""{"substr": ["jsonlogic", 4, 5]}""", null, controlStringExpressions))
    }

    @Test
    fun negativeStartAndPositiveLength() {
        assertEquals("c", evaluate("""{"substr": ["jsonlogic", -1, 1]}""", null, controlStringExpressions))
        assertEquals("logic", evaluate("""{"substr": ["jsonlogic", -5, 5]}""", null, controlStringExpressions))
    }

    @Test
    fun negativeStartAndNegativeLength() {
        assertEquals("log", evaluate("""{"substr": ["jsonlogic", -5, -2]}""", null, controlStringExpressions))
    }

    @Test
    fun positiveStartAndNegativeLength() {
        assertEquals("son", evaluate("""{"substr": ["jsonlogic", 1, -5]}""", null, controlStringExpressions))
    }

    @Test
    fun tooFewOrTooManyArgumentsFails() {
        for (rule in listOf("""{"substr": ["jsonlogic"]}""", """{"substr": ["jsonlogic", 1, 2, 3]}""")) {
            val exception = assertFailsWith<JsonLogicEvaluationException>(rule) {
                evaluate(rule, null, controlStringExpressions)
            }

            assertEquals("substr expects 2 or 3 arguments", exception.message)
            assertEquals("$.substr", exception.jsonPath)
        }
    }

    @Test
    fun secondArgumentMustBeANumber() {
        val exception = assertFailsWith<JsonLogicEvaluationException> {
            evaluate("""{"substr": ["jsonlogic", "one"]}""", null, controlStringExpressions)
        }

        assertEquals("second argument to substr must be a number", exception.message)
        assertEquals("$.substr[1]", exception.jsonPath)
    }

    @Test
    fun thirdArgumentMustBeANumber() {
        val exception = assertFailsWith<JsonLogicEvaluationException> {
            evaluate("""{"substr": ["jsonlogic", 1, "two"]}""", null, controlStringExpressions)
        }

        assertEquals("third argument to substr must be an integer", exception.message)
        assertEquals("$.substr[2]", exception.jsonPath)
    }

    @Test
    fun secondArgumentMustBeADoubleSpecifically() {
        // Every number this engine produces is already a Double, so only a hand-built expression
        // returning a non-Double Number can reach this branch at all.
        val expressions = controlStringExpressions + ConstantExpression("intFour", 4)

        val exception = assertFailsWith<JsonLogicEvaluationException> {
            evaluate("""{"substr": ["jsonlogic", {"intFour": []}]}""", null, expressions)
        }

        assertEquals("second argument to substr must be a number", exception.message)
    }

    /** A start further back than the string is long stops at its beginning, in either form. */
    @Test
    fun aStartBeforeTheBeginningIsClampedToIt() {
        assertEquals("ab", evaluate("""{"substr": ["ab", -10]}""", null, controlStringExpressions))
        assertEquals("jsonlogic", evaluate("""{"substr": ["jsonlogic", -40]}""", null, controlStringExpressions))
        assertEquals("a", SubstringExpression.INSTANCE.evaluate(listOf("ab", -10.0, 1.0), null, "$"))
    }

    @Test
    fun aStartPastTheEndYieldsTheEmptyString() {
        assertEquals("", SubstringExpression.INSTANCE.evaluate(listOf("ab", 5.0), null, "$"))
        assertEquals("", evaluate("""{"substr": ["ab", 2]}""", null, controlStringExpressions))
        assertEquals("", evaluate("""{"substr": ["jsonlogic", 20, -40]}""", null, controlStringExpressions))
    }

    @Test
    fun aLengthPastTheEndIsClampedToWhatIsLeft() {
        assertEquals("ab", evaluate("""{"substr": ["ab", 0, 10]}""", null, controlStringExpressions))
        assertEquals("b", evaluate("""{"substr": ["ab", 1, 10]}""", null, controlStringExpressions))
    }

    @Test
    fun aNegativeLengthLongerThanWhatIsLeftYieldsTheEmptyString() {
        assertEquals("", evaluate("""{"substr": ["ab", 1, -5]}""", null, controlStringExpressions))
        assertEquals("", evaluate("""{"substr": ["ab", 0, -2]}""", null, controlStringExpressions))
    }

    /**
     * The source is stringified, so a null one is the four characters of `"null"` — the reference
     * renders `substr`'s source through `String()`, unlike `cat`, which drops a null argument.
     */
    @Test
    fun aNullSourceRendersAsTheWordNull() {
        assertEquals("ull", SubstringExpression.INSTANCE.evaluate(listOf(null, 1.0), null, "$"))
        assertEquals("nu", SubstringExpression.INSTANCE.evaluate(listOf(null, 0.0, 2.0), null, "$"))
        assertEquals("ull", evaluate("""{"substr": [{"var": "x"}, 1]}""", mapOf("x" to null), controlStringExpressions))
    }

    @Test
    fun nonStringSourcesRenderBeforeSlicing() {
        assertEquals("4", evaluate("""{"substr": [42, 0, 1]}""", null, controlStringExpressions))
        assertEquals("1.5", evaluate("""{"substr": [1.5, 0, 3]}""", null, controlStringExpressions))
        assertEquals("1", evaluate("""{"substr": [1, 0, 1]}""", null, controlStringExpressions))
        assertEquals("tr", SubstringExpression.INSTANCE.evaluate(listOf(true, 0.0, 2.0), null, "$"))
        assertEquals("100", SubstringExpression.INSTANCE.evaluate(listOf(1e7, 0.0, 3.0), null, "$"))
    }
}
