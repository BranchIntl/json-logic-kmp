package co.branch.jsonlogic.evaluator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** Pins `if` and `?:`'s argument-count matrix, its laziness, and its per-argument jsonPath. */
class IfExpressionTest {

    @Test
    fun zeroArgumentsReturnsNull() {
        assertNull(evaluate("""{"if": []}""", null, controlStringExpressions))
    }

    @Test
    fun oneArgumentIsReturnedEvaluated() {
        assertEquals(true, evaluate("""{"if": [true]}""", null, controlStringExpressions))
        assertEquals("apple", evaluate("""{"if": ["apple"]}""", null, controlStringExpressions))
        // The sole argument is returned as-is, with no truthy conversion: a falsy 0 comes back as
        // the Double 0.0, not as a Boolean.
        assertEquals(0.0, evaluate("""{"if": [0]}""", null, controlStringExpressions))
    }

    @Test
    fun twoArgumentsIsATruthyGuardedThenWithNoElse() {
        assertEquals("apple", evaluate("""{"if": [true, "apple"]}""", null, controlStringExpressions))
        assertNull(evaluate("""{"if": [false, "apple"]}""", null, controlStringExpressions))
    }

    @Test
    fun threeArgumentsIsIfThenElse() {
        assertEquals("apple", evaluate("""{"if": [true, "apple", "banana"]}""", null, controlStringExpressions))
        assertEquals("banana", evaluate("""{"if": [false, "apple", "banana"]}""", null, controlStringExpressions))
    }

    @Test
    fun elseIfChainsPickTheFirstTruthyCondition() {
        assertEquals(
            "apple",
            evaluate("""{"if": [true, "apple", true, "banana"]}""", null, controlStringExpressions),
        )
        assertEquals(
            "banana",
            evaluate("""{"if": [false, "apple", true, "banana"]}""", null, controlStringExpressions),
        )
        assertNull(evaluate("""{"if": [false, "apple", false, "banana"]}""", null, controlStringExpressions))
    }

    @Test
    fun aTrailingUnpairedArgumentIsTheElse() {
        assertEquals(
            "carrot",
            evaluate("""{"if": [false, "apple", false, "banana", "carrot"]}""", null, controlStringExpressions),
        )
        assertEquals(
            "apple",
            evaluate("""{"if": [true, "apple", false, "banana", "carrot"]}""", null, controlStringExpressions),
        )
    }

    @Test
    fun withNoTrailingElseAnEvenArgumentCountFallsThroughToNull() {
        assertNull(
            evaluate(
                """{"if": [false, "apple", false, "banana", false, "carrot"]}""",
                null,
                controlStringExpressions,
            ),
        )
    }

    @Test
    fun ternaryIsTheSameThreeArgumentBehaviorUnderAnotherKey() {
        assertEquals(1.0, evaluate("""{"?:": [true, 1, 2]}""", null, controlStringExpressions))
        assertEquals(2.0, evaluate("""{"?:": [false, 1, 2]}""", null, controlStringExpressions))
    }

    @Test
    fun untakenBranchesAreNeverEvaluated() {
        val consequent = CountingExpression("consequent", "apple")
        val alternative = CountingExpression("alternative", "banana")
        val expressions = controlStringExpressions + listOf(consequent, alternative)

        val result = evaluate(
            """{"if": [true, {"consequent": []}, {"alternative": []}]}""",
            null,
            expressions,
        )

        assertEquals("apple", result)
        assertEquals(1, consequent.evaluationCount)
        assertEquals(0, alternative.evaluationCount)
    }

    @Test
    fun conditionsPastTheDecidingOneAreNeverEvaluated() {
        val firstCondition = CountingExpression("firstCondition", true)
        val secondCondition = CountingExpression("secondCondition", true)
        val expressions = controlStringExpressions + listOf(firstCondition, secondCondition)

        val result = evaluate(
            """{"if": [{"firstCondition": []}, "apple", {"secondCondition": []}, "banana"]}""",
            null,
            expressions,
        )

        assertEquals("apple", result)
        assertEquals(1, firstCondition.evaluationCount)
        assertEquals(0, secondCondition.evaluationCount)
    }

    @Test
    fun aFalsyConditionSkipsItsOwnConsequentButEvaluatesTheNextCondition() {
        val firstConsequent = CountingExpression("firstConsequent", "apple")
        val secondCondition = CountingExpression("secondCondition", true)
        val expressions = controlStringExpressions + listOf(firstConsequent, secondCondition)

        val result = evaluate(
            """{"if": [false, {"firstConsequent": []}, {"secondCondition": []}, "banana"]}""",
            null,
            expressions,
        )

        assertEquals("banana", result)
        assertEquals(0, firstConsequent.evaluationCount)
        assertEquals(1, secondCondition.evaluationCount)
    }

    @Test
    fun jsonPathIsExtendedPerArgumentIndex() {
        val exception = assertFailsWith<JsonLogicEvaluationException> {
            evaluate(
                """{"if": [true, {"missing_some": [1]}, false, {"missing_some": [1]}]}""",
                emptyMap<String, Any?>(),
                controlStringExpressions,
            )
        }

        assertEquals("$.if[1].missing_some", exception.jsonPath)
    }

    @Test
    fun jsonPathOfTheTrailingElseUsesItsOwnIndex() {
        val exception = assertFailsWith<JsonLogicEvaluationException> {
            evaluate(
                """{"if": [false, "apple", {"missing_some": [1]}]}""",
                emptyMap<String, Any?>(),
                controlStringExpressions,
            )
        }

        assertEquals("$.if[2].missing_some", exception.jsonPath)
    }
}
