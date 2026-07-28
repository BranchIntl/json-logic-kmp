package co.branch.jsonlogic

import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Ported from upstream `StrictEqualityExpressionTests`. Suffixed `Ported` because
 * `co.branch.jsonlogic.evaluator.expressions.StrictEqualityExpressionTest` already covers `===` in
 * more depth.
 */
class StrictEqualityExpressionTestPorted {
    private val jsonLogic = JsonLogic()

    @Test
    fun testSameValueSameType() {
        assertEquals(true, jsonLogic.apply("""{"===": [1, 1.0]}""", null).jsonPrimitive.boolean)
    }

    @Test
    fun testSameValueDifferentType() {
        assertEquals(false, jsonLogic.apply("""{"===": [1, "1"]}""", null).jsonPrimitive.boolean)
    }
}
