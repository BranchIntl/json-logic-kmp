package co.branch.jsonlogic

import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

/** Ported from upstream `InequalityExpressionTests`. */
class InequalityExpressionTest {
    private val jsonLogic = JsonLogic()

    @Test
    fun testDifferentValueSameType() {
        assertEquals(true, jsonLogic.apply("""{"!=": [1, 2]}""", null).jsonPrimitive.boolean)
    }

    @Test
    fun testSameValueDifferentType() {
        assertEquals(false, jsonLogic.apply("""{"!=": [1.0, "1"]}""", null).jsonPrimitive.boolean)
    }
}
