package co.branch.jsonlogic

import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Ported from upstream `SubstringExpressionTests`. Suffixed `Ported` because
 * `co.branch.jsonlogic.evaluator.SubstringExpressionTest` already covers `substr` in more depth.
 */
class SubstringExpressionTestPorted {
    private val jsonLogic = JsonLogic()

    @Test
    fun testSubstringSingleArg() {
        assertEquals("logic", jsonLogic.apply("""{"substr": ["jsonlogic", 4]}""", null).jsonPrimitive.content)
    }

    @Test
    fun testSubstringSingleNegativeArg() {
        assertEquals("logic", jsonLogic.apply("""{"substr": ["jsonlogic", -5]}""", null).jsonPrimitive.content)
    }

    @Test
    fun testSubstringDoubleArg() {
        assertEquals("son", jsonLogic.apply("""{"substr": ["jsonlogic", 1, 3]}""", null).jsonPrimitive.content)
    }

    @Test
    fun testSubstringDoubleNegativeArg() {
        assertEquals("log", jsonLogic.apply("""{"substr": ["jsonlogic", 4, -2]}""", null).jsonPrimitive.content)
    }

    @Test
    fun testSubstringSingleArgOutOfBounds() {
        assertEquals("", jsonLogic.apply("""{"substr": ["jsonlogic", -40]}""", null).jsonPrimitive.content)
    }

    @Test
    fun testSubstringDoubleArgOutOfBounds() {
        assertEquals("", jsonLogic.apply("""{"substr": ["jsonlogic", 20, -40]}""", null).jsonPrimitive.content)
    }
}
