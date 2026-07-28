package co.branch.jsonlogic

import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Ported from upstream `NotExpressionTests`. Suffixed `Ported` because
 * `co.branch.jsonlogic.evaluator.NotExpressionTest` already covers `!`/`!!` in more depth.
 */
class NotExpressionTestPorted {
    private val jsonLogic = JsonLogic()

    @Test
    fun testSingleBoolean() {
        assertEquals(true, jsonLogic.apply("""{"!": false}""", null).jsonPrimitive.boolean)
    }

    @Test
    fun testSingleNumber() {
        assertEquals(true, jsonLogic.apply("""{"!": 0}""", null).jsonPrimitive.boolean)
    }

    @Test
    fun testSingleString() {
        assertEquals(true, jsonLogic.apply("""{"!": ""}""", null).jsonPrimitive.boolean)
    }

    @Test
    fun testSingleArray() {
        assertEquals(true, jsonLogic.apply("""{"!": []}""", null).jsonPrimitive.boolean)
    }

    @Test
    fun testDoubleBoolean() {
        assertEquals(false, jsonLogic.apply("""{"!!": false}""", null).jsonPrimitive.boolean)
    }

    @Test
    fun testDoubleNumber() {
        assertEquals(false, jsonLogic.apply("""{"!!": 0}""", null).jsonPrimitive.boolean)
    }

    @Test
    fun testDoubleString() {
        assertEquals(false, jsonLogic.apply("""{"!!": ""}""", null).jsonPrimitive.boolean)
    }

    @Test
    fun testDoubleArray() {
        assertEquals(false, jsonLogic.apply("""{"!!": [[]]}""", null).jsonPrimitive.boolean)
    }
}
