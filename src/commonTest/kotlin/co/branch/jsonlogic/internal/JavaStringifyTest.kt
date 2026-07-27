package co.branch.jsonlogic.internal

import kotlin.test.Test
import kotlin.test.assertEquals

/** Pins the Java-`String.valueOf`-equivalent rendering shared by `cat`, `log`, and `substr`. */
class JavaStringifyTest {

    @Test
    fun nullRendersAsTheLiteralWordNull() {
        assertEquals("null", javaStringify(null))
    }

    @Test
    fun doublesRenderThroughCanonicalDoubleToString() {
        assertEquals("1.0", javaStringify(1.0))
        assertEquals("1.5", javaStringify(1.5))
        assertEquals("-2.0", javaStringify(-2.0))
        assertEquals("1.0E7", javaStringify(1e7))
        assertEquals("NaN", javaStringify(Double.NaN))
        assertEquals("Infinity", javaStringify(Double.POSITIVE_INFINITY))
        assertEquals("-Infinity", javaStringify(Double.NEGATIVE_INFINITY))
    }

    @Test
    fun booleansRenderAsTrueOrFalse() {
        assertEquals("true", javaStringify(true))
        assertEquals("false", javaStringify(false))
    }

    @Test
    fun stringsRenderAsThemselves() {
        assertEquals("apple", javaStringify("apple"))
        assertEquals("", javaStringify(""))
    }

    @Test
    fun listsRenderInJavaAbstractCollectionFormat() {
        assertEquals("[]", javaStringify(emptyList<Any?>()))
        assertEquals("[1.0, a, true, null]", javaStringify(listOf(1.0, "a", true, null)))
    }

    @Test
    fun nestedListsRenderRecursivelyWithNoIntegerStripping() {
        assertEquals("[1.0, [2.0, b]]", javaStringify(listOf(1.0, listOf(2.0, "b"))))
    }

    @Test
    fun mapsRenderInJavaAbstractMapFormat() {
        assertEquals("{}", javaStringify(emptyMap<String, Any?>()))
        assertEquals("{a=1.0, b=x}", javaStringify(mapOf("a" to 1.0, "b" to "x")))
    }

    @Test
    fun nestedMapsAndListsRenderRecursively() {
        assertEquals("{a=[1.0, 2.0]}", javaStringify(mapOf("a" to listOf(1.0, 2.0))))
        assertEquals("[{a=1.0}]", javaStringify(listOf(mapOf("a" to 1.0))))
    }
}
