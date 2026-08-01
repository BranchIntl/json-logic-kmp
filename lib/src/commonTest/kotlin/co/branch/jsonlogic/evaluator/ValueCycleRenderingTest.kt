package co.branch.jsonlogic.evaluator

import co.branch.jsonlogic.evaluator.expressions.LogExpression
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins that a rule handing a cyclic value to an operator that renders one terminates.
 *
 * `reduce` hands its reducer a single context map and mutates it in place, so a reducer returning its
 * own data — or a list built around it — leaves a cycle in the value it returns. Any operator that
 * renders a value can then be pointed at that from a rule alone, and rendering a cycle by recursion
 * exhausts the stack, which no caller can catch on Native or Wasm.
 *
 * Which container the rendering starts from is the argument list's doing, not the cycle's: a lone
 * list-like argument is unwrapped into the operands, so the same reduce reaches an operator as the
 * context map when it is the only argument and as the list holding that map when it is not.
 */
class ValueCycleRenderingTest {

    /** The reducer returns its own context, so the accumulator ends up the map that holds it. */
    private val selfHoldingMap = """{"reduce": [[1, 2], {"var": ""}, 0]}"""

    /** `merge` wraps the context in a list, so the cycle closes through two containers, not one. */
    private val mapInsideItsOwnList = """{"reduce": [[1, 2], {"merge": [{"var": ""}]}, 0]}"""

    private val expressions = arrayExpressions + controlStringExpressions

    @Test
    fun catRendersACyclicArgument() {
        assertEquals(
            "{current=2, accumulator=(this Map)}",
            evaluate("""{"cat": [$selfHoldingMap]}""", null, expressions),
        )
        assertEquals(
            "{current=2, accumulator=[(this Map)]}",
            evaluate("""{"cat": [$mapInsideItsOwnList]}""", null, expressions),
        )
        assertEquals(
            "[{current=2, accumulator=(this Collection)}]!",
            evaluate("""{"cat": [$mapInsideItsOwnList, "!"]}""", null, expressions),
        )
    }

    @Test
    fun substrSlicesACyclicSource() {
        assertEquals("{current=2", evaluate("""{"substr": [$selfHoldingMap, 0, 10]}""", null, expressions))
        assertEquals("[{current=2", evaluate("""{"substr": [$mapInsideItsOwnList, 0, 11]}""", null, expressions))
    }

    @Test
    fun inSearchesForACyclicNeedle() {
        assertEquals(false, evaluate("""{"in": [$selfHoldingMap, "nope"]}""", null, expressions))
        assertEquals(false, evaluate("""{"in": [$mapInsideItsOwnList, "nope"]}""", null, expressions))
        assertEquals(
            true,
            evaluate(
                """{"in": [$mapInsideItsOwnList, "x[{current=2, accumulator=(this Collection)}]y"]}""",
                null,
                expressions,
            ),
        )
    }

    @Test
    fun logWritesACyclicValue() {
        val lines = mutableListOf<String>()
        val logging = expressions + LogExpression(lines::add)

        evaluate("""{"log": [$selfHoldingMap]}""", null, logging)
        evaluate("""{"log": [$mapInsideItsOwnList]}""", null, logging)

        assertEquals(
            listOf(
                "JsonLogic: {current=2.0, accumulator=(this Map)}",
                "JsonLogic: {current=2.0, accumulator=[(this Map)]}",
            ),
            lines,
        )
    }
}
