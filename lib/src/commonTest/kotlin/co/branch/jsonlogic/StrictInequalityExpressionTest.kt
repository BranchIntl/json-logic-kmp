package co.branch.jsonlogic

import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

/** Ported from upstream `StrictInequalityExpressionTests`. */
class StrictInequalityExpressionTest {
    private val jsonLogic = JsonLogic()

    @Test
    fun testSameValueSameType() {
        assertEquals(false, jsonLogic.apply("""{"!==": [1, 1.0]}""", null).jsonPrimitive.boolean)
    }

    @Test
    fun testSameValueDifferentType() {
        assertEquals(true, jsonLogic.apply("""{"!==": [1, "1"]}""", null).jsonPrimitive.boolean)
    }
}
