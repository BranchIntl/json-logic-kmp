package co.branch.jsonlogic.evaluator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Pins `and`/`or`'s short-circuiting, its deciding-value return, and its argument-count check. */
class LogicExpressionTest {

    @Test
    fun requiresAtLeastOneArgument() {
        for (operator in listOf("and", "or")) {
            val exception = assertFailsWith<JsonLogicEvaluationException> {
                evaluate("""{"$operator": []}""", null, controlStringExpressions)
            }

            assertEquals("$operator operator expects at least 1 argument", exception.message)
            assertEquals("$.$operator", exception.jsonPath)
        }
    }

    @Test
    fun andReturnsTheDecidingValueNotABoolean() {
        assertEquals(3.0, evaluate("""{"and": [1, 3]}""", null, controlStringExpressions))
        assertEquals(false, evaluate("""{"and": [3, false]}""", null, controlStringExpressions))
        assertEquals(false, evaluate("""{"and": [false, 3]}""", null, controlStringExpressions))
    }

    @Test
    fun orReturnsTheDecidingValueNotABoolean() {
        assertEquals(1.0, evaluate("""{"or": [1, 3]}""", null, controlStringExpressions))
        assertEquals(3.0, evaluate("""{"or": [3, false]}""", null, controlStringExpressions))
        assertEquals(3.0, evaluate("""{"or": [false, 3]}""", null, controlStringExpressions))
    }

    @Test
    fun andOnAllTruthyArgumentsReturnsTheLastOne() {
        assertEquals(true, evaluate("""{"and": [true, true, true]}""", null, controlStringExpressions))
    }

    @Test
    fun orOnAllFalsyArgumentsReturnsTheLastOne() {
        assertEquals(false, evaluate("""{"or": [false, false, false]}""", null, controlStringExpressions))
    }

    @Test
    fun andShortCircuitsAtTheFirstFalsyValue() {
        val untaken = CountingExpression("untaken", "never")
        val expressions = controlStringExpressions + untaken

        val result = evaluate("""{"and": [false, {"untaken": []}]}""", null, expressions)

        assertEquals(false, result)
        assertEquals(0, untaken.evaluationCount)
    }

    @Test
    fun orShortCircuitsAtTheFirstTruthyValue() {
        val untaken = CountingExpression("untaken", "never")
        val expressions = controlStringExpressions + untaken

        val result = evaluate("""{"or": [true, {"untaken": []}]}""", null, expressions)

        assertEquals(true, result)
        assertEquals(0, untaken.evaluationCount)
    }

    @Test
    fun jsonPathIsExtendedPerArgumentIndex() {
        val exception = assertFailsWith<JsonLogicEvaluationException> {
            evaluate("""{"and": [true, {"missing_some": [1]}]}""", emptyMap<String, Any?>(), controlStringExpressions)
        }

        assertEquals("$.and[1].missing_some", exception.jsonPath)
    }
}
