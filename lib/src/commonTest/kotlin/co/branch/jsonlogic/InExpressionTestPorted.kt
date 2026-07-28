package co.branch.jsonlogic

import kotlinx.serialization.json.add
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Ported from upstream `InExpressionTests`. Suffixed `Ported` because
 * `co.branch.jsonlogic.evaluator.InExpressionTest` already covers `in` in more depth.
 */
class InExpressionTestPorted {
    private val jsonLogic = JsonLogic()

    @Test
    fun testStringIn() {
        assertEquals(true, jsonLogic.apply("""{"in": ["race", "racecar"]}""", null).jsonPrimitive.boolean)
    }

    @Test
    fun testStringNotIn() {
        assertEquals(false, jsonLogic.apply("""{"in": ["race", "clouds"]}""", null).jsonPrimitive.boolean)
        assertEquals(false, jsonLogic.apply("""{"in": [null, "clouds"]}""", null).jsonPrimitive.boolean)
    }

    @Test
    fun testArrayIn() {
        assertEquals(true, jsonLogic.apply("""{"in": [1, [1, 2, 3]]}""", null).jsonPrimitive.boolean)
        assertEquals(true, jsonLogic.apply("""{"in": [4.56, [1, 2, 3, 4.56]]}""", null).jsonPrimitive.boolean)
        assertEquals(true, jsonLogic.apply("""{"in": [null, [1, 2, 3, null]]}""", null).jsonPrimitive.boolean)
    }

    @Test
    fun testArrayNotIn() {
        assertEquals(false, jsonLogic.apply("""{"in": [5, [1, 2, 3]]}""", null).jsonPrimitive.boolean)
        assertEquals(false, jsonLogic.apply("""{"in": [null, [1, 2, 3]]}""", null).jsonPrimitive.boolean)
    }

    @Test
    fun testInVariableInt() {
        val data = buildJsonObject { put("list", buildJsonArray { add(1); add(2); add(3) }) }

        assertEquals(true, jsonLogic.apply("""{"in": [2, {"var": "list"}]}""", data).jsonPrimitive.boolean)
    }

    @Test
    fun testNotInVariableInt() {
        val data = buildJsonObject { put("list", buildJsonArray { add(1); add(2); add(3) }) }

        assertEquals(false, jsonLogic.apply("""{"in": [4, {"var": "list"}]}""", data).jsonPrimitive.boolean)
        assertEquals(false, jsonLogic.apply("""{"in": [4, {"var": "list"}]}""", null).jsonPrimitive.boolean)
    }

    @Test
    fun testAllVariables() {
        val data = buildJsonObject {
            put("list", buildJsonArray { add(1); add(2); add(3) })
            put("value", 3)
        }

        assertEquals(true, jsonLogic.apply("""{"in": [{"var": "value"}, {"var": "list"}]}""", data).jsonPrimitive.boolean)
        assertEquals(false, jsonLogic.apply("""{"in": [{"var": "value"}, {"var": "list"}]}""", null).jsonPrimitive.boolean)
    }

    @Test
    fun testSingleArgument() {
        assertFalse(jsonLogic.apply("""{"in": ["Spring"]}""", null).jsonPrimitive.boolean)
    }

    @Test
    fun testBadSecondArgument() {
        assertFalse(jsonLogic.apply("""{"in": ["Spring", 3]}""", null).jsonPrimitive.boolean)
    }
}
