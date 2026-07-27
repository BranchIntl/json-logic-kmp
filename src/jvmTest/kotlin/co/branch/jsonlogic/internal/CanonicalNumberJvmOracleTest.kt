package co.branch.jsonlogic.internal

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Checks the common-code conversions against the JVM they are modelled on.
 *
 * `Double.toString` is compared two ways: against an independent implementation of the JDK 19+
 * specification built from `BigDecimal` (the authority, since it is the same on every JDK) and
 * against the running JDK's own output. On a JDK 17 or 18 runtime the latter disagrees on the
 * values whose legacy rendering was not the shortest decimal, so those are only checked for being
 * legacy-longer renderings of the same double, and reported.
 */
class CanonicalNumberJvmOracleTest {

    @Test
    fun formattingMatchesTheSpecification() {
        var checked = 0
        for (value in oracleCorpus()) {
            assertEquals(
                specificationToString(value),
                canonicalDoubleToString(value),
                "bits=0x${java.lang.Long.toHexString(java.lang.Double.doubleToRawLongBits(value))}",
            )
            checked++
        }
        println("formattingMatchesTheSpecification: $checked values")
        assertTrue(checked >= 110_000, "corpus shrank to $checked values")
    }

    @Test
    fun formattingMatchesThisJdk() {
        val runtime = System.getProperty("java.specification.version").toInt()
        val shortestSince = 19
        var checked = 0
        var longerOnThisJdk = 0
        var lessAccurateOnThisJdk = 0
        val samples = StringBuilder()
        for (value in oracleCorpus()) {
            val mine = canonicalDoubleToString(value)
            val theirs = value.toString()
            checked++
            if (mine == theirs) continue
            val bits = java.lang.Long.toHexString(java.lang.Double.doubleToRawLongBits(value))
            assertTrue(
                runtime < shortestSince,
                "JDK $runtime disagrees on bits=0x$bits: mine=$mine jdk=$theirs",
            )
            // Whatever the legacy algorithm rendered still reads back as the same double; it just
            // was not always the shortest, nor — among the very smallest subnormals, where a single
            // digit suffices — the closest.
            assertEquals(java.lang.Double.parseDouble(theirs), value, "jdk output $theirs")
            if (significantDigits(mine) < significantDigits(theirs)) longerOnThisJdk++ else lessAccurateOnThisJdk++
            if (longerOnThisJdk + lessAccurateOnThisJdk <= 12) {
                samples.append("\n  bits=0x").append(bits)
                    .append(" jdk=").append(theirs).append(" spec=").append(mine)
            }
        }
        println(
            "formattingMatchesThisJdk: JDK $runtime, $checked values, " +
                "$longerOnThisJdk longer on this JDK, $lessAccurateOnThisJdk less accurate$samples",
        )
        if (runtime >= shortestSince) assertEquals(0, longerOnThisJdk + lessAccurateOnThisJdk)
    }

    @Test
    fun parsingMatchesJavaParseDouble() {
        var accepted = 0
        var rejected = 0
        for (input in parseCorpus()) {
            val expected = try {
                java.lang.Double.parseDouble(input)
            } catch (e: NumberFormatException) {
                assertNull(parseJavaDouble(input), "'$input' should have been rejected")
                rejected++
                continue
            }
            val actual = parseJavaDouble(input)
            assertNotNull(actual, "'$input' should have been accepted")
            if (expected.isNaN()) {
                assertTrue(actual.isNaN(), "'$input' should be NaN")
            } else {
                assertEquals(
                    java.lang.Double.doubleToRawLongBits(expected).toString(16),
                    actual.toRawBits().toString(16),
                    "'$input'",
                )
            }
            accepted++
        }
        println("parsingMatchesJavaParseDouble: $accepted accepted, $rejected rejected")
        assertTrue(accepted >= 40_000, "only $accepted accepted inputs")
        assertTrue(rejected >= 2_000, "only $rejected rejected inputs")
    }

    @Test
    fun splittingMatchesJavaStringSplit() {
        val subjects = mutableListOf(
            "", ".", "..", "a", "a.", ".a", "a.b", "a..b", "a.b.", ".a.b.", "...a...b...",
            "a.b.c.d.e", "1.2.3", "..", "....", " . ", ".0.", "0",
        )
        val random = java.util.Random(12345)
        repeat(5_000) {
            val length = random.nextInt(12)
            val builder = StringBuilder()
            repeat(length) {
                builder.append(if (random.nextInt(3) == 0) '.' else ('a' + random.nextInt(3)))
            }
            subjects.add(builder.toString())
        }
        for (subject in subjects) {
            assertEquals(javaSplitReference(subject), javaSplitOnDot(subject), "'$subject'")
        }
    }

    /**
     * `String.split(regex)` delegates to `Pattern.split(input, 0)`, which is reachable from Kotlin
     * (`String.split` resolves to Kotlin's own, differently behaved, extension).
     */
    private fun javaSplitReference(subject: String): List<String> =
        java.util.regex.Pattern.compile("\\.").split(subject, 0).toList()

    // ---- oracle -------------------------------------------------------------------------------

    /**
     * `Double.toString` as specified from JDK 19 onwards: the shortest decimal that reads back as
     * the value, the closest one when several are equally short, the even significand on a tie —
     * and, when a single digit suffices, the closest decimal of one or two digits.
     */
    private fun specificationToString(value: Double): String {
        if (value.isNaN()) return "NaN"
        if (value == Double.POSITIVE_INFINITY) return "Infinity"
        if (value == Double.NEGATIVE_INFINITY) return "-Infinity"
        if (value == 0.0) return if (1 / value < 0) "-0.0" else "0.0"
        val negative = value < 0
        val magnitude = kotlin.math.abs(value)
        val exact = BigDecimal(magnitude)

        var chosen: BigDecimal? = null
        var length = 0
        for (candidateLength in 1..17) {
            chosen = closestThatReadsBack(magnitude, exact, candidateLength)
            if (chosen != null) {
                length = candidateLength
                break
            }
        }
        if (length == 1) chosen = closestThatReadsBack(magnitude, exact, 2) ?: chosen
        val stripped = chosen!!.stripTrailingZeros()
        val digits = stripped.unscaledValue().toString()
        return renderReference(negative, digits, digits.length - stripped.scale())
    }

    private fun closestThatReadsBack(
        magnitude: Double,
        exact: BigDecimal,
        length: Int,
    ): BigDecimal? {
        val down = exact.round(MathContext(length, RoundingMode.FLOOR))
        val up = exact.round(MathContext(length, RoundingMode.CEILING))
        var best: BigDecimal? = null
        var bestDistance: BigDecimal? = null
        for (candidate in if (down.compareTo(up) == 0) listOf(down) else listOf(down, up)) {
            if (java.lang.Double.parseDouble(candidate.toString()) != magnitude) continue
            val distance = candidate.subtract(exact).abs()
            val comparison = if (bestDistance == null) -1 else distance.compareTo(bestDistance)
            if (comparison < 0) {
                best = candidate
                bestDistance = distance
            } else if (comparison == 0 && !candidate.stripTrailingZeros().unscaledValue().testBit(0)) {
                best = candidate
                bestDistance = distance
            }
        }
        return best
    }

    /** Java's layout: plain decimal while the magnitude is in `[1e-3, 1e7)`, scientific outside. */
    private fun renderReference(negative: Boolean, digits: String, pointExponent: Int): String {
        val builder = StringBuilder()
        if (negative) builder.append('-')
        val length = digits.length
        when {
            pointExponent in 1..7 -> if (pointExponent >= length) {
                builder.append(digits).append("0".repeat(pointExponent - length)).append(".0")
            } else {
                builder.append(digits, 0, pointExponent).append('.').append(digits, pointExponent, length)
            }
            pointExponent in -2..0 -> builder.append("0.").append("0".repeat(-pointExponent)).append(digits)
            else -> {
                builder.append(digits[0]).append('.')
                if (length == 1) builder.append('0') else builder.append(digits, 1, length)
                builder.append('E').append(pointExponent - 1)
            }
        }
        return builder.toString()
    }

    private fun significantDigits(rendered: String): Int {
        val body = rendered.removePrefix("-").substringBefore('E')
        return body.filter { it in '0'..'9' }.trimStart('0').trimEnd('0').length.coerceAtLeast(1)
    }

    // ---- corpora ------------------------------------------------------------------------------

    /** The shared cross-target corpus plus 100 000 further seeded bit patterns. */
    private fun oracleCorpus(): DoubleArray {
        val shared = canonicalNumberCorpus()
        val extra = DoubleArray(100_000)
        val random = SeededBits(0x123456789ABCDEFL)
        var produced = 0
        while (produced < extra.size) {
            val value = Double.fromBits(random.next())
            if (value.isFinite()) extra[produced++] = value
        }
        return shared + extra
    }

    private fun parseCorpus(): List<String> {
        val inputs = mutableListOf<String>()
        inputs += GRAMMAR_CASES
        // Every rendering of a corpus double, plus decorations that must not change the value.
        for (value in canonicalNumberCorpus(randomCount = 4_000)) {
            val rendered = canonicalDoubleToString(value)
            inputs += rendered
            inputs += " $rendered "
            inputs += "${rendered}d"
            inputs += rendered.replace("E", "e")
        }
        // Digit strings of every length, with and without exponents, signs and points.
        val random = SeededBits(0x0FEDCBA987654321L)
        repeat(20_000) {
            val digitCount = 1 + (random.next() ushr 40).toInt() % 40
            val builder = StringBuilder()
            if (random.next() and 1L == 0L) builder.append(if (random.next() and 1L == 0L) '-' else '+')
            val point = (random.next() ushr 40).toInt() % (digitCount + 1)
            repeat(digitCount) { index ->
                if (index == point) builder.append('.')
                builder.append('0' + (random.next() ushr 40).toInt() % 10)
            }
            if (random.next() and 3L != 0L) {
                builder.append(if (random.next() and 1L == 0L) 'e' else 'E')
                builder.append((random.next() ushr 40).toInt() % 700 - 350)
            }
            inputs += builder.toString()
        }
        // Long inputs: exact values, exact midpoints, and midpoints nudged either way.
        val random2 = SeededBits(0x5EEDFACE5EEDFACEL)
        repeat(400) {
            val value = Double.fromBits(random2.next())
            if (!value.isFinite() || value == 0.0) return@repeat
            val exact = BigDecimal(kotlin.math.abs(value))
            inputs += exact.toPlainString()
            inputs += exact.toString()
            val gap = BigDecimal(Math.ulp(kotlin.math.abs(value))).divide(BigDecimal(2))
            val midpoint = exact.add(gap)
            inputs += midpoint.toPlainString()
            inputs += midpoint.add(BigDecimal("1E-1100")).toPlainString()
            inputs += midpoint.subtract(BigDecimal("1E-1100")).toPlainString()
        }
        // Single-character mutations of well-formed input, to pin the accept/reject boundary.
        val mutants = SeededBits(0x00C0FFEE00C0FFEEL)
        val mutations = ",. -+eEfFdDpPxX019NnIi\u0000\t"
        val wellFormed = inputs.toList()
        repeat(12_000) {
            val subject = wellFormed[(mutants.next() ushr 40).toInt() % wellFormed.size]
            val position = (mutants.next() ushr 40).toInt() % (subject.length + 1)
            val character = mutations[(mutants.next() ushr 40).toInt() % mutations.length]
            inputs += subject.substring(0, position) + character + subject.substring(position)
            if (position < subject.length) {
                inputs += subject.substring(0, position) + character + subject.substring(position + 1)
                inputs += subject.substring(0, position) + subject.substring(position + 1)
            }
        }
        // Hexadecimal forms.
        val random3 = SeededBits(0x1234ABCD5678EF90L)
        repeat(2_000) {
            val bits = random3.next()
            val hex = java.lang.Long.toHexString(bits ushr (1 + (bits.toInt() and 15)))
            val exponent = ((random3.next() ushr 40).toInt() % 2400) - 1200
            inputs += "0x$hex" + "p$exponent"
            inputs += "0x$hex.${java.lang.Long.toHexString(random3.next())}p$exponent"
            inputs += "0x.${hex}p$exponent"
        }
        return inputs
    }

    private companion object {
        /** Grammar corners, each verified against `Double.parseDouble` by the test itself. */
        val GRAMMAR_CASES = listOf(
            "", " ", "\t", "\n", " \t\r\n\u000b\u000c ", "1", " 1.0 ", "\t1.0\n", " 1", "1 ",
            "1\u0000", "\u00001", "\u001f1\u001f", "+1", "-1", "1.", ".1", ".", "-.", "+.", "-", "+",
            "0", "-0", "0.0", "-0.0", "00", "0.", ".0", "000", "00.5", "0.000", "-0.000",
            "0e999999999", "-0e999999999", "0e-999999999", "0e", "0.0x", "0garbage",
            "1e", "1e+", "1e-", "1e3", "1E-3", "1e03", "1e+3", "1e5x", "1e5f", "1e5d",
            "1f", "1F", "1d", "1D", "1fd", "1df", "1.0d", "1.0D", ".5", "5.", "5.f",
            "NaN", "nan", "NAN", "NaNf", "NaN ", "+NaN", "-NaN", "NaNN",
            "Infinity", "infinity", "INFINITY", "+Infinity", "-Infinity", "Infinityf", "Inf",
            "Infinit", "--1", "+-1", "-+1", "1,0", "1.2.3", "abc", "1 0", "1_000", "1e1e1",
            "0x", "0X", "0x1", "0x1p1", "0X1P1", "0x1.8p1", "0x1.p1", "0x.8p1", "0x.p1", "0x1p",
            "0x1p+", "0x1p+1", "0x1p-1", "0x1p1f", "0x1p1d", "0x1p1F", "0xAp0", "0xap0", "0xfp0",
            "0xFp0", "0xgp0", "0x1.8p1x", "0x0p0", "-0x1p1", "+0x1p1", "0x1p1000", "0x1p-1100",
            "0x10p-4", "0x.0000000000000000001p100", "0x1.fffffffffffffp1023",
            "0x1.fffffffffffff8p1023", "0x1p1024", "0x1p-1074", "0x1p-1075", "0x1.8p-1074",
            "0x0.0000000000001p-1022", "0x1234567890abcdefp0", " 0x1p1 ", "0x1.8", "0x1.8f",
            "0x1p2147483647", "0x1p2147483648", "0x1p-2147483648", "0x1p-2147483649",
            "0x1p99999999999999999999", "0x0p-99999999999", "0x0.0p0", "0x00000001p0",
            "0x000.000p0", "1.e1", ".e1", "0.e1", "1.0e", "0x1p1 x", "0x 1p1", "0 x1p1", "0x1 p1",
            "0x1p1.5", "0x1.8p1.5", "0x1p0x1", "1e0", "1e00000000003", "1e0000000000000000000003",
            "1.5e-0", "-1e-0", "1e310", "1e309", "1e308", "1.7976931348623157e308",
            "1.7976931348623158e308", "1.797693134862315808e308", "1e-323", "1e-324", "4.9e-324",
            "2.4703282292062327e-324", "2.4703282292062328e-324", "1e-325", "1e-400",
            "9.88131291682493088353e-324", "0.1",
            "0.1000000000000000055511151231257827021181583404541015625",
            "0.10000000000000000555111512312578270211815834045410156250000000001",
            "0.100000000000000005551115123125782702118158340454101562499999999",
            "1.0000000000000002220446049250313080847263336181640625",
            "1.00000000000000022204460492503130808472633361816406250000001",
            "1.0000000000000001110223024625156540423631668090820312500",
            "1.00000000000000011102230246251565404236316680908203125000000001",
            "2.2250738585072011e-308", "1e2147483647", "1e2147483648", "1e-2147483648",
            "1e-2147483649", "1e99999999999999999999", "123456789012345678901234567890",
            "1000000000000000000000000000000000000000e-40",
        )
    }
}
