package co.branch.jsonlogic

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Ported from upstream `MapExpressionTests`. Suffixed `Ported` because
 * `co.branch.jsonlogic.evaluator.MapExpressionTest` already covers `map` in more depth.
 */
class MapExpressionTestPorted {
    private val jsonLogic = JsonLogic()

    @Test
    fun testMap() {
        // Upstream passes a Java int[] as the top-level data; a JSON array is the direct equivalent.
        val json = """{"map": [
            {"var": ""},
            {"*": [{"var": ""}, 2]}
        ]}"""
        val data = Json.parseToJsonElement("[1, 2, 3]")

        val result = jsonLogic.apply(json, data).jsonArray

        assertEquals(3, result.size)
        assertEquals("2", result[0].jsonPrimitive.content)
        assertEquals("4", result[1].jsonPrimitive.content)
        assertEquals("6", result[2].jsonPrimitive.content)
    }
}
