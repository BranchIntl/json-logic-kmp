package co.branch.jsonlogic.evaluator

import co.branch.jsonlogic.internal.truthy
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Pins the whole truthiness table the engine's control-flow operators branch on. */
class TruthyTest {

    @Test
    fun nullIsFalsy() {
        assertFalse(truthy(null))
    }

    @Test
    fun booleansAreThemselves() {
        assertTrue(truthy(true))
        assertFalse(truthy(false))
    }

    @Test
    fun onlyZeroAndNaNAreFalsyNumbers() {
        assertFalse(truthy(0.0))
        assertFalse(truthy(-0.0))
        assertFalse(truthy(Double.NaN))
        assertTrue(truthy(1.0))
        assertTrue(truthy(-1.0))
        assertTrue(truthy(1.04))
        assertTrue(truthy(Double.POSITIVE_INFINITY))
        assertTrue(truthy(Double.NEGATIVE_INFINITY))
    }

    @Test
    fun numbersOtherThanDoubleFollowTheSameRule() {
        assertFalse(truthy(0))
        assertTrue(truthy(-1))
        assertFalse(truthy(0L))
        assertTrue(truthy(2L))
        assertFalse(truthy(0.0f))
        assertFalse(truthy(Float.NaN))
        assertTrue(truthy(Float.POSITIVE_INFINITY))
        assertTrue(truthy(Float.NEGATIVE_INFINITY))
        assertTrue(truthy(1.5f))
    }

    @Test
    fun onlyTheEmptyStringIsFalsy() {
        assertFalse(truthy(""))
        assertTrue(truthy("0"))
        assertTrue(truthy("false"))
        assertTrue(truthy(" "))
        assertTrue(truthy("hello world"))
    }

    @Test
    fun collectionsAreTruthyWhenNonEmpty() {
        assertFalse(truthy(emptyList<Any?>()))
        assertFalse(truthy(emptySet<Any?>()))
        assertTrue(truthy(listOf(1)))
        assertTrue(truthy(listOf(null)))
        assertTrue(truthy(setOf(1)))
    }

    @Test
    fun mapsAreAlwaysTruthy() {
        // A map is not a collection, so both branches above miss it and it lands on the catch-all.
        assertTrue(truthy(emptyMap<String, Any?>()))
        assertTrue(truthy(mapOf("a" to 1)))
    }
}
