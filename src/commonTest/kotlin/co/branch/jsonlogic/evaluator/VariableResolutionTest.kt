package co.branch.jsonlogic.evaluator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Pins `var` resolution: dotted paths, numeric indexing, the default-value fallback, and above all
 * the distinction between a key that is absent (falls back to the default) and a key that is present
 * with a null value (resolves to null).
 */
class VariableResolutionTest {

    @Test
    fun emptyKeyReturnsTheDataItself() {
        val data = mapOf("x" to 1)

        assertSame(data, evaluate("""{"var": ""}""", data))
        assertEquals(3.14, evaluate("""{"var": ""}""", 3.14))
    }

    @Test
    fun nullKeyReturnsTheDataNormalizedToDouble() {
        assertEquals(5.0, evaluate("""{"var": null}""", 5))
        assertEquals(5.0, evaluate("""{"var": []}""", 5))
    }

    @Test
    fun mapKeyResolvesToItsValue() {
        assertEquals(3.14, evaluate("""{"var": "pi"}""", mapOf("pi" to 3.14)))
    }

    @Test
    fun nullDataResolvesToTheDefaultValue() {
        assertEquals(3.14, evaluate("""{"var": ["pi", 3.14]}""", null))
        assertNull(evaluate("""{"var": ["pi"]}""", null))
        assertNull(evaluate("""{"var": ""}""", null))
        assertNull(evaluate("""{"var": 0}""", null))
    }

    @Test
    fun defaultValueIsEvaluatedWithoutTheData() {
        // The default expression is evaluated against null data, so its own var cannot see "a".
        assertNull(evaluate("""{"var": ["b", {"var": "a"}]}""", mapOf("a" to 1)))
    }

    @Test
    fun numericKeyIndexesListData() {
        val data = listOf("hello", "world")

        assertEquals("hello", evaluate("""{"var": 0}""", data))
        assertEquals("world", evaluate("""{"var": 1}""", data))
        assertNull(evaluate("""{"var": 2}""", data))
        assertEquals("fallback", evaluate("""{"var": [2, "fallback"]}""", data))
        assertEquals("fallback", evaluate("""{"var": [-1, "fallback"]}""", data))
    }

    @Test
    fun numericKeyTruncatesTowardsZero() {
        assertEquals("world", evaluate("""{"var": 1.9}""", listOf("hello", "world")))
    }

    @Test
    fun numericKeyNeverReachesIntoAMap() {
        // Map data is not list-like, so a numeric key falls straight through to the default.
        assertEquals("fallback", evaluate("""{"var": [0, "fallback"]}""", mapOf("0" to "zero")))
    }

    @Test
    fun stringKeyIndexesListData() {
        val data = listOf("hello", "world")

        assertEquals("hello", evaluate("""{"var": "0"}""", data))
        assertEquals("world", evaluate("""{"var": "1"}""", data))
        assertNull(evaluate("""{"var": "2"}""", data))
        assertEquals("fallback", evaluate("""{"var": ["2", "fallback"]}""", data))
    }

    @Test
    fun dottedPathWalksMapsAndLists() {
        val data = mapOf(
            "users" to listOf(
                mapOf("name" to "John", "followers" to 1337),
                mapOf("name" to "Jane", "followers" to 2048),
            ),
        )

        assertEquals("John", evaluate("""{"var": "users.0.name"}""", data))
        assertEquals(1337.0, evaluate("""{"var": "users.0.followers"}""", data))
        assertEquals("Jane", evaluate("""{"var": "users.1.name"}""", data))
        assertEquals(2048.0, evaluate("""{"var": "users.1.followers"}""", data))
    }

    @Test
    fun absentKeyFallsBackToTheDefault() {
        assertEquals("fallback", evaluate("""{"var": ["a", "fallback"]}""", emptyMap<String, Any?>()))
        assertEquals("fallback", evaluate("""{"var": ["a.b.c", "fallback"]}""", mapOf("a" to mapOf("b" to emptyMap<String, Any?>()))))
        assertEquals("fallback", evaluate("""{"var": ["items.2", "fallback"]}""", mapOf("items" to listOf(10, 20))))
    }

    @Test
    fun presentNullValueResolvesToNullRatherThanTheDefault() {
        assertNull(evaluate("""{"var": ["a", "fallback"]}""", mapOf("a" to null)))
        assertNull(evaluate("""{"var": ["user.age", 42]}""", mapOf("user" to mapOf("age" to null))))
        assertNull(evaluate("""{"var": ["items.0", "fallback"]}""", mapOf("items" to listOf(null))))
    }

    @Test
    fun nullPartWayAlongThePathResolvesToNullRatherThanTheDefault() {
        assertNull(evaluate("""{"var": ["a.b.c", "fallback"]}""", mapOf("a" to mapOf("b" to null))))
    }

    @Test
    fun pathThroughANonTraversableValueResolvesToNull() {
        assertNull(evaluate("""{"var": ["a.b", "fallback"]}""", mapOf("a" to 5)))
    }

    @Test
    fun listElementIsNormalizedToDouble() {
        assertEquals(20.0, evaluate("""{"var": ["items.1", 999]}""", mapOf("items" to listOf(10, 20))))
    }

    @Test
    fun trailingPathSeparatorsAreDropped() {
        // Java's split("\\.") discards trailing empty parts, so "a." asks for "a" and "." asks for
        // nothing at all, leaving the data itself.
        val data = mapOf("a" to 1)

        assertEquals(1.0, evaluate("""{"var": "a."}""", data))
        assertEquals(1.0, evaluate("""{"var": "a.."}""", data))
        assertSame(data, evaluate("""{"var": "."}""", data))
    }

    @Test
    fun leadingPathSeparatorAsksForTheEmptyKey() {
        assertEquals("fallback", evaluate("""{"var": [".a", "fallback"]}""", mapOf("a" to 1)))
        assertEquals(1.0, evaluate("""{"var": ".a"}""", mapOf("" to mapOf("a" to 1))))
    }

    @Test
    fun nonNumericSegmentIntoListDataFailsAsJavaNumberFormatException() {
        val exception = assertFailsWith<JsonLogicEvaluationException> {
            evaluate("""{"var": "key.foo"}""", mapOf("key" to listOf(1, 2)))
        }

        assertEquals("""java.lang.NumberFormatException: For input string: "foo"""", exception.message)
        assertEquals("$.var[0]", exception.jsonPath)
    }

    @Test
    fun keyThatIsNeitherNumberNorStringFails() {
        val exception = assertFailsWith<JsonLogicEvaluationException> {
            evaluate("""{"var": [[1, 2]]}""", emptyMap<String, Any?>())
        }

        assertEquals("var first argument must be null, number, or string", exception.message)
        assertEquals("$.var[0]", exception.jsonPath)

        assertFailsWith<JsonLogicEvaluationException> {
            evaluate("""{"var": true}""", emptyMap<String, Any?>())
        }
    }
}
