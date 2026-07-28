package co.branch.jsonlogic

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Ported from upstream `MathExpressionTests`. Suffixed `Ported` because
 * `co.branch.jsonlogic.evaluator.expressions.MathExpressionTest` already covers arithmetic in more
 * depth.
 */
class MathExpressionTestPorted {
    private val jsonLogic = JsonLogic()

    @Test
    fun testAdd() {
        val result = jsonLogic.apply("""{"+":[4,2]}""", null)

        assertEquals("6.0", result.jsonPrimitive.content)
    }

    @Test
    fun testMultiAdd() {
        val result = jsonLogic.apply("""{"+":[2,2,2,2,2]}""", null)

        assertEquals("10.0", result.jsonPrimitive.content)
    }

    @Test
    fun testSingleAdd() {
        val result = jsonLogic.apply("""{"+" : "3.14"}""", null)

        assertEquals("3.14", result.jsonPrimitive.content)
    }

    @Test
    fun testAddWithArray() {
        val result = jsonLogic.apply("""{"+":[2,[[3,4],5]]}""", null)

        assertEquals("5.0", result.jsonPrimitive.content) // This matches reference impl at jsonlogic.com
    }

    @Test
    fun testStringAdd() {
        assertEquals(JsonNull, jsonLogic.apply("""{"+" : "foo"}""", null))
        assertEquals(JsonNull, jsonLogic.apply("""{"+" : ["foo"]}""", null))
        assertEquals(JsonNull, jsonLogic.apply("""{"+" : [1, "foo"]}""", null))
    }

    @Test
    fun testSubtract() {
        val result = jsonLogic.apply("""{"-":[4,2]}""", null)

        assertEquals("2.0", result.jsonPrimitive.content)
    }

    @Test
    fun testSingleSubtract() {
        val result = jsonLogic.apply("""{"-": 2 }""", null)

        assertEquals("-2.0", result.jsonPrimitive.content)
    }

    @Test
    fun testSingleSubtractString() {
        val result = jsonLogic.apply("""{"-": "2" }""", null)

        assertEquals("-2.0", result.jsonPrimitive.content)
    }

    @Test
    fun testMultiply() {
        val result = jsonLogic.apply("""{"*":[4,2]}""", null)

        assertEquals("8.0", result.jsonPrimitive.content)
    }

    @Test
    fun testMultiMultiply() {
        val result = jsonLogic.apply("""{"*":[2,2,2,2,2]}""", null)

        assertEquals("32.0", result.jsonPrimitive.content)
    }

    @Test
    fun testMultiplyWithArray() {
        val result = jsonLogic.apply("""{"*":[2,[[3, 4], 5]]}""", null)

        assertEquals("6.0", result.jsonPrimitive.content) // This matches reference impl at jsonlogic.com
    }

    @Test
    fun testMultiplyWithEmptyArray() {
        val result = jsonLogic.apply("""{"*":[2,[]]}""", null)

        assertEquals(JsonNull, result) // This matches reference impl at jsonlogic.com
    }

    @Test
    fun testDivide() {
        val result = jsonLogic.apply("""{"/":[4,2]}""", null)

        assertEquals("2.0", result.jsonPrimitive.content)
    }

    @Test
    fun testDivideBy0() {
        val result = jsonLogic.apply("""{"/":[4,0]}""", null)

        assertEquals("Infinity", result.jsonPrimitive.content)
    }

    @Test
    fun testModulo() {
        val result = jsonLogic.apply("""{"%": [101,2]}""", null)

        assertEquals("1.0", result.jsonPrimitive.content)
    }

    @Test
    fun testMin() {
        val result = jsonLogic.apply("""{"min":[1,2,3]}""", null)

        assertEquals("1.0", result.jsonPrimitive.content)
    }

    @Test
    fun testMax() {
        val result = jsonLogic.apply("""{"max":[1,2,3]}""", null)

        assertEquals("3.0", result.jsonPrimitive.content)
    }

    @Test
    fun testDivideSingleNumber() {
        assertEquals(JsonNull, jsonLogic.apply("""{"/": [0]}""", null))
    }
}
