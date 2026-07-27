package co.branch.jsonlogic

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
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
        // Upstream passed data as a raw, unparsed Java string literal ("{\"fruits\":null}") - the
        // oracle's apply(String, Object) never parses its data argument, so {"var":"fruits"} resolves
        // against a scalar string (neither list-like nor map-like) and falls through to null via that
        // path, not via a present-and-null map entry. The exact scenario is directly representable
        // here as a JsonPrimitive string, so it is ported as-is rather than adapted.
        val data = JsonPrimitive("""{"fruits":null}""")
        assertEquals(
            false,
            jsonLogic.apply("""{"and":[{"some":[{"var":"fruits"},{"in":[{"var":""},["apple"]]}]}]}""", data)
                .jsonPrimitive.boolean,
        )
    }

    @Test
    fun testSomeWithPresentNullProperty() {
        // A second, genuinely distinct scenario worth pinning alongside testSomeWithNull: here
        // "fruits" is an actual object key present with a null value, so {"var":"fruits"} resolves to
        // null through the map-lookup path instead of the scalar-data fallthrough above. Both reach
        // the same "some over null is false" outcome, but via different code paths.
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
        // Same upstream raw-string-data quirk as testSomeWithNull; ported the same way (see that
        // test's comment) rather than adapted to a parsed object.
        val data = JsonPrimitive("""{"fruits":null}""")
        assertEquals(
            true,
            jsonLogic.apply("""{"and":[{"none":[{"var":"fruits"},{"in":[{"var":""},["apple"]]}]}]}""", data)
                .jsonPrimitive.boolean,
        )
    }

    @Test
    fun testNoneWithPresentNullProperty() {
        // Distinct-scenario counterpart to testSomeWithPresentNullProperty; see that test's comment.
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
