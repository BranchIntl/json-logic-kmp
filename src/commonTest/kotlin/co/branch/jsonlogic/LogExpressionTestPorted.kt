package co.branch.jsonlogic

import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Ported from upstream `LogExpressionTests`. Suffixed `Ported` because
 * `co.branch.jsonlogic.evaluator.LogExpressionTest` already covers `log` in more depth.
 */
class LogExpressionTestPorted {
    private val jsonLogic = JsonLogic()

    @Test
    fun testDoesLog() {
        assertEquals("hello world", jsonLogic.apply("""{"log": "hello world"}""", null).jsonPrimitive.content)
    }
}
