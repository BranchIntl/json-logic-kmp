package co.branch.jsonlogic.evaluator

import co.branch.jsonlogic.evaluator.expressions.ConcatenateExpression
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins `cat`'s rendering of every domain shape. Edge doubles (NaN, Infinity, `1e7`) and nested
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
    fun wholeNumbersRenderWithoutADecimalPoint() {
        assertEquals("12", evaluate("""{"cat": [1, 2]}""", null, controlStringExpressions))
        assertEquals("Robocop2", evaluate("""{"cat": ["Robocop", 2]}""", null, controlStringExpressions))
        assertEquals("1", ConcatenateExpression.INSTANCE.evaluate(listOf(1.0), null, "$"))
        assertEquals("-2", ConcatenateExpression.INSTANCE.evaluate(listOf(-2.0), null, "$"))
        assertEquals("0", ConcatenateExpression.INSTANCE.evaluate(listOf(0.0), null, "$"))
        assertEquals("0", ConcatenateExpression.INSTANCE.evaluate(listOf(-0.0), null, "$"))
        assertEquals("9999999", ConcatenateExpression.INSTANCE.evaluate(listOf(9999999.0), null, "$"))
        assertEquals("00", ConcatenateExpression.INSTANCE.evaluate(listOf(0.0, -0.0), null, "$"))
    }

    @Test
    fun nonIntegralNumbersRenderInFull() {
        assertEquals("1.5", ConcatenateExpression.INSTANCE.evaluate(listOf(1.5), null, "$"))
        assertEquals("0.1", ConcatenateExpression.INSTANCE.evaluate(listOf(0.1), null, "$"))
        assertEquals("-2.5", ConcatenateExpression.INSTANCE.evaluate(listOf(-2.5), null, "$"))
        assertEquals("-2.1.5", ConcatenateExpression.INSTANCE.evaluate(listOf(-2.0, ".", 1.5), null, "$"))
    }

    /** The magnitudes either side of where ECMAScript switches to exponential notation. */
    @Test
    fun largeAndSmallMagnitudesFollowTheEcmaScriptNotationSwitch() {
        assertEquals("10000000", ConcatenateExpression.INSTANCE.evaluate(listOf(1e7), null, "$"))
        assertEquals("100000000000000000000", ConcatenateExpression.INSTANCE.evaluate(listOf(1e20), null, "$"))
        assertEquals("1e+21", ConcatenateExpression.INSTANCE.evaluate(listOf(1e21), null, "$"))
        assertEquals("0.000001", ConcatenateExpression.INSTANCE.evaluate(listOf(1e-6), null, "$"))
        assertEquals("1e-7", ConcatenateExpression.INSTANCE.evaluate(listOf(1e-7), null, "$"))
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
    fun aNullArgumentContributesNothing() {
        assertEquals("", ConcatenateExpression.INSTANCE.evaluate(listOf<Any?>(null), null, "$"))
        assertEquals("test", ConcatenateExpression.INSTANCE.evaluate(listOf(null, "test", null), null, "$"))
        assertEquals(
            "Welcome back, ",
            evaluate("""{"cat": ["Welcome back, ", {"var": "x"}]}""", mapOf("x" to null), controlStringExpressions),
        )
    }

    /** A var that resolves to nothing reads the same as a literal null, and is the common way to get one. */
    @Test
    fun aMissingVariableContributesNothing() {
        assertEquals(
            "Welcome back, ",
            evaluate("""{"cat": ["Welcome back, ", {"var": "name"}]}""", emptyMap<String, Any?>(), controlStringExpressions),
        )
    }

    @Test
    fun listArgumentsRenderInJavaCollectionFormat() {
        assertEquals(
            "[1, a]",
            ConcatenateExpression.INSTANCE.evaluate(listOf(listOf(1.0, "a"), ""), null, "$"),
        )
    }

    @Test
    fun mapArgumentsRenderInJavaMapFormat() {
        assertEquals(
            "{a=1, b=x}",
            ConcatenateExpression.INSTANCE.evaluate(listOf(mapOf("a" to 1.0, "b" to "x"), ""), null, "$"),
        )
    }
}
