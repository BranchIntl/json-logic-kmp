package co.branch.jsonlogic

import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Ported from upstream `NumericComparisonExpressionTests`. Suffixed `Ported` because
 * `co.branch.jsonlogic.evaluator.expressions.NumericComparisonExpressionTest` already covers `<`,
 * `<=`, `>`, `>=` in more depth.
 */
class NumericComparisonExpressionTestPorted {
    private val jsonLogic = JsonLogic()

    @Test
    fun testLessThan() {
        val result = jsonLogic.apply("""{"<" : [1, 2]}""", null)

        assertEquals(true, result.jsonPrimitive.boolean)
    }

    @Test
    fun testLessThanOrEqual() {
        val result = jsonLogic.apply("""{"<=" : [1, 1]}""", null)

        assertEquals(true, result.jsonPrimitive.boolean)
    }

    @Test
    fun testGreaterThan() {
        val result = jsonLogic.apply("""{">" : [2, 1]}""", null)

        assertEquals(true, result.jsonPrimitive.boolean)
    }

    @Test
    fun testGreaterThanOrEqual() {
        val result = jsonLogic.apply("""{">=" : [1, 1]}""", null)

        assertEquals(true, result.jsonPrimitive.boolean)
    }

    @Test
    fun testBetweenExclusive() {
        val result = jsonLogic.apply("""{"<" : [1, 2, 3]}""", null)

        assertEquals(true, result.jsonPrimitive.boolean)
    }

    @Test
    fun testBetweenInclusive() {
        val result = jsonLogic.apply("""{"<=" : [1, 1, 3]}""", null)

        assertEquals(true, result.jsonPrimitive.boolean)
    }

    @Test
    fun testGtBetweenExclusive() {
        val result = jsonLogic.apply("""{">" : [3, 2, 1]}""", null)

        assertEquals(true, result.jsonPrimitive.boolean)
    }

    @Test
    fun testGtBetweenInclusive() {
        val result = jsonLogic.apply("""{">=" : [3, 1, 1]}""", null)

        assertEquals(true, result.jsonPrimitive.boolean)
    }

    @Test
    fun testEdgeCases() {
        assertEquals(true, jsonLogic.apply("""{">=" : [3, 1, 1, 1]}""", null).jsonPrimitive.boolean)
        assertEquals(false, jsonLogic.apply("""{">=" : [3, 1, 3, 1]}""", null).jsonPrimitive.boolean)
    }
}
