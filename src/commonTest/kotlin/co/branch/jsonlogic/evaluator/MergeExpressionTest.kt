package co.branch.jsonlogic.evaluator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Pins `merge`: how deep it flattens, what it wraps, and that it preserves order and duplicates. */
class MergeExpressionTest {

    @Test
    fun listLikeArgumentsContributeTheirElements() {
        assertEquals(listOf(1.0, 2.0), evaluateArrayOp("""{"merge": [[1], [2]]}"""))
        assertEquals(listOf(1.0, 2.0, 3.0), evaluateArrayOp("""{"merge": [[1, 2], [3]]}"""))
        assertEquals(listOf(1.0), evaluateArrayOp("""{"merge": [[1], []]}"""))
        assertEquals(emptyList<Any?>(), evaluateArrayOp("""{"merge": []}"""))
    }

    @Test
    fun onlyOneLevelIsFlattened() {
        assertEquals(listOf(listOf(1.0, 2.0), 3.0), evaluateArrayOp("""{"merge": [[[1, 2]], [3]]}"""))
        assertEquals(
            listOf(listOf(listOf(1.0)), listOf(2.0), 3.0),
            evaluateArrayOp("""{"merge": [[[[1]], [2]], [3]]}"""),
        )
        assertEquals(listOf(1.0, 2.0, listOf(3.0)), evaluateArrayOp("""{"merge": [[1, [2, [3]]]]}"""))
    }

    @Test
    fun anythingElseContributesItself() {
        assertEquals(listOf(1.0, 2.0), evaluateArrayOp("""{"merge": [1, 2]}"""))
        assertEquals(listOf(1.0, 2.0), evaluateArrayOp("""{"merge": [1, [2]]}"""))
        assertEquals(listOf(1.0), evaluateArrayOp("""{"merge": 1}"""))
        assertEquals(listOf("ab", 1.0), evaluateArrayOp("""{"merge": ["ab", [1]]}"""))
        assertEquals(listOf(null, null), evaluateArrayOp("""{"merge": [null, [null]]}"""))
        assertEquals(
            listOf(mapOf("a" to 1.0), 1.0),
            evaluateArrayOp("""{"merge": [{"var": "m"}, [1]]}""", mapOf("m" to mapOf("a" to 1.0))),
        )
    }

    @Test
    fun orderAndDuplicatesAreKept() {
        assertEquals(listOf(3.0, 1.0, 2.0), evaluateArrayOp("""{"merge": [[3], [1], [2]]}"""))
        assertEquals(listOf(1.0, 1.0, 1.0), evaluateArrayOp("""{"merge": [[1, 1], [1]]}"""))
    }

    @Test
    fun aLoneListArgumentIsUnwrappedBeforeFlattening() {
        assertEquals(listOf(1.0, 2.0), evaluateArrayOp("""{"merge": [[1, 2]]}"""))
        assertEquals(listOf(1.0, 2.0), evaluateArrayOp("""{"merge": [[[1, 2]]]}"""))
        assertEquals(listOf(listOf(listOf(1.0))), evaluateArrayOp("""{"merge": [[[[[1]]]]]}"""))
    }

    @Test
    fun elementsAreNormalizedToDoubles() {
        assertEquals(
            listOf(1.0, 2.0, 9.0),
            evaluateArrayOp("""{"merge": [{"var": "i"}, [9]]}""", mapOf("i" to listOf(1, 2))),
        )
    }

    @Test
    fun aFailingArgumentReportsItsOwnPath() {
        val exception = assertFailsWith<JsonLogicEvaluationException> {
            evaluateArrayOp("""{"merge": [1, 2, {"missing_some": [1]}]}""")
        }

        assertEquals(NUMBERS_ONLY_MESSAGE, exception.message)
        assertEquals("$.merge[2].missing_some", exception.jsonPath)
    }
}
