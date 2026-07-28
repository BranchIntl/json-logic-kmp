package co.branch.jsonlogic

import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Ported from upstream `LogicExpressionTests`. Suffixed `Ported` because
 * `co.branch.jsonlogic.evaluator.LogicExpressionTest` already covers `and`/`or` in more depth.
 */
class LogicExpressionTestPorted {
    private val jsonLogic = JsonLogic()

    @Test
    fun testOr() {
        assertEquals("a", jsonLogic.apply("""{"or": [0, false, "a"]}""", null).jsonPrimitive.content)
    }

    @Test
    fun testAnd() {
        assertEquals("", jsonLogic.apply("""{"and": [true, "", 3]}""", null).jsonPrimitive.content)
    }
}
