package co.branch.jsonlogic.evaluator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Pins the `some`/`none` truth table, including the empty-array and null first-argument cases. */
class ArrayHasExpressionTest {

    @Test
    fun someAndNoneAreOppositeVerdictsOnTheSameScan() {
        assertEquals(true, evaluateArrayOp("""{"some": [[0, 1], {"var": ""}]}"""))
        assertEquals(false, evaluateArrayOp("""{"none": [[0, 1], {"var": ""}]}"""))
        assertEquals(false, evaluateArrayOp("""{"some": [[0, 0], {"var": ""}]}"""))
        assertEquals(true, evaluateArrayOp("""{"none": [[0, 0], {"var": ""}]}"""))
    }

    @Test
    fun anEmptyArraySatisfiesNoneAndNotSome() {
        assertEquals(false, evaluateArrayOp("""{"some": [[], true]}"""))
        assertEquals(true, evaluateArrayOp("""{"none": [[], true]}"""))
    }

    @Test
    fun aNullFirstArgumentIsReadAsAnEmptyArray() {
        assertEquals(false, evaluateArrayOp("""{"some": [null, true]}"""))
        assertEquals(true, evaluateArrayOp("""{"none": [null, true]}"""))
        assertEquals(false, evaluateArrayOp("""{"some": [{"var": "nope"}, true]}""", mapOf("a" to 1.0)))
        assertEquals(true, evaluateArrayOp("""{"none": [{"var": "nope"}, true]}""", mapOf("a" to 1.0)))
    }

    @Test
    fun anElementsOwnTruthinessDecides() {
        assertEquals(true, evaluateArrayOp("""{"some": [[[1, 2], []], {"var": ""}]}"""))
        assertEquals(false, evaluateArrayOp("""{"some": [[[], []], {"var": ""}]}"""))
        assertEquals(true, evaluateArrayOp("""{"none": [[[], []], {"var": ""}]}"""))
    }

    @Test
    fun evaluationStopsAtTheFirstTruthyElement() {
        // The last element would fail the sub-rule, so reaching it would throw instead.
        assertEquals(true, evaluateArrayOp("""{"some": [[1, 1, "x"], $NUMBERS_ONLY]}"""))
        assertEquals(false, evaluateArrayOp("""{"none": [[1, 1, "x"], $NUMBERS_ONLY]}"""))
    }

    @Test
    fun aFirstArgumentThatIsNeitherNullNorListLikeIsAnError() {
        for ((rule, key) in listOf("""{"some": [0, 1]}""" to "some", """{"none": [1, 2]}""" to "none")) {
            val exception = assertFailsWith<JsonLogicEvaluationException>(rule) { evaluateArrayOp(rule) }

            assertEquals("first argument to $key must be a valid array", exception.message)
            assertEquals("$.$key[0]", exception.jsonPath)
        }
    }

    @Test
    fun bothExpectTwoArguments() {
        for ((rule, key) in listOf("""{"some": []}""" to "some", """{"none": [[1], 1, 1]}""" to "none")) {
            val exception = assertFailsWith<JsonLogicEvaluationException>(rule) { evaluateArrayOp(rule) }

            assertEquals("$key expects exactly 2 arguments", exception.message)
            assertEquals("$.$key", exception.jsonPath)
        }
    }

    @Test
    fun aFailingSubRuleReportsTheSecondArgumentsPath() {
        for (key in listOf("some", "none")) {
            val rule = """{"$key": [["x"], $NUMBERS_ONLY]}"""
            val exception = assertFailsWith<JsonLogicEvaluationException>(rule) { evaluateArrayOp(rule) }

            assertEquals(NUMBERS_ONLY_MESSAGE, exception.message)
            assertEquals("$.$key[1].missing_some", exception.jsonPath)
        }
    }
}
