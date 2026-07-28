package co.branch.jsonlogic

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Ported from upstream `MergeExpressionTests`. Suffixed `Ported` because
 * `co.branch.jsonlogic.evaluator.MergeExpressionTest` already covers `merge` in more depth.
 */
class MergeExpressionTestPorted {
    private val jsonLogic = JsonLogic()

    @Test
    fun testMerge() {
        val result = jsonLogic.apply("""{"merge": [[1, 2], [3, 4]]}""", null).jsonArray

        assertEquals(4, result.size)
        assertEquals("1.0", result[0].jsonPrimitive.content)
        assertEquals("2.0", result[1].jsonPrimitive.content)
        assertEquals("3.0", result[2].jsonPrimitive.content)
        assertEquals("4.0", result[3].jsonPrimitive.content)
    }

    @Test
    fun testMergeWithNonArrays() {
        val result = jsonLogic.apply("""{"merge": [1, 2, [3, 4]]}""", null).jsonArray

        assertEquals(4, result.size)
        assertEquals("1.0", result[0].jsonPrimitive.content)
        assertEquals("2.0", result[1].jsonPrimitive.content)
        assertEquals("3.0", result[2].jsonPrimitive.content)
        assertEquals("4.0", result[3].jsonPrimitive.content)
    }
}
