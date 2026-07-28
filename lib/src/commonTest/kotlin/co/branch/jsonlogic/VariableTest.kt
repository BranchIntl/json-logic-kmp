package co.branch.jsonlogic

import co.branch.jsonlogic.fixtures.jsonSemanticEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Ported from upstream `VariableTests`. */
class VariableTest {
    private val jsonLogic = JsonLogic()

    @Test
    fun testEmptyString() {
        // Upstream's data is a raw Java Double; the direct equivalent is a top-level JSON number.
        assertEquals("3.14", jsonLogic.apply("""{"var": ""}""", JsonPrimitive(3.14)).jsonPrimitive.content)
    }

    @Test
    fun testMapAccess() {
        val data = buildJsonObject { put("pi", 3.14) }

        assertEquals("3.14", jsonLogic.apply("""{"var": "pi"}""", data).jsonPrimitive.content)
    }

    @Test
    fun testDefaultValue() {
        assertEquals("3.14", jsonLogic.apply("""{"var": ["pi", 3.14]}""", null).jsonPrimitive.content)
    }

    @Test
    fun testUndefined() {
        assertEquals(JsonNull, jsonLogic.apply("""{"var": ["pi"]}""", null))
        assertEquals(JsonNull, jsonLogic.apply("""{"var": ""}""", null))
        assertEquals(JsonNull, jsonLogic.apply("""{"var": 0}""", null))
    }

    @Test
    fun testArrayAccess() {
        // Upstream's data is a Java String[]; the direct equivalent is a top-level JSON array.
        val data = Json.parseToJsonElement("""["hello", "world"]""")

        assertEquals("hello", jsonLogic.apply("""{"var": 0}""", data).jsonPrimitive.content)
        assertEquals("world", jsonLogic.apply("""{"var": 1}""", data).jsonPrimitive.content)
        assertEquals(JsonNull, jsonLogic.apply("""{"var": 2}""", data))
        assertEquals(JsonNull, jsonLogic.apply("""{"var": 3}""", data))
    }

    @Test
    fun testArrayAccessWithStringKeys() {
        val data = Json.parseToJsonElement("""["hello", "world"]""")

        assertEquals("hello", jsonLogic.apply("""{"var": "0"}""", data).jsonPrimitive.content)
        assertEquals("world", jsonLogic.apply("""{"var": "1"}""", data).jsonPrimitive.content)
        assertEquals(JsonNull, jsonLogic.apply("""{"var": "2"}""", data))
        assertEquals(JsonNull, jsonLogic.apply("""{"var": "3"}""", data))
    }

    @Test
    fun testListAccess() {
        // Upstream repeats testArrayAccess against a java.util.List instead of an array; kotlinx's
        // JsonElement has only one sequence shape (JsonArray), so this exercises the same path.
        val data = Json.parseToJsonElement("""["hello", "world"]""")

        assertEquals("hello", jsonLogic.apply("""{"var": 0}""", data).jsonPrimitive.content)
        assertEquals("world", jsonLogic.apply("""{"var": 1}""", data).jsonPrimitive.content)
        assertEquals(JsonNull, jsonLogic.apply("""{"var": 2}""", data))
        assertEquals(JsonNull, jsonLogic.apply("""{"var": 3}""", data))
    }

    @Test
    fun testListAccessWithStringKeys() {
        // Same List-vs-array note as testListAccess.
        val data = Json.parseToJsonElement("""["hello", "world"]""")

        assertEquals("hello", jsonLogic.apply("""{"var": "0"}""", data).jsonPrimitive.content)
        assertEquals("world", jsonLogic.apply("""{"var": "1"}""", data).jsonPrimitive.content)
        assertEquals(JsonNull, jsonLogic.apply("""{"var": "2"}""", data))
        assertEquals(JsonNull, jsonLogic.apply("""{"var": "3"}""", data))
    }

    @Test
    fun testComplexAccess() {
        val data = buildJsonObject {
            put(
                "users",
                buildJsonArray {
                    add(buildJsonObject { put("name", "John"); put("followers", 1337) })
                    add(buildJsonObject { put("name", "Jane"); put("followers", 2048) })
                },
            )
        }

        assertEquals("John", jsonLogic.apply("""{"var": "users.0.name"}""", data).jsonPrimitive.content)
        assertEquals("1337.0", jsonLogic.apply("""{"var": "users.0.followers"}""", data).jsonPrimitive.content)
        assertEquals("Jane", jsonLogic.apply("""{"var": "users.1.name"}""", data).jsonPrimitive.content)
        assertEquals("2048.0", jsonLogic.apply("""{"var": "users.1.followers"}""", data).jsonPrimitive.content)
    }

    @Test
    fun missingNestedMapKey_returnsDefault() {
        // data.a.b is missing -> should use default
        val rule = """{"var": ["a.b.c", "fallback"]}"""
        val data = buildJsonObject { put("a", buildJsonObject { put("b", buildJsonObject {}) }) }

        assertEquals("fallback", jsonLogic.apply(rule, data).jsonPrimitive.content)
    }

    @Test
    fun presentNullLeaf_returnsNull_notDefault() {
        // data.user.age present with value null -> should return null (no default)
        val rule = """{"var": ["user.age", 42]}"""
        val data = buildJsonObject { put("user", buildJsonObject { put("age", JsonNull) }) }

        assertEquals(JsonNull, jsonLogic.apply(rule, data))
    }

    @Test
    fun intermediateNull_returnsNull_notDefault() {
        // data.a.b is null before finishing path -> should return null (no default)
        val rule = """{"var": ["a.b.c", "fallback"]}"""
        val data = buildJsonObject { put("a", buildJsonObject { put("b", JsonNull) }) }

        assertEquals(JsonNull, jsonLogic.apply(rule, data))
    }

    @Test
    fun nonTraversableIntermediate_returnsNull_notDefault() {
        // data.a is a number; trying to access a.b -> should return null (no default)
        val rule = """{"var": ["a.b", "fallback"]}"""
        val data = buildJsonObject { put("a", 5) }

        assertEquals(JsonNull, jsonLogic.apply(rule, data))
    }

    @Test
    fun arrayIndexWithinBounds_returnsElement_asDoubleForNumbers() {
        // items.1 exists -> should return 20 (as a double per evaluator.transform)
        val rule = """{"var": ["items.1", 999]}"""
        val data = buildJsonObject { put("items", buildJsonArray { add(10); add(20) }) }

        val result = jsonLogic.apply(rule, data)

        // Upstream checked `result instanceof Number`; the JsonElement equivalent is an unquoted
        // primitive whose content is the Java Double rendering.
        assertFalse(result.jsonPrimitive.isString)
        assertEquals("20.0", result.jsonPrimitive.content)
    }

    @Test
    fun arrayIndexOutOfBounds_returnsDefault() {
        // items.2 missing -> use default
        val rule = """{"var": ["items.2", "missing"]}"""
        val data = buildJsonObject { put("items", buildJsonArray { add(10); add(20) }) }

        assertEquals("missing", jsonLogic.apply(rule, data).jsonPrimitive.content)
    }

    @Test
    fun arrayElementPresentButNull_returnsNull_notDefault() {
        // items.0 exists and is null -> should return null (no default)
        val rule = """{"var": ["items.0", "missing"]}"""
        val data = buildJsonObject { put("items", buildJsonArray { add(JsonNull) }) }

        assertEquals(JsonNull, jsonLogic.apply(rule, data))
    }

    @Test
    fun topLevelNumericIndex_overList_works() {
        // {"var": [1, "missing"]} over a top-level list -> "banana"
        val rule = """{"var": [1, "missing"]}"""
        val data = Json.parseToJsonElement("""["apple", "banana", "carrot"]""")

        assertEquals("banana", jsonLogic.apply(rule, data).jsonPrimitive.content)
    }

    @Test
    fun emptyVarKey_returnsWholeDataObject() {
        // {"var": ""} should return the entire data object. Upstream asserted reference identity
        // (assertSame) on the returned Java object; the port's apply() always reconstructs a fresh
        // JsonElement tree on the way out, so reference identity cannot hold here - structural
        // equality is the meaningful equivalent.
        val rule = """{"var": ""}"""
        val data = buildJsonObject { put("x", 1) }

        val result = jsonLogic.apply(rule, data)

        assertTrue(jsonSemanticEquals(data, result), "expected $result to equal the original data $data")
    }
}
