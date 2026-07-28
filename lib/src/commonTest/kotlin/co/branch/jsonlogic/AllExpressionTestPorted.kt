package co.branch.jsonlogic

import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Ported from upstream `AllExpressionTests`. Suffixed `Ported` because
 * `co.branch.jsonlogic.evaluator.AllExpressionTest` already covers this operator in more depth.
 */
class AllExpressionTestPorted {
    private val jsonLogic = JsonLogic()

    @Test
    fun testEmptyArray() {
        assertEquals(false, jsonLogic.apply("""{"all": [[], {">": [{"var": ""}, 0]}]}""", null).jsonPrimitive.boolean)
    }

    @Test
    fun testAll() {
        assertEquals(true, jsonLogic.apply("""{"all": [[1, 2, 3], {">": [{"var": ""}, 0]}]}""", null).jsonPrimitive.boolean)
        assertEquals(false, jsonLogic.apply("""{"all": [[1, 2, 3], {">": [{"var": ""}, 1]}]}""", null).jsonPrimitive.boolean)
    }
}
