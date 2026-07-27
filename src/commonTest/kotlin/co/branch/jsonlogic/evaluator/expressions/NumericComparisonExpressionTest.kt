package co.branch.jsonlogic.evaluator.expressions

import co.branch.jsonlogic.evaluator.JsonLogicEvaluationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Pins the ordering comparisons: two arguments, the three-argument between, and their failures. */
class NumericComparisonExpressionTest {

    @Test
    fun twoArgumentsCompareDirectly() {
        assertEquals(true, evaluateRule("""{">": [2, 1]}"""))
        assertEquals(false, evaluateRule("""{">": [1, 1]}"""))
        assertEquals(true, evaluateRule("""{">=": [1, 1]}"""))
        assertEquals(false, evaluateRule("""{">=": [1, 2]}"""))
        assertEquals(true, evaluateRule("""{"<": [1, 2]}"""))
        assertEquals(false, evaluateRule("""{"<": [1, 1]}"""))
        assertEquals(true, evaluateRule("""{"<=": [1, 1]}"""))
        assertEquals(false, evaluateRule("""{"<=": [2, 1]}"""))
    }

    @Test
    fun threeArgumentsCompareAsABetween() {
        assertEquals(true, evaluateRule("""{"<": [1, 2, 3]}"""))
        assertEquals(false, evaluateRule("""{"<": [1, 1, 3]}"""))
        assertEquals(false, evaluateRule("""{"<": [1, 4, 3]}"""))
        assertEquals(true, evaluateRule("""{"<=": [1, 1, 3]}"""))
        assertEquals(false, evaluateRule("""{"<=": [1, 4, 3]}"""))
        assertEquals(true, evaluateRule("""{">": [3, 2, 1]}"""))
        assertEquals(false, evaluateRule("""{">": [3, 3, 1]}"""))
        assertEquals(true, evaluateRule("""{">=": [1, 1, 1]}"""))
    }

    @Test
    fun onlyTheFirstThreeArgumentsAreLookedAt() {
        assertEquals(true, evaluateRule("""{"<": [1, 2, 3, 4]}"""))
        assertEquals(true, evaluateRule("""{"<": [1, 2, 3, 0]}"""))
        assertEquals(false, evaluateRule("""{"<": [3, 2, 1, 0]}"""))
        assertEquals(true, evaluateRule("""{">": [3, 2, 1, 0]}"""))
        // A fourth argument that is not a number at all is still never read.
        assertEquals(true, evaluateRule("""{"<": [1, 2, 3, "four"]}"""))
    }

    @Test
    fun aSoleListArgumentIsSpreadIntoArguments() {
        assertEquals(true, evaluateRule("""{"<": [[1, 2]]}"""))
        assertEquals(true, evaluateRule("""{"<": [[1, 2, 3]]}"""))
        assertEquals(false, evaluateRule("""{"<": [[2, 1]]}"""))
    }

    @Test
    fun stringArgumentsAreReadAsJavaWouldParseThem() {
        assertEquals(true, evaluateRule("""{">": ["2", 1]}"""))
        assertEquals(true, evaluateRule("""{"<": [" 1 ", 2]}"""))
        assertEquals(true, evaluateRule("""{"<": ["1", 2, "3"]}"""))
    }

    @Test
    fun anArgumentNoNumberCanBeReadOutOfIsFalse() {
        assertEquals(false, evaluateRule("""{"<": [1, "x"]}"""))
        assertEquals(false, evaluateRule("""{"<": ["x", 1]}"""))
        assertEquals(false, evaluateRule("""{"<": [1, ""]}"""))
        assertEquals(false, evaluateRule("""{"<": [1, null]}"""))
        assertEquals(false, evaluateRule("""{"<": [null, 1]}"""))
        assertEquals(false, evaluateRule("""{"<": [1, true]}"""))
        assertEquals(false, evaluateRule("""{"<": [[1], [2]]}"""))
        // A third argument that cannot be read is false even where the first two decide the between.
        assertEquals(false, evaluateRule("""{"<": [1, 2, "x"]}"""))
        assertEquals(false, evaluateRule("""{"<": [1, 2, null]}"""))
        assertEquals(false, evaluateRule("""{">": [0, 1, 2, "three", 4, 5]}"""))
    }

    @Test
    fun nanIsNeverOrdered() {
        assertEquals(false, evaluateRule("""{"<": [{"/": [0, 0]}, 1]}"""))
        assertEquals(false, evaluateRule("""{"<": [1, {"/": [0, 0]}]}"""))
        assertEquals(false, evaluateRule("""{">": [{"/": [0, 0]}, 1]}"""))
        assertEquals(false, evaluateRule("""{"<=": [{"/": [0, 0]}, {"/": [0, 0]}]}"""))
        assertEquals(false, evaluateRule("""{">=": [{"/": [0, 0]}, {"/": [0, 0]}]}"""))
    }

    @Test
    fun theTwoZerosCompareEqual() {
        assertEquals(false, evaluateRule("""{">": [{"-": [0]}, 0]}"""))
        assertEquals(true, evaluateRule("""{">=": [{"-": [0]}, 0]}"""))
        assertEquals(true, evaluateRule("""{"<=": [0, {"-": [0]}]}"""))
    }

    @Test
    fun fewerThanTwoArgumentsIsAnError() {
        for (key in listOf("<", "<=", ">", ">=")) {
            val failure = assertFailsWith<JsonLogicEvaluationException> { evaluateRule("""{"$key": [1]}""") }

            assertEquals("'$key' requires at least 2 arguments", failure.message)
            assertEquals("$.$key", failure.jsonPath)
        }

        assertFailsWith<JsonLogicEvaluationException> { evaluateRule("""{"<": []}""") }
        // The spread leaves a single argument here too.
        assertFailsWith<JsonLogicEvaluationException> { evaluateRule("""{"<": [[1]]}""") }
    }

    @Test
    fun theErrorNamesTheFailingSubExpression() {
        val failure = assertFailsWith<JsonLogicEvaluationException> { evaluateRule("""{"+": [1, {">": [1]}]}""") }

        assertEquals("'>' requires at least 2 arguments", failure.message)
        assertEquals("$.+[1].>", failure.jsonPath)
    }
}
