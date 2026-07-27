package co.branch.jsonlogic

import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Ported from upstream `EqualityExpressionTests`. Suffixed `Ported` because
 * `co.branch.jsonlogic.evaluator.expressions.EqualityExpressionTest` already covers `==` in more
 * depth.
 */
class EqualityExpressionTestPorted {
    private val jsonLogic = JsonLogic()

    @Test
    fun testSameValueSameType() {
        assertEquals(true, jsonLogic.apply("""{"==": [1, 1]}""", null).jsonPrimitive.boolean)
    }

    @Test
    fun testSameValueDifferentType() {
        // Upstream's body is a verbatim duplicate of testSameValueSameType (the name promises a
        // different-type comparison the rule never makes); ported as literally written for 1:1
        // fidelity. See the workstream report for this note.
        assertEquals(true, jsonLogic.apply("""{"==": [1, 1]}""", null).jsonPrimitive.boolean)
    }

    @Test
    fun testDifferentValueDifferentType() {
        assertEquals(true, jsonLogic.apply("""{"==": [[], false]}""", null).jsonPrimitive.boolean)
    }

    @Test
    fun testEmptyStringAndZeroComparison() {
        assertEquals(true, jsonLogic.apply("""{"==": [" ", 0]}""", null).jsonPrimitive.boolean)
    }
}
