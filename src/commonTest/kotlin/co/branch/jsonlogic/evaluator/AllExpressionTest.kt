package co.branch.jsonlogic.evaluator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Pins `all`, including its verdict on an empty array and the path it reports a failing element at. */
class AllExpressionTest {

    @Test
    fun everyElementMustSatisfyTheSubRule() {
        assertEquals(true, evaluateArrayOp("""{"all": [[1, 2], {"var": ""}]}"""))
        assertEquals(false, evaluateArrayOp("""{"all": [[1, 0, 2], {"var": ""}]}"""))
        assertEquals(true, evaluateArrayOp("""{"all": [[1, 2], true]}"""))
    }

    @Test
    fun anEmptyArrayIsFalse() {
        assertEquals(false, evaluateArrayOp("""{"all": [[], true]}"""))
        assertEquals(false, evaluateArrayOp("""{"all": [{"var": ""}, true]}""", emptyList<Any?>()))
    }

    @Test
    fun aNullFirstArgumentIsFalse() {
        assertEquals(false, evaluateArrayOp("""{"all": [null, true]}"""))
        assertEquals(false, evaluateArrayOp("""{"all": [{"var": "item"}, true]}""", emptyMap<String, Any?>()))
    }

    @Test
    fun aFirstArgumentThatIsNeitherNullNorListLikeIsAnError() {
        for (rule in listOf("""{"all": [1, true]}""", """{"all": ["ab", true]}""", """{"all": [{"var": ""}, true]}""")) {
            val exception = assertFailsWith<JsonLogicEvaluationException>(rule) {
                evaluateArrayOp(rule, mapOf("a" to 1.0))
            }

            assertEquals("first argument to all must be a valid array", exception.message)
            // The operator's own path, without the argument index the other array operations add.
            assertEquals("$.all", exception.jsonPath)
        }
    }

    @Test
    fun evaluationStopsAtTheFirstFalsyElement() {
        // The second element would fail the sub-rule, so reaching it would throw instead.
        assertEquals(false, evaluateArrayOp("""{"all": [[0, "x"], $NUMBERS_ONLY]}"""))
    }

    @Test
    fun everyElementIsReportedAtPathIndexOne() {
        for (rule in listOf(
            """{"all": [["x"], $NUMBERS_ONLY]}""",
            """{"all": [[1, "x"], $NUMBERS_ONLY]}""",
            """{"all": [[1, 1, "x"], $NUMBERS_ONLY]}""",
        )) {
            val exception = assertFailsWith<JsonLogicEvaluationException>(rule) { evaluateArrayOp(rule) }

            assertEquals(NUMBERS_ONLY_MESSAGE, exception.message)
            assertEquals("$.all[1].missing_some", exception.jsonPath)
        }
    }

    @Test
    fun allExpectsTwoArguments() {
        for (rule in listOf("""{"all": [1]}""", """{"all": [[1], 1, 1]}""")) {
            val exception = assertFailsWith<JsonLogicEvaluationException>(rule) { evaluateArrayOp(rule) }

            assertEquals("all expects exactly 2 arguments", exception.message)
            assertEquals("$.all", exception.jsonPath)
        }
    }
}
