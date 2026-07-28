package co.branch.jsonlogic.evaluator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Pins `map`: the sub-rule's data, the shape of the result, and what a non-array first argument does. */
class MapExpressionTest {

    private val desserts = mapOf(
        "desserts" to listOf(
            mapOf("name" to "apple", "qty" to 1.0),
            mapOf("name" to "brownie", "qty" to 2.0),
        ),
    )

    @Test
    fun eachElementIsTheSubRulesData() {
        assertEquals(listOf(1.0, 2.0), evaluateArrayOp("""{"map": [[1, 2], {"var": ""}]}"""))
        assertEquals(listOf(1.0, 2.0), evaluateArrayOp("""{"map": [{"var": "desserts"}, {"var": "qty"}]}""", desserts))
    }

    @Test
    fun elementsKeepTheirShape() {
        assertEquals(
            listOf(listOf(1.0, 2.0), listOf(3.0)),
            evaluateArrayOp("""{"map": [[[1, 2], [3]], {"var": ""}]}"""),
        )
        assertEquals(listOf(2.0, 4.0), evaluateArrayOp("""{"map": [[[1, 2], [3, 4]], {"var": "1"}]}"""))
    }

    @Test
    fun anEmptyArrayMapsToAnEmptyList() {
        assertEquals(emptyList<Any?>(), evaluateArrayOp("""{"map": [[], {"var": ""}]}"""))
    }

    @Test
    fun aFirstArgumentThatIsNotListLikeIsAnEmptyList() {
        assertEquals(emptyList<Any?>(), evaluateArrayOp("""{"map": [1, {"var": ""}]}"""))
        assertEquals(emptyList<Any?>(), evaluateArrayOp("""{"map": ["ab", {"var": ""}]}"""))
        assertEquals(emptyList<Any?>(), evaluateArrayOp("""{"map": [null, {"var": ""}]}"""))
        assertEquals(emptyList<Any?>(), evaluateArrayOp("""{"map": [{"var": "nope"}, {"var": ""}]}""", desserts))
        assertEquals(emptyList<Any?>(), evaluateArrayOp("""{"map": [{"var": ""}, {"var": ""}]}""", desserts))
    }

    @Test
    fun elementsAreNormalizedToDoubles() {
        assertEquals(listOf(1.0, 2.0), evaluateArrayOp("""{"map": [{"var": "i"}, {"var": ""}]}""", mapOf("i" to listOf(1, 2))))
    }

    @Test
    fun mapExpectsTwoArguments() {
        for (rule in listOf("""{"map": [1]}""", """{"map": [[1], 1, 1]}""")) {
            val exception = assertFailsWith<JsonLogicEvaluationException>(rule) { evaluateArrayOp(rule) }

            assertEquals("map expects exactly 2 arguments", exception.message)
            assertEquals("$.map", exception.jsonPath)
        }
    }

    @Test
    fun aFailingSubRuleReportsTheSecondArgumentsPath() {
        val exception = assertFailsWith<JsonLogicEvaluationException> {
            evaluateArrayOp("""{"map": [[1, 1, "x"], $NUMBERS_ONLY]}""")
        }

        assertEquals(NUMBERS_ONLY_MESSAGE, exception.message)
        assertEquals("$.map[1].missing_some", exception.jsonPath)
    }
}
