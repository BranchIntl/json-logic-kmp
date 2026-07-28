package co.branch.jsonlogic.evaluator

import co.branch.jsonlogic.evaluator.expressions.PreEvaluatedArgumentsExpression
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins `!`/`!!`'s truthy table and their shared no-argument default. A sole list argument is
 * excluded from these spot checks because [PreEvaluatedArgumentsExpression]'s own unwrapping of a
 * lone list-like argument would otherwise confound what is under test here; a second argument keeps
 * the first one intact so its truthiness is what actually reaches `!`/`!!`.
 */
class NotExpressionTest {

    @Test
    fun singleBangNegatesTruthiness() {
        assertEquals(true, evaluate("""{"!": [false]}""", null, controlStringExpressions))
        assertEquals(false, evaluate("""{"!": [true]}""", null, controlStringExpressions))
        assertEquals(true, evaluate("""{"!": [0]}""", null, controlStringExpressions))
        assertEquals(false, evaluate("""{"!": [1]}""", null, controlStringExpressions))
        assertEquals(true, evaluate("""{"!": [""]}""", null, controlStringExpressions))
        assertEquals(false, evaluate("""{"!": ["0"]}""", null, controlStringExpressions))
        assertEquals(true, evaluate("""{"!": [[], null]}""", null, controlStringExpressions))
        assertEquals(false, evaluate("""{"!": [[1], null]}""", null, controlStringExpressions))
    }

    @Test
    fun doubleBangReflectsTruthiness() {
        assertEquals(false, evaluate("""{"!!": [false]}""", null, controlStringExpressions))
        assertEquals(true, evaluate("""{"!!": [true]}""", null, controlStringExpressions))
        assertEquals(false, evaluate("""{"!!": [0]}""", null, controlStringExpressions))
        assertEquals(true, evaluate("""{"!!": [1]}""", null, controlStringExpressions))
        assertEquals(false, evaluate("""{"!!": [[], null]}""", null, controlStringExpressions))
        assertEquals(true, evaluate("""{"!!": [[1], null]}""", null, controlStringExpressions))
    }

    @Test
    fun aMissingArgumentIsTreatedAsFalsy() {
        assertEquals(true, evaluate("""{"!": []}""", null, controlStringExpressions))
        assertEquals(false, evaluate("""{"!!": []}""", null, controlStringExpressions))
    }
}
