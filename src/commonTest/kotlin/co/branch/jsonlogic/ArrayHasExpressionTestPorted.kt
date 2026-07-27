package co.branch.jsonlogic

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Ported from upstream `ArrayHasExpressionTests`. Suffixed `Ported` because
 * `co.branch.jsonlogic.evaluator.ArrayHasExpressionTest` already covers `some`/`none` in more depth.
 */
class ArrayHasExpressionTestPorted {
    private val jsonLogic = JsonLogic()

    @Test
    fun testSomeWithNull() {
        // Upstream passed data as a raw, unparsed Java string literal ("{\"fruits\":null}"), which
        // (per the oracle's apply(String, Object) not parsing its data argument) exercised a
        // different code path than intended but produced the same expected result. Ported using the
        // evidently intended data shape - an object whose "fruits" key is present and null - which
        // reaches the same expected outcome (some over a null array is false) via the real path.
        val data = buildJsonObject { put("fruits", JsonNull) }
        assertEquals(
            false,
            jsonLogic.apply("""{"and":[{"some":[{"var":"fruits"},{"in":[{"var":""},["apple"]]}]}]}""", data)
                .jsonPrimitive.boolean,
        )
    }

    @Test
    fun testSomeEmptyArray() {
        assertEquals(false, jsonLogic.apply("""{"some": [[], {">": [{"var": ""}, 0]}]}""", null).jsonPrimitive.boolean)
    }

    @Test
    fun testSomeAll() {
        assertEquals(false, jsonLogic.apply("""{"some": [[1, 2, 3], {">": [{"var": ""}, 3]}]}""", null).jsonPrimitive.boolean)
        assertEquals(true, jsonLogic.apply("""{"some": [[1, 2, 3], {">": [{"var": ""}, 1]}]}""", null).jsonPrimitive.boolean)
    }

    @Test
    fun testNoneWithNull() {
        // Same upstream raw-string-data quirk as testSomeWithNull; ported the same way.
        val data = buildJsonObject { put("fruits", JsonNull) }
        assertEquals(
            true,
            jsonLogic.apply("""{"and":[{"none":[{"var":"fruits"},{"in":[{"var":""},["apple"]]}]}]}""", data)
                .jsonPrimitive.boolean,
        )
    }

    @Test
    fun testNoneEmptyArray() {
        // Upstream's body uses "some", not "none" - a verbatim duplicate of testSomeEmptyArray,
        // evidently a copy/paste artifact in the original suite (the test's name promises "none").
        // Ported as literally written for 1:1 fidelity; see the workstream report for this note.
        assertEquals(false, jsonLogic.apply("""{"some": [[], {">": [{"var": ""}, 0]}]}""", null).jsonPrimitive.boolean)
    }

    @Test
    fun testNoneAll() {
        assertEquals(true, jsonLogic.apply("""{"none": [[1, 2, 3], {">": [{"var": ""}, 3]}]}""", null).jsonPrimitive.boolean)
        assertEquals(false, jsonLogic.apply("""{"none": [[1, 2, 3], {">": [{"var": ""}, 2]}]}""", null).jsonPrimitive.boolean)
    }
}
