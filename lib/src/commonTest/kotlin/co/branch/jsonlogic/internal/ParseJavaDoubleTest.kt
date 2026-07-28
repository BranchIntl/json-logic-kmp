package co.branch.jsonlogic.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins [parseJavaDouble] against `java.lang.Double.parseDouble`: every accepted input is checked
 * bit for bit, and every rejected input is one that makes `parseDouble` throw. Expectations come
 * from a JDK 17 runtime; unlike `Double.toString`, the parse direction has never changed.
 */
class ParseJavaDoubleTest {

    @Test
    fun rejectsMalformedInput() {
        val rejected = listOf(
            "", " ", "\t", "\n", " \t\r\n ", "abc", "1,0", "0x", "0X", "1.2.3", "Inf", "Infinit",
            "+-1", "1e", "--1", "-+1", ".", "-.", "+.", "-", "+", "1e+", "1e-", "0e", "1.0e",
            "1e5x", "1fd", "1df", "1 0", "1_000", "1e1e1", "nan", "NAN", "NaNf", "NaNN",
            "infinity", "INFINITY", "Infinityf", "0.0x", "0garbage", "0x1", "0x1.8", "0x1.8f",
            "0x.p1", "0x1p", "0x1p+", "0x1p1.5", "0x1p0x1", "0xgp0", "0x1.8p1x", "0x 1p1",
            "0x1 p1", ".e1",
            // Trimming stops at ' ', so a non-breaking space is not whitespace.
            "\u00a01", "1\u00a0",
        )
        for (input in rejected) assertNull(parseJavaDouble(input), "expected rejection of '$input'")
    }

    @Test
    fun acceptsTheDocumentedGrammar() {
        assertBits(0x3ff0000000000000L, "1")
        assertBits(0x3ff0000000000000L, " 1.0 ")
        assertBits(0x3ff0000000000000L, "\t1.0\n")
        assertBits(0x3ff0000000000000L, "\u00001\u001f")
        assertBits(0x3ff0000000000000L, "+1")
        assertBits(-0x4010000000000000L, "-1")
        assertBits(0x3ff0000000000000L, "1.")
        assertBits(0x3ff0000000000000L, "1f")
        assertBits(0x3ff0000000000000L, "1F")
        assertBits(0x3ff0000000000000L, "1d")
        assertBits(0x3ff0000000000000L, "1D")
        assertBits(0x3ff0000000000000L, "1.0d")
        assertBits(0x3fb999999999999aL, ".1")
        assertBits(0x3fe0000000000000L, ".5")
        assertBits(0x4014000000000000L, "5.")
        assertBits(0x4014000000000000L, "5.f")
        assertBits(0x3fe0000000000000L, "00.5")
        assertBits(0x408f400000000000L, "1e3")
        assertBits(0x408f400000000000L, "1e03")
        assertBits(0x408f400000000000L, "1e+3")
        assertBits(0x408f400000000000L, "1e0000000000000000000003")
        assertBits(0x3f50624dd2f1a9fcL, "1E-3")
        assertBits(0x40f86a0000000000L, "1e5f")
        assertBits(0x3ff8000000000000L, "1.5e-0")
    }

    @Test
    fun acceptsSignedZeroAndZeroSpellings() {
        assertBits(0L, "0")
        assertBits(0L, "00")
        assertBits(0L, "0.")
        assertBits(0L, ".0")
        assertBits(0L, "000")
        assertBits(0L, "0.000")
        assertBits(0L, "0.e1")
        assertBits(0L, "0e999999999")
        assertBits(0L, "0e-999999999")
        assertBits(NEGATIVE_ZERO_BITS, "-0")
        assertBits(NEGATIVE_ZERO_BITS, "-0.0")
        assertBits(NEGATIVE_ZERO_BITS, "-0.000")
        assertBits(NEGATIVE_ZERO_BITS, "-0e999999999")
        assertBits(NEGATIVE_ZERO_BITS, "-1e-400")
        assertBits(NEGATIVE_ZERO_BITS, "-0x0p0")
    }

    @Test
    fun acceptsNonFiniteSpellings() {
        assertTrue(parseJavaDouble("NaN")!!.isNaN())
        assertTrue(parseJavaDouble("+NaN")!!.isNaN())
        assertTrue(parseJavaDouble("-NaN")!!.isNaN())
        assertTrue(parseJavaDouble("NaN ")!!.isNaN())
        assertEquals(Double.POSITIVE_INFINITY, parseJavaDouble("Infinity"))
        assertEquals(Double.POSITIVE_INFINITY, parseJavaDouble("+Infinity"))
        assertEquals(Double.NEGATIVE_INFINITY, parseJavaDouble("-Infinity"))
    }

    @Test
    fun acceptsHexadecimalFloatingPoint() {
        assertBits(0x4000000000000000L, "0x1p1")
        assertBits(0x4000000000000000L, "0X1P1")
        assertBits(0x4000000000000000L, "0x1.p1")
        assertBits(0x4000000000000000L, "0x1p+1")
        assertBits(0x4000000000000000L, "0x1p1f")
        assertBits(0x4000000000000000L, "0x1p1d")
        assertBits(0x4000000000000000L, " 0x1p1 ")
        assertBits(-0x4000000000000000L, "-0x1p1")
        assertBits(0x4008000000000000L, "0x1.8p1")
        assertBits(0x3ff0000000000000L, "0x.8p1")
        assertBits(0x3fe0000000000000L, "0x1p-1")
        assertBits(0x3ff0000000000000L, "0x10p-4")
        assertBits(0x3ff0000000000000L, "0x00000001p0")
        assertBits(0x4024000000000000L, "0xAp0")
        assertBits(0x4024000000000000L, "0xap0")
        assertBits(0x402e000000000000L, "0xfp0")
        assertBits(0L, "0x0p0")
        assertBits(0L, "0x000.000p0")
        assertBits(0L, "0x0p-99999999999")
        assertBits(0x4170000000000000L, "0x.0000000000000000001p100")
        assertBits(0x7e70000000000000L, "0x1p1000")
        assertBits(0L, "0x1p-1100")
        assertBits(1L, "0x1p-1074")
        assertBits(0L, "0x1p-1075")
        assertBits(2L, "0x1.8p-1074")
        assertBits(1L, "0x0.0000000000001p-1022")
        // A 61-bit significand, rounded to 53 bits half-even.
        assertBits(0x43b234567890abceL, "0x1234567890abcdefp0")
        assertBits(0x7fefffffffffffffL, "0x1.fffffffffffffp1023")
        assertEquals(Double.POSITIVE_INFINITY, parseJavaDouble("0x1.fffffffffffff8p1023"))
        assertEquals(Double.POSITIVE_INFINITY, parseJavaDouble("0x1p1024"))
        assertEquals(Double.POSITIVE_INFINITY, parseJavaDouble("0x1p99999999999999999999"))
    }

    @Test
    fun saturatesAtTheEndsOfTheRange() {
        assertEquals(Double.POSITIVE_INFINITY, parseJavaDouble("1e309"))
        assertEquals(Double.POSITIVE_INFINITY, parseJavaDouble("1e310"))
        assertEquals(Double.POSITIVE_INFINITY, parseJavaDouble("1e2147483648"))
        assertEquals(Double.POSITIVE_INFINITY, parseJavaDouble("1e99999999999999999999"))
        assertEquals(Double.NEGATIVE_INFINITY, parseJavaDouble("-1e400"))
        assertBits(0x7fe1ccf385ebc8a0L, "1e308")
        assertBits(0x7fefffffffffffffL, "1.7976931348623157e308")
        assertBits(0x7fefffffffffffffL, "1.7976931348623158e308")
        assertEquals(Double.POSITIVE_INFINITY, parseJavaDouble("1.797693134862315808e308"))
        assertBits(0L, "1e-324")
        assertBits(0L, "1e-325")
        assertBits(0L, "1e-400")
        assertBits(0L, "1e-2147483649")
        assertBits(1L, "4.9e-324")
        assertBits(2L, "1e-323")
        assertBits(2L, "9.88131291682493088353e-324")
        assertBits(0L, "2.4703282292062327e-324")
        assertBits(1L, "2.4703282292062328e-324")
        assertBits(0xfffffffffffffL, "2.2250738585072011e-308")
    }

    /** Long inputs must be rounded half-even on the exact value, not on a truncation of it. */
    @Test
    fun roundsLongInputsCorrectly() {
        assertBits(0x3fb999999999999aL, "0.1000000000000000055511151231257827021181583404541015625")
        assertBits(
            0x3fb999999999999aL,
            "0.10000000000000000555111512312578270211815834045410156250000000001",
        )
        assertBits(
            0x3fb999999999999aL,
            "0.100000000000000005551115123125782702118158340454101562499999999",
        )
        // Exactly halfway below 0.1, then a hair above and a hair below that midpoint.
        assertBits(0x3fb999999999999aL, "0.099999999999999998612221219218554324470460414886474609375")
        assertBits(0x3fb999999999999aL, "0.0999999999999999986122212192185543244704604148864746093751")
        assertBits(
            0x3fb9999999999999L,
            "0.09999999999999999861222121921855432447046041488647460937499999999999999999",
        )
        assertBits(0x3ff0000000000001L, "1.0000000000000002220446049250313080847263336181640625")
        // Exactly halfway between 1.0 and its successor: half-even keeps the even significand.
        assertBits(0x3ff0000000000000L, "1.0000000000000001110223024625156540423631668090820312500")
        assertBits(
            0x3ff0000000000001L,
            "1.00000000000000011102230246251565404236316680908203125000000001",
        )
        assertBits(0x45f8ee90ff6c373eL, "123456789012345678901234567890")
        assertBits(0x3fb999999999999aL, "1000000000000000000000000000000000000000e-40")
        assertBits(0x3ff0000000000000L, "1" + "0".repeat(400) + "e-400")
        assertBits(0x2d404bd984990e6fL, "0." + "0".repeat(89) + "1")
        assertBits(0x3e8L, "0." + "0".repeat(320) + "494065645841246544")
    }

    /** A digit far beyond the significant ones still decides a tie. */
    @Test
    fun stickyDigitsBeyondTheTruncationLimitStillCount() {
        val halfway = "1.0000000000000001110223024625156540423631668090820312500"
        assertBits(0x3ff0000000000000L, halfway + "0".repeat(2_000))
        assertBits(0x3ff0000000000001L, halfway + "0".repeat(2_000) + "1")
    }

    @Test
    fun parsesEveryFormattedCorpusValue() {
        for (value in canonicalNumberCorpus(randomCount = 500)) {
            val text = canonicalDoubleToString(value)
            val parsed = parseJavaDouble(text)
            assertNotNull(parsed, "$text was rejected")
            assertEquals(value.toRawBits(), parsed.toRawBits(), "round trip of $text")
        }
    }

    private fun assertBits(expected: Long, input: String) {
        val parsed = parseJavaDouble(input)
        assertNotNull(parsed, "expected '$input' to be accepted")
        assertEquals(
            expected.toString(16),
            parsed.toRawBits().toString(16),
            "wrong value for '$input' (${canonicalDoubleToString(parsed)})",
        )
    }

    private companion object {
        const val NEGATIVE_ZERO_BITS = Long.MIN_VALUE
    }
}
