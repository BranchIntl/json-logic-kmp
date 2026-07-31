package co.branch.jsonlogic.internal

import kotlin.test.Test
import kotlin.test.assertEquals

/** Pins the Java-`String.valueOf`-equivalent rendering `log` writes its diagnostic text with. */
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

    /**
     * A collection a custom operation returned renders its numbers here too, rather than falling
     * through to a `toString` whose form varies by target.
     */
    @Test
    fun aCollectionThatIsNotAListRendersTheSameWay() {
        assertEquals("[1.0, 2.0]", javaStringify(linkedSetOf(1.0, 2.0)))
        assertEquals("[[1.0], 2.0]", javaStringify(linkedSetOf(listOf(1.0), 2.0)))
    }

    /** A container reached from inside itself renders as java.util names it instead of being entered. */
    @Test
    fun aContainerThatHoldsItselfRendersTheRepeatWithoutEnteringIt() {
        val map = mutableMapOf<String, Any?>("a" to 1.0)
        map["self"] = map

        val list = mutableListOf<Any?>(1.0)
        list.add(list)

        assertEquals("{a=1.0, self=(this Map)}", javaStringify(map))
        assertEquals("[1.0, (this Collection)]", javaStringify(list))
    }

    /**
     * A cycle closing through more than one container stops too, which java.util's own one-level check
     * does not manage: it compares an entry's value against the map holding it, and here they differ.
     */
    @Test
    fun aCycleThroughMoreThanOneContainerAlsoStops() {
        val map = mutableMapOf<String, Any?>("current" to 2.0)
        val list = mutableListOf<Any?>(map)
        map["accumulator"] = list

        assertEquals("[{current=2.0, accumulator=(this Collection)}]", javaStringify(list))
        assertEquals("{current=2.0, accumulator=[(this Map)]}", javaStringify(map))
    }

    /** Only a container enclosing itself is a cycle; the same one twice in a row renders twice. */
    @Test
    fun theSameContainerInTwoPlacesIsNotACycle() {
        val map = mapOf("a" to 1.0)
        val list = listOf(1.0)

        assertEquals("[{a=1.0}, {a=1.0}]", javaStringify(listOf(map, map)))
        assertEquals("{x=[1.0], y=[1.0]}", javaStringify(mapOf("x" to list, "y" to list)))
    }
}
