package co.branch.jsonlogic

import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

/** Ported from upstream `CustomOperationTests`. */
class CustomOperationTest {

    @Test
    fun testCustomOp() {
        val jsonLogic = JsonLogic()
        jsonLogic.addOperation("greet") { args -> "Hello ${args[0]}!" }

        assertEquals("Hello json-logic!", jsonLogic.apply("""{"greet": ["json-logic"]}""", null).jsonPrimitive.content)
    }

    @Test
    fun testCustomOpWithUppercaseLetter() {
        val jsonLogic = JsonLogic()
        jsonLogic.addOperation("Greet") { args -> "Hello ${args[0]}!" }

        assertEquals("Hello json-logic!", jsonLogic.apply("""{"Greet": ["json-logic"]}""", null).jsonPrimitive.content)
    }
}
