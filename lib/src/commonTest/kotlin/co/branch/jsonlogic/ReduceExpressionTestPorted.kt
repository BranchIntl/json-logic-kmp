package co.branch.jsonlogic

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Ported from upstream `ReduceExpressionTests`. Suffixed `Ported` because
 * `co.branch.jsonlogic.evaluator.ReduceExpressionTest` already covers `reduce` in more depth.
 */
class ReduceExpressionTestPorted {
    private val jsonLogic = JsonLogic()

    @Test
    fun testReduce() {
        // Upstream passes a Java int[] as the top-level data; a JSON array is the direct equivalent.
        val json = """{"reduce":[
            {"var":""},
            {"+":[{"var":"current"}, {"var":"accumulator"}]},
            0
        ]}"""
        val data = Json.parseToJsonElement("[1, 2, 3, 4, 5, 6]")

        val result = jsonLogic.apply(json, data)

        assertEquals("21", result.jsonPrimitive.content)
    }
}
