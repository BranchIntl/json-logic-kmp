package co.branch.jsonlogic

import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Ported from upstream `IfExpressionTests`. Suffixed `Ported` because
 * `co.branch.jsonlogic.evaluator.IfExpressionTest` already covers `if` in more depth.
 */
class IfExpressionTestPorted {
    private val jsonLogic = JsonLogic()

    @Test
    fun testIfTrue() {
        val result = jsonLogic.apply("""{"if" : [true, "yes", "no"]}""", null)

        assertEquals("yes", result.jsonPrimitive.content)
    }

    @Test
    fun testIfFalse() {
        val result = jsonLogic.apply("""{"if" : [false, "yes", "no"]}""", null)

        assertEquals("no", result.jsonPrimitive.content)
    }

    @Test
    fun testIfElseIfElse() {
        val json = """{"if" : [
            {"<": [50, 0]}, "freezing",
            {"<": [50, 100]}, "liquid",
            "gas"
        ]}"""
        val result = jsonLogic.apply(json, null)

        assertEquals("liquid", result.jsonPrimitive.content)
    }
}
