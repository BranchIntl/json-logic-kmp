package co.branch.jsonlogic.evaluator

import co.branch.jsonlogic.evaluator.expressions.LogExpression
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins that an operator rendering a cyclic value terminates.
 *
 * No built-in operation builds a cycle, but a custom operation can return one, and rendering a cycle
 * by recursion exhausts the stack, which no caller can catch on Native or Wasm.
 *
 * Which container the rendering starts from is the argument list's doing: a lone list-like argument
 * is unwrapped into the operands, so `cycleList` reaches an operator as the map inside it when it is
 * the only argument and as the list itself when it is not.
 */
class ValueCycleRenderingTest {

    /** A map that holds itself under `self`. */
    private val selfHoldingMap = mutableMapOf<String, Any?>("a" to 1.0).also { it["self"] = it }

    /** A list holding a map that holds the list, so the cycle closes through two containers. */
    private val cycleList = mutableListOf<Any?>().also { list ->
        list += mutableMapOf<String, Any?>("a" to 1.0, "list" to list)
    }

    private val expressions =
        arrayExpressions + controlStringExpressions +
            ConstantExpression("cycleMap", selfHoldingMap) + ConstantExpression("cycleList", cycleList)

    @Test
    fun catRendersACyclicArgument() {
        assertEquals("{a=1, self=(this Map)}", evaluate("""{"cat": [{"cycleMap": []}]}""", null, expressions))
        assertEquals("{a=1, list=[(this Map)]}", evaluate("""{"cat": [{"cycleList": []}]}""", null, expressions))
        assertEquals(
            "[{a=1, list=(this Collection)}]!",
            evaluate("""{"cat": [{"cycleList": []}, "!"]}""", null, expressions),
        )
    }

    @Test
    fun substrSlicesACyclicSource() {
        assertEquals("{a=1, self", evaluate("""{"substr": [{"cycleMap": []}, 0, 10]}""", null, expressions))
    }

    @Test
    fun inSearchesForACyclicNeedle() {
        assertEquals(false, evaluate("""{"in": [{"cycleMap": []}, "nope"]}""", null, expressions))
        assertEquals(
            true,
            evaluate("""{"in": [{"cycleMap": []}, "x{a=1, self=(this Map)}y"]}""", null, expressions),
        )
    }

    @Test
    fun logWritesACyclicValue() {
        val lines = mutableListOf<String>()

        evaluate("""{"log": [{"cycleMap": []}]}""", null, expressions + LogExpression(lines::add))

        assertEquals(listOf("JsonLogic: {a=1.0, self=(this Map)}"), lines)
    }
}
