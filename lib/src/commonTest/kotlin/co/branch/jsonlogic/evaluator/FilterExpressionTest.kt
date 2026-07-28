package co.branch.jsonlogic.evaluator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Pins `filter`: which elements survive, in what form, and when a first argument is rejected. */
class FilterExpressionTest {

    @Test
    fun aTruthySubRuleKeepsTheElement() {
        assertEquals(listOf(1.0, 2.0), evaluateArrayOp("""{"filter": [[1, 2], true]}"""))
        assertEquals(emptyList<Any?>(), evaluateArrayOp("""{"filter": [[1, 2], false]}"""))
    }

    @Test
    fun theElementsOwnTruthinessDecides() {
        assertEquals(
            listOf(1.0, "a", listOf(1.0)),
            evaluateArrayOp("""{"filter": [[0, 1, "", "a", null, [], [1]], {"var": ""}]}"""),
        )
    }

    @Test
    fun theElementIsKeptRatherThanTheSubRulesResult() {
        assertEquals(listOf(1.0, 2.0), evaluateArrayOp("""{"filter": [[1, 2], $NUMBERS_ONLY]}"""))
        assertEquals(listOf(listOf(1.0), listOf(2.0)), evaluateArrayOp("""{"filter": [[[1], [2]], true]}"""))
    }

    @Test
    fun anEmptyArrayFiltersToAnEmptyList() {
        assertEquals(emptyList<Any?>(), evaluateArrayOp("""{"filter": [[], true]}"""))
    }

    @Test
    fun elementsAreNormalizedToDoubles() {
        assertEquals(listOf(1.0, 2.0), evaluateArrayOp("""{"filter": [{"var": "i"}, true]}""", mapOf("i" to listOf(1, 2))))
    }

    @Test
    fun aFirstArgumentThatIsNotListLikeIsAnError() {
        for (rule in listOf(
            """{"filter": [1, true]}""",
            """{"filter": ["ab", true]}""",
            """{"filter": [null, true]}""",
            """{"filter": [{"var": "nope"}, true]}""",
        )) {
            val exception = assertFailsWith<JsonLogicEvaluationException>(rule) {
                evaluateArrayOp(rule, mapOf("a" to 1.0))
            }

            assertEquals("first argument to filter must be a valid array", exception.message)
            assertEquals("$.filter[0]", exception.jsonPath)
        }
    }

    @Test
    fun filterExpectsTwoArguments() {
        val exception = assertFailsWith<JsonLogicEvaluationException> { evaluateArrayOp("""{"filter": [0]}""") }

        assertEquals("filter expects exactly 2 arguments", exception.message)
        assertEquals("$.filter", exception.jsonPath)
    }

    @Test
    fun aFailingSubRuleReportsTheSecondArgumentsPath() {
        val exception = assertFailsWith<JsonLogicEvaluationException> {
            evaluateArrayOp("""{"filter": [[1, 1, "x"], $NUMBERS_ONLY]}""")
        }

        assertEquals(NUMBERS_ONLY_MESSAGE, exception.message)
        assertEquals("$.filter[1].missing_some", exception.jsonPath)
    }
}
