package co.branch.jsonlogic

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Ported from upstream `TruthyTests`. Suffixed `Ported` because
 * `co.branch.jsonlogic.evaluator.TruthyTest` already covers `truthy` in more depth.
 *
 * Upstream's `testTruthyValues` also asserted `JsonLogic.truthy(new int[0])` (false) and
 * `JsonLogic.truthy(new boolean[] {false})` (true) - Java-array duck-typing was deliberately
 * dropped in this migration, so those two assertions have no KMP equivalent and are omitted here.
 * Every other assertion in the method is ported.
 */
class TruthyTestPorted {

    @Test
    fun testTruthyValues() {
        // Zero
        assertFalse(JsonLogic.truthy(0))

        // Any non-zero number
        assertTrue(JsonLogic.truthy(1.04))
        assertTrue(JsonLogic.truthy(-1))

        // Empty array or collection
        assertFalse(JsonLogic.truthy(emptyList<Any?>()))

        // Any non-empty array or collection
        assertTrue(JsonLogic.truthy(setOf(1)))

        // Empty string
        assertFalse(JsonLogic.truthy(""))

        // Any non-empty string
        assertTrue(JsonLogic.truthy("hello world"))
        assertTrue(JsonLogic.truthy("0"))

        // Null
        assertFalse(JsonLogic.truthy(null))

        // NaN and Infinity
        assertFalse(JsonLogic.truthy(Double.NaN))
        assertFalse(JsonLogic.truthy(Float.NaN))
        assertTrue(JsonLogic.truthy(Double.POSITIVE_INFINITY))
        assertTrue(JsonLogic.truthy(Double.NEGATIVE_INFINITY))
        assertTrue(JsonLogic.truthy(Float.POSITIVE_INFINITY))
        assertTrue(JsonLogic.truthy(Float.NEGATIVE_INFINITY))
    }
}
