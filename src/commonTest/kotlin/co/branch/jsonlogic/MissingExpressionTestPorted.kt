package co.branch.jsonlogic

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Ported from upstream `MissingExpressionTests`. Suffixed `Ported` because
 * `co.branch.jsonlogic.evaluator.MissingExpressionTest` already covers `missing`/`missing_some` in
 * more depth.
 */
class MissingExpressionTestPorted {
    private val jsonLogic = JsonLogic()

    @Test
    fun testMissing() {
        val data = buildJsonObject {
            put("a", "apple")
            put("c", "carrot")
        }
        val result = jsonLogic.apply("""{"missing": ["a", "b"]}""", data).jsonArray

        assertEquals(1, result.size)
        assertEquals("b", result[0].jsonPrimitive.content)
    }

    @Test
    fun testMissingSomeUnderThreshold() {
        val data = buildJsonObject {
            put("a", "apple")
            put("c", "carrot")
        }
        val result = jsonLogic.apply("""{"missing_some": [1, ["a", "b"]]}""", data).jsonArray

        assertEquals(0, result.size)
    }

    @Test
    fun testMissingSomeOverThreshold() {
        val data = buildJsonObject { put("a", "apple") }
        val result = jsonLogic.apply("""{"missing_some": [2, ["a", "b", "c"]]}""", data).jsonArray

        assertEquals(2, result.size)
        assertEquals("b", result[0].jsonPrimitive.content)
        assertEquals("c", result[1].jsonPrimitive.content)
    }

    @Test
    fun testMissingSomeComplexExpression() {
        val data = buildJsonObject {
            put("first_name", "Bruce")
            put("last_name", "Wayne")
        }
        val json = """{"if" :[
            {"merge": [
                {"missing":["first_name", "last_name"]},
                {"missing_some":[1, ["cell_phone", "home_phone"] ]}
            ]},
            "We require first name, last name, and one phone number.",
            "OK to proceed"
        ]}"""
        val result = jsonLogic.apply(json, data)

        assertEquals("We require first name, last name, and one phone number.", result.jsonPrimitive.content)
    }

    @Test
    fun testMissingSomeWithNullData() {
        val result = jsonLogic.apply("""{"missing_some": [2, ["a", "b", "c"]]}""", null).jsonArray

        assertEquals(3, result.size)
        assertEquals("a", result[0].jsonPrimitive.content)
        assertEquals("b", result[1].jsonPrimitive.content)
        assertEquals("c", result[2].jsonPrimitive.content)
    }

    @Test
    fun testMissingSomeWithZeroThreshold() {
        val result = jsonLogic.apply("""{"missing_some": [0, ["a", "b", "c"]]}""", null).jsonArray

        assertEquals(0, result.size)
    }
}
