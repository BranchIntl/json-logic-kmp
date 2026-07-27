package co.branch.jsonlogic.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * The property that makes the pair of conversions usable as a value model: rendering a double and
 * reading the result back reproduces the same bits, on every target.
 */
class CanonicalNumberRoundTripTest {

    @Test
    fun everyCorpusValueSurvivesTheRoundTrip() {
        var checked = 0
        for (value in canonicalNumberCorpus()) {
            val text = canonicalDoubleToString(value)
            val parsed = parseJavaDouble(text)
            assertNotNull(parsed, "'$text' was rejected")
            assertEquals(
                value.toRawBits().toString(16),
                parsed.toRawBits().toString(16),
                "'$text' did not read back",
            )
            checked++
        }
        assertEquals(true, checked >= 10_000, "corpus shrank to $checked values")
    }

    @Test
    fun nonFiniteValuesSurviveTheRoundTrip() {
        assertEquals(Double.POSITIVE_INFINITY, parseJavaDouble(canonicalDoubleToString(Double.POSITIVE_INFINITY)))
        assertEquals(Double.NEGATIVE_INFINITY, parseJavaDouble(canonicalDoubleToString(Double.NEGATIVE_INFINITY)))
        assertEquals(true, parseJavaDouble(canonicalDoubleToString(Double.NaN))!!.isNaN())
    }
}
