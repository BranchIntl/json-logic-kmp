package co.branch.jsonlogic.evaluator

import co.branch.jsonlogic.evaluator.expressions.ConcatenateExpression
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Pins `cat`'s number rendering (the whole-number-Double-to-integer quirk and its `1e7` boundary),
 * and its rendering of every other domain shape. Edge doubles (NaN, Infinity, `1e7`) and nested
 * lists/maps are exercised through [ConcatenateExpression.INSTANCE] directly — JSON has no literal
 * for the former, and a sole list/map argument would otherwise be unwrapped into `cat`'s own argument
 * list by [co.branch.jsonlogic.evaluator.expressions.PreEvaluatedArgumentsExpression].
 */
class ConcatenateExpressionTest {

    @Test
    fun joinsStringsWithNoSeparator() {
        assertEquals("icecream", evaluate("""{"cat": ["ice", "cream"]}""", null, controlStringExpressions))
        assertEquals(
            "we all scream for icecream",
            evaluate("""{"cat": ["we all scream for ", "ice", "cream"]}""", null, controlStringExpressions),
        )
    }

    @Test
    fun aSoleScalarArgumentIsReturnedRendered() {
        assertEquals("ice", evaluate("""{"cat": "ice"}""", null, controlStringExpressions))
    }

    @Test
    fun wholeNumberDoublesRenderAsTheirIntegerPart() {
        assertEquals("12", evaluate("""{"cat": [1, 2]}""", null, controlStringExpressions))
        assertEquals("Robocop2", evaluate("""{"cat": ["Robocop", 2]}""", null, controlStringExpressions))
        assertEquals("1", ConcatenateExpression.INSTANCE.evaluate(listOf(1.0), null, "$"))
        assertEquals("-2", ConcatenateExpression.INSTANCE.evaluate(listOf(-2.0), null, "$"))
        assertEquals("0", ConcatenateExpression.INSTANCE.evaluate(listOf(0.0), null, "$"))
        assertEquals("0", ConcatenateExpression.INSTANCE.evaluate(listOf(-0.0), null, "$"))
        assertEquals("9999999", ConcatenateExpression.INSTANCE.evaluate(listOf(9999999.0), null, "$"))
    }

    @Test
    fun mixedArgumentsStripOnlyTheIntegralDoublesAmongThem() {
        assertEquals("-2.1.5", ConcatenateExpression.INSTANCE.evaluate(listOf(-2.0, ".", 1.5), null, "$"))
        assertEquals("00", ConcatenateExpression.INSTANCE.evaluate(listOf(0.0, -0.0), null, "$"))
    }

    @Test
    fun nonIntegralDoublesRenderInFull() {
        assertEquals("1.5", ConcatenateExpression.INSTANCE.evaluate(listOf(1.5), null, "$"))
        assertEquals("0.1", ConcatenateExpression.INSTANCE.evaluate(listOf(0.1), null, "$"))
        assertEquals("-2.5", ConcatenateExpression.INSTANCE.evaluate(listOf(-2.5), null, "$"))
    }

    @Test
    fun oneETo7StaysInScientificNotationRatherThanBeingStripped() {
        // 1e7 is a whole number, but canonicalDoubleToString renders magnitudes at or above 1e7 in
        // scientific notation ("1.0E7"), which does not end in ".0" — so the integer-stripping quirk
        // does not fire here, unlike every whole-number Double below that magnitude.
        assertEquals("1.0E7", ConcatenateExpression.INSTANCE.evaluate(listOf(1e7), null, "$"))
    }

    @Test
    fun nanAndInfinityRenderAsTheirNames() {
        assertEquals("NaN", ConcatenateExpression.INSTANCE.evaluate(listOf(Double.NaN), null, "$"))
        assertEquals("Infinity", ConcatenateExpression.INSTANCE.evaluate(listOf(Double.POSITIVE_INFINITY), null, "$"))
        assertEquals(
            "-Infinity",
            ConcatenateExpression.INSTANCE.evaluate(listOf(Double.NEGATIVE_INFINITY), null, "$"),
        )
    }

    @Test
    fun booleansRenderAsTrueAndFalse() {
        assertEquals("truefalse", ConcatenateExpression.INSTANCE.evaluate(listOf(true, false), null, "$"))
    }

    @Test
    fun aNullArgumentThrowsRatherThanRenderingAsTheStringNull() {
        // Upstream renders every cat argument through a direct toString() call, not the null-safe
        // String.valueOf that log's string concatenation uses, so a null argument crashes here —
        // whether it is a literal null or a var lookup that resolves to null.
        assertFailsWith<NullPointerException> {
            ConcatenateExpression.INSTANCE.evaluate(listOf<Any?>(null), null, "$")
        }
        assertFailsWith<NullPointerException> {
            evaluate("""{"cat": [{"var": "x"}]}""", mapOf("x" to null), controlStringExpressions)
        }
    }

    @Test
    fun listArgumentsRenderInJavaCollectionFormatWithoutIntegerStripping() {
        assertEquals(
            "[1.0, a]",
            ConcatenateExpression.INSTANCE.evaluate(listOf(listOf(1.0, "a"), ""), null, "$"),
        )
    }

    @Test
    fun mapArgumentsRenderInJavaMapFormat() {
        assertEquals(
            "{a=1.0, b=x}",
            ConcatenateExpression.INSTANCE.evaluate(listOf(mapOf("a" to 1.0, "b" to "x"), ""), null, "$"),
        )
    }
}
