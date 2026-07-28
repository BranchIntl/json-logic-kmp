package co.branch.jsonlogic.internal

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the exact output of [ecmaDoubleToString] against ECMAScript's `Number::toString`. Every
 * expectation was taken from a V8 runtime; the `wasmJsTest` oracle checks the same function against
 * V8 over the full shared corpus.
 */
class EcmaDoubleToStringTest {

    @Test
    fun rendersSpecialValues() {
        assertEquals("NaN", ecmaDoubleToString(Double.NaN))
        assertEquals("Infinity", ecmaDoubleToString(Double.POSITIVE_INFINITY))
        assertEquals("-Infinity", ecmaDoubleToString(Double.NEGATIVE_INFINITY))
        assertEquals("0", ecmaDoubleToString(0.0))
    }

    /** ECMAScript renders both zeros as `0`; Java keeps the sign. */
    @Test
    fun rendersNegativeZeroWithoutItsSign() {
        assertEquals("0", ecmaDoubleToString(-0.0))
    }

    @Test
    fun rendersWholeNumbersWithoutADecimalPoint() {
        assertEquals("0", ecmaDoubleToString(0.0))
        assertEquals("1", ecmaDoubleToString(1.0))
        assertEquals("-1", ecmaDoubleToString(-1.0))
        assertEquals("3", ecmaDoubleToString(3.0))
        assertEquals("-2", ecmaDoubleToString(-2.0))
        assertEquals("17", ecmaDoubleToString(17.0))
        assertEquals("100", ecmaDoubleToString(100.0))
        assertEquals("1000000", ecmaDoubleToString(1e6))
    }

    @Test
    fun rendersPlainDecimalRange() {
        assertEquals("0.5", ecmaDoubleToString(0.5))
        assertEquals("1.5", ecmaDoubleToString(1.5))
        assertEquals("3.14", ecmaDoubleToString(3.14))
        assertEquals("-3.14", ecmaDoubleToString(-3.14))
        assertEquals("12345.678", ecmaDoubleToString(12345.678))
        assertEquals("0.1", ecmaDoubleToString(0.1))
        assertEquals("0.01", ecmaDoubleToString(1e-2))
        assertEquals("0.001", ecmaDoubleToString(0.001))
        assertEquals("33.33", ecmaDoubleToString(33.33))
        assertEquals("0.3333333333333333", ecmaDoubleToString(1 / 3.0))
        assertEquals("0.30000000000000004", ecmaDoubleToString(0.1 + 0.2))
    }

    /**
     * The plain form covers `[1e-6, 1e21)` — four orders of magnitude wider at the top than Java's
     * `[1e-3, 1e7)`, and three wider at the bottom.
     */
    @Test
    fun rendersNotationBoundaries() {
        assertEquals("9999999", ecmaDoubleToString(9999999.0))
        assertEquals("10000000", ecmaDoubleToString(1e7))
        assertEquals("11000000", ecmaDoubleToString(1.1e7))
        assertEquals("12345678", ecmaDoubleToString(12345678.0))
        assertEquals("123456789", ecmaDoubleToString(123456789.0))
        assertEquals("100000000000000000000", ecmaDoubleToString(1e20))
        assertEquals("1e+21", ecmaDoubleToString(1e21))
        assertEquals("1e+23", ecmaDoubleToString(1e23))
        assertEquals("1.5e+21", ecmaDoubleToString(1.5e21))
        assertEquals("0.0001", ecmaDoubleToString(1e-4))
        assertEquals("0.00001", ecmaDoubleToString(1e-5))
        assertEquals("0.000001", ecmaDoubleToString(1e-6))
        assertEquals("1e-7", ecmaDoubleToString(1e-7))
        assertEquals("1.23e-7", ecmaDoubleToString(1.23e-7))
        assertEquals("0.000001234", ecmaDoubleToString(1.234e-6))
    }

    @Test
    fun rendersExtremes() {
        assertEquals("1.7976931348623157e+308", ecmaDoubleToString(Double.MAX_VALUE))
        assertEquals("2.2250738585072014e-308", ecmaDoubleToString(Double.fromBits(0x0010000000000000L)))
        assertEquals("9007199254740992", ecmaDoubleToString(9007199254740992.0))
        assertEquals("123456789012345680000", ecmaDoubleToString(123456789012345680000.0))
    }

    /**
     * The one value where the shortest decimal itself differs: Java's rule lets a two-digit decimal
     * compete when one digit already reads back, and renders `4.9E-324`.
     */
    @Test
    fun rendersTheSmallestSubnormalWithASingleDigit() {
        assertEquals("5e-324", ecmaDoubleToString(Double.MIN_VALUE))
        assertEquals("4.9E-324", canonicalDoubleToString(Double.MIN_VALUE))
    }

    /** Every rendering of a non-zero value reads back as that exact value, on every target. */
    @Test
    fun roundTripsTheSharedCorpus() {
        for (value in canonicalNumberCorpus(randomCount = 2_000)) {
            if (value == 0.0) continue // Both zeros render as "0", which reads back as positive zero.
            val rendered = ecmaDoubleToString(value)
            assertEquals(
                value.toRawBits(),
                parseJavaDouble(rendered)?.toRawBits(),
                "$rendered did not read back as bits=0x${value.toRawBits().toString(16)}",
            )
        }
    }
}
