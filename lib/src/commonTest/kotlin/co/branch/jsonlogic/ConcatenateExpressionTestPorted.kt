package co.branch.jsonlogic

import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Ported from upstream `ConcatenateExpressionTests`. Suffixed `Ported` because
 * `co.branch.jsonlogic.evaluator.ConcatenateExpressionTest` already covers `cat` in more depth.
 */
class ConcatenateExpressionTestPorted {
    private val jsonLogic = JsonLogic()

    @Test
    fun testCat() {
        assertEquals(
            "hello world 2!",
            jsonLogic.apply("""{"cat": ["hello", " world ", 2, "!"]}""", null).jsonPrimitive.content,
        )
        assertEquals(
            "pi is 3.14159",
            jsonLogic.apply("""{"cat": ["pi is ", 3.14159]}""", null).jsonPrimitive.content,
        )
    }
}
