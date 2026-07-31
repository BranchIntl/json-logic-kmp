package co.branch.jsonlogic.internal

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the `String(value)`-equivalent rendering `cat` and `substr` build their results from. It
 * differs from [javaStringify] only in how it renders a number, so the two agree on everything below
 * except the whole numbers.
 */
class EcmaStringifyTest {

    @Test
    fun nullRendersAsTheLiteralWordNull() {
        assertEquals("null", ecmaStringify(null))
    }

    @Test
    fun numbersRenderThroughEcmaDoubleToString() {
        assertEquals("1", ecmaStringify(1.0))
        assertEquals("1.5", ecmaStringify(1.5))
        assertEquals("-2", ecmaStringify(-2.0))
        assertEquals("0", ecmaStringify(-0.0))
        assertEquals("10000000", ecmaStringify(1e7))
        assertEquals("1e+21", ecmaStringify(1e21))
        assertEquals("NaN", ecmaStringify(Double.NaN))
        assertEquals("Infinity", ecmaStringify(Double.POSITIVE_INFINITY))
        assertEquals("-Infinity", ecmaStringify(Double.NEGATIVE_INFINITY))
    }

    @Test
    fun booleansRenderAsTrueOrFalse() {
        assertEquals("true", ecmaStringify(true))
        assertEquals("false", ecmaStringify(false))
    }

    @Test
    fun stringsRenderAsThemselves() {
        assertEquals("apple", ecmaStringify("apple"))
        assertEquals("", ecmaStringify(""))
    }

    @Test
    fun listsRenderInJavaAbstractCollectionFormat() {
        assertEquals("[]", ecmaStringify(emptyList<Any?>()))
        assertEquals("[1, a, true, null]", ecmaStringify(listOf(1.0, "a", true, null)))
        assertEquals("[1, [2, b]]", ecmaStringify(listOf(1.0, listOf(2.0, "b"))))
    }

    @Test
    fun mapsRenderInJavaAbstractMapFormat() {
        assertEquals("{}", ecmaStringify(emptyMap<String, Any?>()))
        assertEquals("{a=1, b=x}", ecmaStringify(mapOf("a" to 1.0, "b" to "x")))
        assertEquals("{a=[1, 2]}", ecmaStringify(mapOf("a" to listOf(1.0, 2.0))))
    }

    /** A container reached from inside itself renders as java.util names it instead of being entered. */
    @Test
    fun aContainerThatHoldsItselfRendersTheRepeatWithoutEnteringIt() {
        val map = mutableMapOf<String, Any?>("a" to 1.0)
        map["self"] = map

        val list = mutableListOf<Any?>(1.0)
        list.add(list)

        assertEquals("{a=1, self=(this Map)}", ecmaStringify(map))
        assertEquals("[1, (this Collection)]", ecmaStringify(list))
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

        assertEquals("[{current=2, accumulator=(this Collection)}]", ecmaStringify(list))
        assertEquals("{current=2, accumulator=[(this Map)]}", ecmaStringify(map))
    }

    /** Only a container enclosing itself is a cycle; the same one twice in a row renders twice. */
    @Test
    fun theSameContainerInTwoPlacesIsNotACycle() {
        val map = mapOf("a" to 1.0)
        val list = listOf(1.0)

        assertEquals("[{a=1}, {a=1}]", ecmaStringify(listOf(map, map)))
        assertEquals("{x=[1], y=[1]}", ecmaStringify(mapOf("x" to list, "y" to list)))
    }
}
