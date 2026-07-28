package co.branch.jsonlogic.evaluator

import co.branch.jsonlogic.evaluator.expressions.SubstringExpression
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Pins `substr`'s negative-index arithmetic, its argument-count and type checks, and the places
 * where an out-of-range offset is clamped to `""` versus reaches the underlying `substring` call
 * and throws — upstream only clamps the 2-argument form's start offset, and the 3-argument form's
 * combination of offsets when the start is past the end or past the computed end offset.
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

    @Test
    fun aStartPastTheEndOfTheStringIsClampedToEmptyInTheTwoArgumentForm() {
        assertEquals("", evaluate("""{"substr": ["ab", -10]}""", null, controlStringExpressions))
    }

    @Test
    fun aStartPastTheEndOfTheStringThrowsInTheThreeArgumentForm() {
        // Unlike the 2-argument form, upstream never clamps a start offset that is still negative
        // after the end-relative adjustment in the 3-argument form: it reaches the underlying
        // substring call and throws.
        assertFailsWith<IndexOutOfBoundsException> {
            SubstringExpression.INSTANCE.evaluate(listOf("ab", -10.0, 1.0), null, "$")
        }
    }

    @Test
    fun aPositiveStartPastTheStringLengthThrowsInTheTwoArgumentForm() {
        // The 2-argument form only clamps a start that is negative after adjustment; a positive
        // start past the string's length is never checked and reaches substring, which throws.
        assertFailsWith<IndexOutOfBoundsException> {
            SubstringExpression.INSTANCE.evaluate(listOf("ab", 5.0), null, "$")
        }
    }

    @Test
    fun aStartPastTheComputedEndIsClampedToEmptyInTheThreeArgumentForm() {
        assertEquals("", evaluate("""{"substr": ["ab", 0, 10]}""", null, controlStringExpressions))
    }

    @Test
    fun aNullFirstArgumentThrowsRatherThanRenderingAsTheStringNull() {
        assertFailsWith<NullPointerException> {
            SubstringExpression.INSTANCE.evaluate(listOf(null, 0.0, 1.0), null, "$")
        }
    }
}
