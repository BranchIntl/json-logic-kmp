package co.branch.jsonlogic.evaluator

import kotlin.test.Test
import kotlin.test.assertEquals

/** Pins `in`: the substring branch, the membership branch, and the equality the latter compares with. */
class InExpressionTest {

    private val numbers = mapOf(
        "nan" to Double.NaN,
        "nans" to listOf(Double.NaN),
        "zero" to 0.0,
        "zeros" to listOf(0.0),
        "negativeZero" to -0.0,
        "negativeZeros" to listOf(-0.0),
    )

    @Test
    fun aStringSecondArgumentIsASubstringTest() {
        assertEquals(true, evaluateArrayOp("""{"in": ["Spring", "Springfield"]}"""))
        assertEquals(false, evaluateArrayOp("""{"in": ["i", "team"]}"""))
        assertEquals(true, evaluateArrayOp("""{"in": ["", "abc"]}"""))
        assertEquals(false, evaluateArrayOp("""{"in": ["a", ""]}"""))
        assertEquals(false, evaluateArrayOp("""{"in": [null, "null"]}"""))
    }

    @Test
    fun aNonStringIsRenderedTheWayJavaWouldRenderIt() {
        assertEquals(true, evaluateArrayOp("""{"in": [1, "a1.0b"]}"""))
        assertEquals(false, evaluateArrayOp("""{"in": [1, "a1b"]}"""))
        assertEquals(true, evaluateArrayOp("""{"in": [true, "is true here"]}"""))
        assertEquals(true, evaluateArrayOp("""{"in": [[1, 2], "x[1.0, 2.0]y"]}"""))
        assertEquals(true, evaluateArrayOp("""{"in": [{"merge": []}, "x[]y"]}"""))
    }

    @Test
    fun renderedNumbersFollowJavasDoubleToString() {
        val data = mapOf(
            "big" to 1e8,
            "small" to 0.001,
            "smaller" to 0.0001,
            "nan" to Double.NaN,
            "infinity" to Double.POSITIVE_INFINITY,
            "negativeZero" to -0.0,
        )

        assertEquals(true, evaluateArrayOp("""{"in": [{"var": "big"}, "x1.0E8y"]}""", data))
        assertEquals(true, evaluateArrayOp("""{"in": [{"var": "small"}, "x0.001y"]}""", data))
        assertEquals(true, evaluateArrayOp("""{"in": [{"var": "smaller"}, "x1.0E-4y"]}""", data))
        assertEquals(true, evaluateArrayOp("""{"in": [{"var": "nan"}, "xNaNy"]}""", data))
        assertEquals(true, evaluateArrayOp("""{"in": [{"var": "infinity"}, "xInfinityy"]}""", data))
        assertEquals(true, evaluateArrayOp("""{"in": [{"var": "negativeZero"}, "x-0.0y"]}""", data))
    }

    @Test
    fun renderedCollectionsAndMapsFollowJavasToString() {
        val data = mapOf(
            "nested" to listOf(listOf(1.0), "x"),
            "map" to mapOf("a" to 1.0, "b" to listOf(2.0)),
        )

        assertEquals(true, evaluateArrayOp("""{"in": [{"var": "nested"}, "x[[1.0], x]y"]}""", data))
        assertEquals(true, evaluateArrayOp("""{"in": [{"var": "map"}, "x{a=1.0, b=[2.0]}y"]}""", data))
    }

    @Test
    fun aMapThatHoldsItselfIsRenderedWithoutRecurring() {
        assertEquals(false, evaluateArrayOp("""{"in": [{"reduce": [[1, 2], {"var": ""}, 0]}, "nope"]}"""))
    }

    @Test
    fun aListLikeSecondArgumentIsAMembershipTest() {
        assertEquals(true, evaluateArrayOp("""{"in": ["a", ["a", "b"]]}"""))
        assertEquals(false, evaluateArrayOp("""{"in": ["c", ["a", "b"]]}"""))
        assertEquals(true, evaluateArrayOp("""{"in": [1, [1, 2]]}"""))
        assertEquals(true, evaluateArrayOp("""{"in": [true, [true]]}"""))
        assertEquals(true, evaluateArrayOp("""{"in": [null, [null]]}"""))
        assertEquals(false, evaluateArrayOp("""{"in": [null, [1]]}"""))
    }

    @Test
    fun membershipDoesNotCoerceAcrossTypes() {
        assertEquals(false, evaluateArrayOp("""{"in": [1, ["1.0"]]}"""))
        assertEquals(false, evaluateArrayOp("""{"in": ["1.0", [1]]}"""))
        assertEquals(false, evaluateArrayOp("""{"in": [true, [1]]}"""))
        assertEquals(false, evaluateArrayOp("""{"in": [1, [true]]}"""))
    }

    @Test
    fun membershipComparesByValueToAnyDepth() {
        assertEquals(true, evaluateArrayOp("""{"in": [[1, 2], [[1, 2], [3]]]}"""))
        assertEquals(false, evaluateArrayOp("""{"in": [[1, 2], [[1, 3], [3]]]}"""))
        assertEquals(true, evaluateArrayOp("""{"in": [[[1]], [[[1]], [2]]]}"""))
        assertEquals(
            true,
            evaluateArrayOp(
                """{"in": [{"var": "m"}, {"var": "ms"}]}""",
                mapOf("m" to mapOf("a" to 1.0), "ms" to listOf(mapOf("a" to 1.0))),
            ),
        )
    }

    @Test
    fun aListAnotherOperationProducedIsMatchedLikeAnyOtherList() {
        // A deliberate divergence from the engine this library ports: there, a list that reaches this
        // membership test still wrapped in the engine's own list adapter never matches anything, since
        // that adapter's equals always returns false, and `missing` returns exactly such a wrapper.
        // This port has no wrapper type — every list in the value domain is a plain list — so the
        // comparison here is by value whatever produced the list.
        assertEquals(true, evaluateArrayOp("""{"in": [{"missing": [["a"]]}, [["a"]]]}"""))
        assertEquals(true, evaluateArrayOp("""{"in": [{"merge": [["a"]]}, [["a"]]]}"""))
    }

    @Test
    fun membershipComparesNumbersBitwise() {
        assertEquals(true, evaluateArrayOp("""{"in": [{"var": "nan"}, {"var": "nans"}]}""", numbers))
        assertEquals(false, evaluateArrayOp("""{"in": [{"var": "negativeZero"}, {"var": "zeros"}]}""", numbers))
        assertEquals(false, evaluateArrayOp("""{"in": [{"var": "zero"}, {"var": "negativeZeros"}]}""", numbers))
        assertEquals(true, evaluateArrayOp("""{"in": [{"var": "zero"}, {"var": "zeros"}]}""", numbers))
        assertEquals(true, evaluateArrayOp("""{"in": [{"var": "negativeZero"}, {"var": "negativeZeros"}]}""", numbers))
    }

    @Test
    fun aSecondArgumentThatIsNeitherStringNorListLikeIsFalse() {
        assertEquals(false, evaluateArrayOp("""{"in": [1, null]}"""))
        assertEquals(false, evaluateArrayOp("""{"in": [1, 2]}"""))
        assertEquals(false, evaluateArrayOp("""{"in": [1, true]}"""))
        assertEquals(false, evaluateArrayOp("""{"in": ["a", {"var": "m"}]}""", mapOf("m" to mapOf("a" to 1.0))))
    }

    @Test
    fun fewerThanTwoArgumentsIsFalse() {
        assertEquals(false, evaluateArrayOp("""{"in": []}"""))
        assertEquals(false, evaluateArrayOp("""{"in": [1]}"""))
    }

    @Test
    fun aLoneListArgumentIsUnwrappedIntoTheTwoOperands() {
        assertEquals(true, evaluateArrayOp("""{"in": [["Spring", "Springfield"]]}"""))
        assertEquals(false, evaluateArrayOp("""{"in": [["a"]]}"""))
    }
}
