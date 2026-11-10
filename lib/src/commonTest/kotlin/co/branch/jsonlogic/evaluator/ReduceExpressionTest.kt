package co.branch.jsonlogic.evaluator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Pins `reduce`: the context object it hands the reducer, and where the initial accumulator comes from. */
class ReduceExpressionTest {

    private val outer = mapOf("seed" to 100.0, "integers" to listOf(1, 2, 3), "outer" to "OUTER")

    @Test
    fun theReducerFoldsCurrentIntoAccumulator() {
        assertEquals(
            listOf(1.0, 2.0, 3.0),
            evaluateArrayOp("""{"reduce": [{"var": "integers"}, {"merge": [{"var": "accumulator"}, {"var": "current"}]}, []]}""", outer),
        )
    }

    @Test
    fun theContextHasExactlyCurrentAndAccumulator() {
        assertEquals(
            listOf("x"),
            evaluateArrayOp("""{"reduce": [[1], {"missing": ["current", "accumulator", "x"]}, 0]}"""),
        )
    }

    @Test
    fun theOuterDataIsNotReachableFromTheReducer() {
        assertEquals(null, evaluateArrayOp("""{"reduce": [[1], {"var": "outer"}, 0]}""", outer))
        assertEquals("dflt", evaluateArrayOp("""{"reduce": [[1], {"var": ["outer", "dflt"]}, 0]}""", outer))
    }

    @Test
    fun theInitialAccumulatorIsEvaluatedAgainstTheOuterData() {
        assertEquals(100.0, evaluateArrayOp("""{"reduce": [[1, 2], {"var": "accumulator"}, {"var": "seed"}]}""", outer))
        assertEquals(listOf(1.0, 2.0), evaluateArrayOp("""{"reduce": [[], {"var": "accumulator"}, [1, 2]]}"""))
        assertEquals("s", evaluateArrayOp("""{"reduce": [[1, 2], {"var": "accumulator"}, "s"]}"""))
    }

    @Test
    fun currentIsTheElementItself() {
        assertEquals(listOf(3.0), evaluateArrayOp("""{"reduce": [[[1, 2], [3]], {"var": "current"}, 0]}"""))
    }

    @Test
    fun eachIterationGetsANewContext() {
        val result = evaluateArrayOp("""{"reduce": [[1, 2], {"var": ""}, 0]}""")

        assertEquals(mapOf("current" to 2.0, "accumulator" to mapOf("current" to 1.0, "accumulator" to 0.0)), result)
    }

    @Test
    fun theContextKeepsCurrentBeforeAccumulator() {
        // `in` renders a non-string first argument, which is what exposes the context's key order.
        assertEquals(
            true,
            evaluateArrayOp("""{"in": [{"reduce": [[1], {"var": ""}, 0]}, "{current=1, accumulator=0}"]}"""),
        )
        assertEquals(
            false,
            evaluateArrayOp("""{"in": [{"reduce": [[1], {"var": ""}, 0]}, "{accumulator=0, current=1}"]}"""),
        )
    }

    @Test
    fun anEmptyArrayYieldsTheInitialAccumulator() {
        assertEquals(99.0, evaluateArrayOp("""{"reduce": [[], {"var": "current"}, 99]}"""))
    }

    @Test
    fun aFirstArgumentThatIsNotListLikeYieldsTheInitialAccumulator() {
        assertEquals(42.0, evaluateArrayOp("""{"reduce": [1, {"var": "current"}, 42]}"""))
        assertEquals(42.0, evaluateArrayOp("""{"reduce": ["ab", {"var": "current"}, 42]}"""))
        assertEquals(42.0, evaluateArrayOp("""{"reduce": [null, {"var": "current"}, 42]}"""))
        assertEquals(42.0, evaluateArrayOp("""{"reduce": [{"var": "nope"}, {"var": "current"}, 42]}""", outer))
        assertEquals(42.0, evaluateArrayOp("""{"reduce": [{"var": ""}, {"var": "current"}, 42]}""", outer))
    }

    @Test
    fun theInitialAccumulatorIsEvaluatedEvenWhenTheFoldDoesNotRun() {
        val exception = assertFailsWith<JsonLogicEvaluationException> {
            evaluateArrayOp("""{"reduce": [1, 2, {"missing_some": [1]}]}""")
        }

        assertEquals(NUMBERS_ONLY_MESSAGE, exception.message)
        assertEquals("$.reduce[2].missing_some", exception.jsonPath)
    }

    @Test
    fun reduceExpectsThreeArguments() {
        for (rule in listOf("""{"reduce": [1]}""", """{"reduce": [[1], 1]}""", """{"reduce": [[1], 1, 1, 1]}""")) {
            val exception = assertFailsWith<JsonLogicEvaluationException>(rule) { evaluateArrayOp(rule) }

            assertEquals("reduce expects exactly 3 arguments", exception.message)
            assertEquals("$.reduce", exception.jsonPath)
        }
    }

    @Test
    fun aFailingReducerReportsTheSecondArgumentsPath() {
        val exception = assertFailsWith<JsonLogicEvaluationException> {
            evaluateArrayOp("""{"reduce": [["x"], $NUMBERS_ONLY, 0]}""")
        }

        assertEquals(NUMBERS_ONLY_MESSAGE, exception.message)
        assertEquals("$.reduce[1].missing_some", exception.jsonPath)
    }
}
