package co.branch.jsonlogic.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pins the exact output of [canonicalDoubleToString] against `java.lang.Double.toString` as
 * specified from JDK 19 onwards. Every expectation was taken from a JDK 21 runtime.
 */
class CanonicalDoubleToStringTest {

    @Test
    fun rendersSpecialValues() {
        assertEquals("NaN", canonicalDoubleToString(Double.NaN))
        assertEquals("Infinity", canonicalDoubleToString(Double.POSITIVE_INFINITY))
        assertEquals("-Infinity", canonicalDoubleToString(Double.NEGATIVE_INFINITY))
        assertEquals("0.0", canonicalDoubleToString(0.0))
        assertEquals("-0.0", canonicalDoubleToString(-0.0))
    }

    @Test
    fun rendersPlainDecimalRange() {
        assertEquals("1.0", canonicalDoubleToString(1.0))
        assertEquals("-1.0", canonicalDoubleToString(-1.0))
        assertEquals("0.5", canonicalDoubleToString(0.5))
        assertEquals("1.5", canonicalDoubleToString(1.5))
        assertEquals("3.14", canonicalDoubleToString(3.14))
        assertEquals("-3.14", canonicalDoubleToString(-3.14))
        assertEquals("100.0", canonicalDoubleToString(100.0))
        assertEquals("12345.678", canonicalDoubleToString(12345.678))
        assertEquals("1000000.0", canonicalDoubleToString(1e6))
        assertEquals("1234567.0", canonicalDoubleToString(1234567.0))
        assertEquals("0.1", canonicalDoubleToString(0.1))
        assertEquals("0.2", canonicalDoubleToString(0.2))
        assertEquals("0.3", canonicalDoubleToString(0.3))
        assertEquals("0.7", canonicalDoubleToString(0.7))
        assertEquals("0.01", canonicalDoubleToString(1e-2))
        assertEquals("33.33", canonicalDoubleToString(33.33))
        assertEquals("0.3333333333333333", canonicalDoubleToString(1 / 3.0))
    }

    /** The plain form covers `[1e-3, 1e7)`; either side of both ends switches notation. */
    @Test
    fun rendersNotationBoundaries() {
        assertEquals("9999999.0", canonicalDoubleToString(9999999.0))
        assertEquals("9999999.999999998", canonicalDoubleToString(Double.fromBits(0x416312cfffffffffL)))
        assertEquals("1.0E7", canonicalDoubleToString(1e7))
        assertEquals("1.1E7", canonicalDoubleToString(1.1e7))
        assertEquals("1.2345678E7", canonicalDoubleToString(12345678.0))
        assertEquals("1.23456789E8", canonicalDoubleToString(123456789.0))
        assertEquals("0.001", canonicalDoubleToString(0.001))
        assertEquals("0.001", canonicalDoubleToString(1.0e-3))
        assertEquals("0.0010000000000000002", canonicalDoubleToString(Double.fromBits(0x3f50624dd2f1a9fdL)))
        assertEquals("9.999999999999998E-4", canonicalDoubleToString(Double.fromBits(0x3f50624dd2f1a9fbL)))
        assertEquals("9.999E-4", canonicalDoubleToString(9.999e-4))
        assertEquals("1.0E-4", canonicalDoubleToString(1e-4))
        assertEquals("1.23E-4", canonicalDoubleToString(1.23e-4))
        assertEquals("1.0E-5", canonicalDoubleToString(1e-5))
        assertEquals("1.0E-7", canonicalDoubleToString(1e-7))
    }

    @Test
    fun rendersExtremes() {
        assertEquals("1.7976931348623157E308", canonicalDoubleToString(Double.MAX_VALUE))
        assertEquals("1.7976931348623155E308", canonicalDoubleToString(Double.fromBits(0x7feffffffffffffeL)))
        assertEquals("4.9E-324", canonicalDoubleToString(Double.MIN_VALUE))
        assertEquals("4.9E-324", canonicalDoubleToString(4.9e-324))
        assertEquals("2.2250738585072014E-308", canonicalDoubleToString(Double.fromBits(0x0010000000000000L)))
        assertEquals("-2.2250738585072014E-308", canonicalDoubleToString(-Double.fromBits(0x0010000000000000L)))
        assertEquals("2.225073858507201E-308", canonicalDoubleToString(Double.fromBits(0x000fffffffffffffL)))
        assertEquals("1.0E100", canonicalDoubleToString(1e100))
        assertEquals("1.0E-100", canonicalDoubleToString(1e-100))
    }

    /**
     * Subnormals whose interval is wide enough for a single digit to read back: the two-digit
     * decimal that is closer to the value wins, which is why none of these end in a lone digit.
     */
    @Test
    fun rendersSmallestSubnormals() {
        assertEquals("4.9E-324", canonicalDoubleToString(Double.fromBits(1L)))
        assertEquals("9.9E-324", canonicalDoubleToString(Double.fromBits(2L)))
        assertEquals("1.5E-323", canonicalDoubleToString(Double.fromBits(3L)))
        assertEquals("1.6E-322", canonicalDoubleToString(Double.fromBits(0x20L)))
        assertEquals("6.3E-322", canonicalDoubleToString(Double.fromBits(0x80L)))
        assertEquals("-9.9E-324", canonicalDoubleToString(-Double.fromBits(2L)))
    }

    /** Values where the pre-JDK-19 algorithm emitted a longer, non-shortest decimal. */
    @Test
    fun rendersShortestDecimalWhereLegacyJavaDidNot() {
        assertEquals("1.0E23", canonicalDoubleToString(1e23))
        assertEquals("2.0E23", canonicalDoubleToString(2e23))
        assertEquals("6.02E23", canonicalDoubleToString(6.02e23))
    }

    @Test
    fun rendersPowersOfTenAroundTheLongLimit() {
        assertEquals("1.0E16", canonicalDoubleToString(1e16))
        assertEquals("1.0E17", canonicalDoubleToString(1e17))
        assertEquals("1.0E21", canonicalDoubleToString(1e21))
        assertEquals("1.0E22", canonicalDoubleToString(1e22))
        assertEquals("9.007199254740992E15", canonicalDoubleToString(9007199254740992.0))
        assertEquals("9.007199254740991E15", canonicalDoubleToString(9007199254740991.0))
        assertEquals("4.503599627370496E15", canonicalDoubleToString(4503599627370496.0))
    }

    /** Values that show up in, or fall out of arithmetic on, the JsonLogic fixtures. */
    @Test
    fun rendersFixtureValues() {
        assertEquals("3.14", canonicalDoubleToString(3.14))
        assertEquals("3.1416", canonicalDoubleToString(3.1416))
        assertEquals("1.1", canonicalDoubleToString(1.1))
        assertEquals("0.5", canonicalDoubleToString(0.5))
        assertEquals("12.0", canonicalDoubleToString(12.0))
        assertEquals("60.0", canonicalDoubleToString(60.0))
        assertEquals("0.30000000000000004", canonicalDoubleToString(0.1 + 0.2))
        assertEquals("3.3000000000000003", canonicalDoubleToString(1.1 * 3))
        assertEquals("6.28", canonicalDoubleToString(3.14 * 2))
        assertEquals("0.6666666666666666", canonicalDoubleToString(2.0 / 3.0))
        assertEquals("3.3333333333333335", canonicalDoubleToString(10.0 / 3.0))
        assertEquals("14.285714285714286", canonicalDoubleToString(100.0 / 7.0))
        assertEquals("142857.14285714287", canonicalDoubleToString(1000000.0 / 7.0))
        assertEquals("9.8596", canonicalDoubleToString(3.14 * 3.14))
        assertEquals("0.10000000000000009", canonicalDoubleToString(1.1 - 1.0))
        assertEquals("0.3666666666666667", canonicalDoubleToString(1.1 / 3))
        assertEquals("1.5", canonicalDoubleToString(0.5 * 3))
        assertEquals("1.0000000000000002", canonicalDoubleToString(Double.fromBits(0x3ff0000000000001L)))
        assertEquals("4.35", canonicalDoubleToString(4.35))
    }

    @Test
    fun exponentsCarryNoPlusSignAndNoLeadingZeros() {
        for (value in doubleArrayOf(1e7, 1e8, 1e100, 1e-4, 1e-9, 1e-100, Double.MIN_VALUE)) {
            val text = canonicalDoubleToString(value)
            val exponent = text.substring(text.indexOf('E') + 1)
            assertTrue(!exponent.startsWith("+"), "unexpected sign in $text")
            assertTrue(exponent.trimStart('-').first() != '0', "leading zero in $text")
        }
    }

    /**
     * Every rendered decimal is the shortest that reads back. Two-digit results are exempt: when a
     * single digit already reads back, Java picks the closest one- or two-digit decimal instead of
     * the shortest, and confirming that choice needs exact arithmetic the JVM oracle test provides.
     */
    @Test
    fun rendersNoMoreDigitsThanNeeded() {
        for (value in canonicalNumberCorpus(randomCount = 2_000)) {
            if (value == 0.0) continue
            val rendered = decompose(canonicalDoubleToString(value))
            val length = rendered.digits.length
            if (length < 3) continue
            val shorter = rendered.digits.substring(0, length - 1)
            val scale = rendered.pointExponent - (length - 1)
            for (candidate in listOf(shorter, (shorter.toLong() + 1).toString())) {
                val text = "${candidate}e$scale"
                val parsed = parseJavaDouble(text)
                assertNotNull(parsed, "$text was rejected")
                assertTrue(
                    parsed.toRawBits() != value.toRawBits(),
                    "$text also reads back as ${canonicalDoubleToString(value)}",
                )
            }
        }
    }

    private class Rendered(val digits: String, val pointExponent: Int)

    /** Splits a rendered double into `0.digits * 10^pointExponent`. */
    private fun decompose(text: String): Rendered {
        var body = text.removePrefix("-")
        var exponent = 0
        val e = body.indexOf('E')
        if (e >= 0) {
            exponent = body.substring(e + 1).toInt()
            body = body.substring(0, e)
        }
        val point = body.indexOf('.')
        val padded = body.substring(0, point) + body.substring(point + 1)
        val digits = padded.trimStart('0')
        return Rendered(
            digits.trimEnd('0'),
            point + exponent - (padded.length - digits.length),
        )
    }
}
