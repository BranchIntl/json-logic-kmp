package co.branch.jsonlogic.internal

/*
 * Conversions between Double and String that never consult the platform.
 *
 * Kotlin's own `Double.toString()` and `String.toDouble()` are documented as platform-dependent, so
 * every conversion this engine needs is re-implemented here in common code: one implementation,
 * byte-identical on every target.
 *
 * Two renderings are needed, because the engine this library ports and the JsonLogic reference
 * implementation lay a number out differently, and this library follows each where its behavior is
 * the one observed. [canonicalDoubleToString] matches `java.lang.Double.toString` and is what a
 * number renders as inside `log` and `in`; [ecmaDoubleToString] matches ECMAScript's
 * `Number::toString` and is what a number renders as in `cat`, `substr`, and every result crossing
 * back into JSON. Both directions of string coercion — loose equality, numeric comparison,
 * arithmetic — go through [parseJavaDouble].
 */

private const val EXPONENT_MASK = 0x7FFL
private const val SIGNIFICAND_MASK = 0xFFFFFFFFFFFFFL
private const val IMPLICIT_BIT = 1L shl 52
private const val MAGNITUDE_MASK = 0x7FFFFFFFFFFFFFFFL

/** Raw bits of 2^53, the first magnitude whose ulp exceeds one. */
private const val TWO_POW_53_BITS = 0x4340000000000000L

/** log10(2) as a binary fraction, `78913 / 2^18`; used for a first guess that is then corrected. */
private const val LOG10_OF_2_NUMERATOR = 78913L

/** log2(10) as a binary fraction, `217707 / 2^16`; used for a first guess that is then corrected. */
private const val LOG2_OF_10_NUMERATOR = 217707L

/** 10^0 .. 10^18, the powers of ten that are exact in a Long. */
private val POWERS_OF_TEN_LONG = longArrayOf(
    1L, 10L, 100L, 1_000L, 10_000L, 100_000L, 1_000_000L, 10_000_000L, 100_000_000L,
    1_000_000_000L, 10_000_000_000L, 100_000_000_000L, 1_000_000_000_000L, 10_000_000_000_000L,
    100_000_000_000_000L, 1_000_000_000_000_000L, 10_000_000_000_000_000L,
    100_000_000_000_000_000L, 1_000_000_000_000_000_000L,
)

/** 10^0 .. 10^22, the powers of ten that are exact as a Double. */
private val POWERS_OF_TEN_DOUBLE = doubleArrayOf(
    1e0, 1e1, 1e2, 1e3, 1e4, 1e5, 1e6, 1e7, 1e8, 1e9, 1e10, 1e11, 1e12, 1e13, 1e14, 1e15, 1e16,
    1e17, 1e18, 1e19, 1e20, 1e21, 1e22,
)

/**
 * Every double is uniquely identified by 17 significant decimal digits, so the search for the
 * shortest decimal that reads back always succeeds at or before this length.
 */
private const val MAX_SIGNIFICANT_DIGITS = 17

/**
 * Renders [d] exactly as `java.lang.Double.toString(double)` does on JDK 19 and later.
 *
 * The returned decimal is the shortest one that reads back as [d]; among equally short ones the
 * closest to [d], and among two equally close the one with the even significand. When a single
 * significant digit already reads back as [d], two-digit decimals join the contest, because one of
 * them can be strictly closer — that is why `Double.MIN_VALUE` renders as `4.9E-324` rather than
 * `5E-324`. Magnitudes in `[1e-3, 1e7)` are rendered in plain decimal form with at least one digit
 * after the point, everything else in computerized scientific notation.
 *
 * JDK 19 shortened the output of `Double.toString` for the ~0.3% of values where the older
 * algorithm emitted a non-shortest decimal; this function implements the current specification, so
 * it disagrees with a JDK 17 or 18 runtime on exactly those values.
 */
internal fun canonicalDoubleToString(d: Double): String {
    if (d.isNaN()) return "NaN"
    if (d.isInfinite()) return if (d < 0) "-Infinity" else "Infinity"
    if (d == 0.0) return if (d.toRawBits() < 0) "-0.0" else "0.0"

    return shortestDecimal(d, refineSingleDigit = true, ::renderJava)
}

/**
 * Renders [d] exactly as ECMAScript's `Number::toString` does, which is also how `JSON.stringify`
 * writes a finite number: the shortest decimal that reads back as [d], laid out in plain decimal
 * while the magnitude is in `[1e-6, 1e21)` and in exponential form outside it, with no digit ever
 * added after the point — a whole number renders without one at all, and both zeros render as `0`.
 *
 * Infinity and NaN render as `Infinity`, `-Infinity`, and `NaN`, the text ECMAScript's own
 * `String()` produces. `JSON.stringify` writes `null` for all three instead, which discards which
 * one it was.
 */
internal fun ecmaDoubleToString(d: Double): String {
    if (d.isNaN()) return "NaN"
    if (d.isInfinite()) return if (d < 0) "-Infinity" else "Infinity"
    if (d == 0.0) return "0"

    return shortestDecimal(d, refineSingleDigit = false, ::renderEcmaScript)
}

/**
 * Finds the shortest decimal that reads back as the finite, non-zero [d] and hands it to [render] as
 * a sign, a digit string without trailing zeros, and the `decimalExponent` that places the decimal
 * point: `10^(decimalExponent - 1) <= |d| < 10^decimalExponent`.
 *
 * Java and ECMAScript agree on which decimal that is — fewest significant digits, then closest to
 * [d], then the even significand — except at one step. Java lets two-digit decimals compete when a
 * single digit already reads back, since one of them can be strictly closer; that is why
 * `Double.MIN_VALUE` is `4.9E-324` in Java and `5e-324` in ECMAScript. [refineSingleDigit] selects
 * that step.
 */
private inline fun shortestDecimal(
    d: Double,
    refineSingleDigit: Boolean,
    render: (negative: Boolean, digits: String, decimalExponent: Int) -> String,
): String {
    val bits = d.toRawBits()
    val negative = bits < 0
    val biasedExponent = ((bits ushr 52) and EXPONENT_MASK).toInt()
    val significand = bits and SIGNIFICAND_MASK

    // Integral magnitudes below 2^53 are their own shortest representation: the rounding interval
    // is at most half a unit wide, so it holds no second integer, and a decimal with fewer
    // significant digits would have to be a multiple of ten and therefore further away than that.
    val magnitudeBits = bits and MAGNITUDE_MASK
    if (magnitudeBits < TWO_POW_53_BITS) {
        val magnitude = Double.fromBits(magnitudeBits)
        val integral = magnitude.toLong()
        if (integral.toDouble() == magnitude) {
            val plain = integral.toString()
            return render(negative, plain.trimEnd('0'), plain.length)
        }
    }

    val significandValue: Long
    val binaryExponent: Int
    if (biasedExponent == 0) {
        significandValue = significand
        binaryExponent = -1074
    } else {
        significandValue = significand or IMPLICIT_BIT
        binaryExponent = biasedExponent - 1075
    }

    // The decimals that read back as v = significandValue * 2^binaryExponent are those between the
    // midpoints to the neighbouring doubles. Both midpoints and v itself are whole multiples of
    // 2^(binaryExponent - 2), which is the scale everything below is expressed in. The interval is
    // closed exactly when the significand is even, since decimal-to-binary rounding is half-even.
    val scaledValue = 4 * significandValue
    val scaledUpperBound = scaledValue + 2
    val scaledLowerBound = if (significand == 0L && biasedExponent > 1) {
        // A power of two has a nearer lower neighbour: the ulp below it is half the ulp above.
        scaledValue - 1
    } else {
        scaledValue - 2
    }
    val scale = binaryExponent - 2
    val closedInterval = (significandValue and 1L) == 0L

    // decimalExponent places the decimal point: 10^(decimalExponent - 1) <= v < 10^decimalExponent.
    var decimalExponent = estimateDecimalExponent(significandValue, binaryExponent)
    while (comparePowerOfTenTo(decimalExponent - 1, scaledValue, scale) > 0) decimalExponent--
    while (comparePowerOfTenTo(decimalExponent, scaledValue, scale) <= 0) decimalExponent++

    // Candidates are compared on the finest grid in play, that of 17 significant digits, so that a
    // single power of five covers every comparison in this call.
    val comparator = ScaledComparator(decimalExponent - MAX_SIGNIFICANT_DIGITS, scale)
    val truncated = truncateToSeventeenDigits(comparator, scaledValue)

    var chosen = 0L
    var chosenLength = 0
    for (length in 1..MAX_SIGNIFICANT_DIGITS) {
        val unit = POWERS_OF_TEN_LONG[MAX_SIGNIFICANT_DIGITS - length]
        val lower = truncated / unit * unit
        val candidate = pickCandidate(
            comparator, lower, unit, scaledValue, scaledLowerBound, scaledUpperBound, closedInterval,
        )
        if (candidate != 0L) {
            chosen = candidate
            chosenLength = length
            break
        }
    }
    if (refineSingleDigit && chosenLength == 1) {
        val unit = POWERS_OF_TEN_LONG[MAX_SIGNIFICANT_DIGITS - 2]
        val lower = truncated / unit * unit
        val candidate = pickCandidate(
            comparator, lower, unit, scaledValue, scaledLowerBound, scaledUpperBound, closedInterval,
        )
        if (candidate != 0L) chosen = candidate
    }

    val digits = chosen.toString()
    return render(negative, digits.trimEnd('0'), decimalExponent - MAX_SIGNIFICANT_DIGITS + digits.length)
}

/**
 * Returns the double [java.lang.Double.parseDouble] would produce for [s], or null where it would
 * throw `NumberFormatException`.
 *
 * The accepted grammar is the one documented on `Double.valueOf(String)` as implemented by
 * `FloatingDecimal.readJavaFormatString`: surrounding characters up to `' '` are trimmed away, then
 * an optional sign followed by `NaN`, `Infinity`, a decimal numeral with an optional exponent, or a
 * hexadecimal numeral with a mandatory binary exponent, optionally closed by one `f`, `F`, `d` or
 * `D`. Conversion is correctly rounded (half-even) for inputs of any length.
 */
internal fun parseJavaDouble(s: String): Double? {
    var start = 0
    var end = s.length
    while (start < end && s[start] <= ' ') start++
    while (end > start && s[end - 1] <= ' ') end--
    if (start == end) return null

    var i = start
    var negative = false
    var signSeen = false
    when (s[i]) {
        '-' -> {
            negative = true
            signSeen = true
            i++
        }
        '+' -> {
            signSeen = true
            i++
        }
    }
    if (i == end) return null
    when (s[i]) {
        'N' -> return if (end - i == 3 && s.startsWith("NaN", i)) Double.NaN else null
        'I' -> return if (end - i == 8 && s.startsWith("Infinity", i)) {
            if (negative) Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY
        } else {
            null
        }
        '0' -> if (i + 1 < end && (s[i + 1] == 'x' || s[i + 1] == 'X')) {
            return parseHexadecimal(s, i + 2, end, negative)
        }
    }

    // Java scans the leading zeros (and a decimal point among them) first, then the significant
    // digits, so that the point's character offset minus the number of leading zeros gives the
    // decimal exponent directly.
    val digits = StringBuilder(end - i)
    var digitCount = 0
    var trailingZeroCount = 0
    var leadingZeroCount = 0
    var pointSeen = false
    var digitsBeforePoint = 0
    while (i < end) {
        val c = s[i]
        if (c == '0') {
            leadingZeroCount++
        } else if (c == '.') {
            if (pointSeen) return null
            pointSeen = true
            digitsBeforePoint = i - start - (if (signSeen) 1 else 0)
        } else {
            break
        }
        i++
    }
    while (i < end) {
        val c = s[i]
        if (c in '0'..'9') {
            digits.append(c)
            digitCount++
            if (c == '0') trailingZeroCount++ else trailingZeroCount = 0
        } else if (c == '.') {
            if (pointSeen) return null
            pointSeen = true
            digitsBeforePoint = i - start - (if (signSeen) 1 else 0)
        } else {
            break
        }
        i++
    }
    digitCount -= trailingZeroCount
    val isZero = digitCount == 0
    if (isZero && leadingZeroCount == 0) return null
    var decimalExponent = if (pointSeen) {
        digitsBeforePoint - leadingZeroCount
    } else {
        digitCount + trailingZeroCount
    }

    if (i < end && (s[i] == 'e' || s[i] == 'E')) {
        i++
        var exponentNegative = false
        if (i < end && (s[i] == '-' || s[i] == '+')) {
            exponentNegative = s[i] == '-'
            i++
        }
        val exponentStart = i
        var exponentValue = 0L
        while (i < end && s[i] in '0'..'9') {
            if (exponentValue < 1_000_000_000L) exponentValue = exponentValue * 10 + (s[i] - '0')
            i++
        }
        if (i == exponentStart) return null
        val shifted = decimalExponent + if (exponentNegative) -exponentValue else exponentValue
        decimalExponent = shifted.coerceIn(-1_000_000L, 1_000_000L).toInt()
    }
    // A single f/F/d/D may close the numeral; anything else left over is an error.
    if (i < end) {
        if (i != end - 1) return null
        val c = s[i]
        if (c != 'f' && c != 'F' && c != 'd' && c != 'D') return null
    }
    if (isZero) return if (negative) -0.0 else 0.0

    return decimalToDouble(digits, digitCount, decimalExponent, negative)
}

/**
 * Digits beyond this count are folded into a sticky digit. The exact value of a double, and of the
 * midpoint between two doubles, needs at most 767 significant digits, so no rounding boundary can
 * lie strictly inside the interval a longer input pins down: rounding is unaffected.
 */
private const val MAX_PARSED_DIGITS = 1100

private fun decimalToDouble(
    digits: StringBuilder,
    digitCount: Int,
    decimalExponent: Int,
    negative: Boolean,
): Double {
    // v = 0.digits * 10^decimalExponent, so 10^(decimalExponent - 1) <= v < 10^decimalExponent.
    if (decimalExponent >= 310) {
        return if (negative) Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY
    }
    if (decimalExponent <= -324) return if (negative) -0.0 else 0.0

    var count = digitCount
    var sticky = false
    if (count > MAX_PARSED_DIGITS) {
        count = MAX_PARSED_DIGITS
        sticky = true
    }

    if (!sticky && count <= 15) {
        // Both operands are exact here (a 15-digit integer and a power of ten below 10^23), so the
        // single multiply or divide is the correctly rounded result of the whole conversion.
        var significand = 0L
        for (k in 0 until count) significand = significand * 10 + (digits[k] - '0')
        val powerOfTen = decimalExponent - count
        val magnitude = when {
            powerOfTen == 0 -> significand.toDouble()
            powerOfTen in 1..22 -> significand.toDouble() * POWERS_OF_TEN_DOUBLE[powerOfTen]
            powerOfTen in -22..-1 -> significand.toDouble() / POWERS_OF_TEN_DOUBLE[-powerOfTen]
            else -> Double.NaN
        }
        if (!magnitude.isNaN()) return if (negative) -magnitude else magnitude
    }

    val value = BigUInt(count / 9 + 2)
    value.set(0L)
    var k = 0
    while (k < count) {
        val chunkLength = if (count - k < 9) count - k else 9
        var chunk = 0
        for (j in 0 until chunkLength) chunk = chunk * 10 + (digits[k + j] - '0')
        if (sticky && k + chunkLength == count) {
            // Java's sticky digit: the truncated value keeps a one in the last kept place, which
            // leaves it strictly between the truncation and its successor, exactly where the full
            // input sits (its last digit is never zero).
            chunk = chunk / 10 * 10 + 1
        }
        value.mulAdd(POWERS_OF_TEN_LONG[chunkLength].toInt(), chunk)
        k += chunkLength
    }

    val comparator = ValueComparator(value, decimalExponent - count)
    var exponent = ((decimalExponent.toLong() * LOG2_OF_10_NUMERATOR) shr 16).toInt()
    while (comparator.compare(1L, exponent) > 0) exponent--
    while (comparator.compare(1L, exponent + 1) <= 0) exponent++

    // Normalized doubles are multiples of 2^(exponent - 52); subnormals of 2^-1074.
    val quantum = if (exponent - 52 < -1074) -1074 else exponent - 52
    var mantissa = 0L
    var bit = 1L shl 53
    while (bit != 0L) {
        val trial = mantissa or bit
        if (comparator.compare(trial, quantum - 1) <= 0) mantissa = trial
        bit = bit ushr 1
    }
    val exact = comparator.compare(mantissa, quantum - 1) == 0
    return assemble(mantissa, quantum, !exact, negative)
}

private fun parseHexadecimal(s: String, from: Int, end: Int, negative: Boolean): Double? {
    var i = from
    // The scanned value is significand * 16^droppedDigits * 16^-fractionDigits * 2^(p exponent):
    // [significand] keeps the leading hex digits, [droppedDigits] counts those that no longer fit
    // and [sticky] records whether any of them was nonzero.
    var significand = 0L
    var droppedDigits = 0
    var fractionDigits = 0
    var sticky = false
    var pointSeen = false
    var anyDigit = false
    while (i < end) {
        val c = s[i]
        if (c == '.') {
            if (pointSeen) return null
            pointSeen = true
            i++
            continue
        }
        val digit = hexadecimalValue(c)
        if (digit < 0) break
        anyDigit = true
        if (pointSeen) fractionDigits++
        if (significand != 0L) {
            // Keep at least 56 bits, enough for the 53-bit result plus its round and sticky bits.
            if (significand < (1L shl 56)) {
                significand = (significand shl 4) or digit.toLong()
            } else {
                droppedDigits++
                if (digit != 0) sticky = true
            }
        } else if (digit != 0) {
            significand = digit.toLong()
        }
        i++
    }
    if (!anyDigit) return null
    if (i >= end || (s[i] != 'p' && s[i] != 'P')) return null
    i++
    var exponentNegative = false
    if (i < end && (s[i] == '-' || s[i] == '+')) {
        exponentNegative = s[i] == '-'
        i++
    }
    val exponentStart = i
    var exponentValue = 0L
    while (i < end && s[i] in '0'..'9') {
        if (exponentValue < 1_000_000_000L) exponentValue = exponentValue * 10 + (s[i] - '0')
        i++
    }
    if (i == exponentStart) return null
    if (i < end) {
        if (i != end - 1) return null
        val c = s[i]
        if (c != 'f' && c != 'F' && c != 'd' && c != 'D') return null
    }
    if (significand == 0L) return if (negative) -0.0 else 0.0

    val binaryExponent = (if (exponentNegative) -exponentValue else exponentValue) +
        4L * (droppedDigits - fractionDigits)
    val bitLength = 64 - significand.countLeadingZeroBits()
    val exponent = bitLength - 1 + binaryExponent
    if (exponent >= 1024) return if (negative) Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY
    if (exponent < -1075) return if (negative) -0.0 else 0.0

    val quantum = if (exponent - 52 < -1074) -1074 else (exponent - 52).toInt()
    val shift = quantum - 1 - binaryExponent
    return if (shift <= 0) {
        // At most 54 significant bits: the whole significand fits above the rounding bit, and no
        // hex digit was folded into the sticky flag either (that needs 57 bits or more).
        assemble(significand shl (-shift).toInt(), quantum, false, negative)
    } else if (shift >= 64) {
        // Below half the smallest quantum; the round bit is zero either way, so this reads zero.
        assemble(0L, quantum, true, negative)
    } else {
        val dropped = shift.toInt()
        val mantissa = significand ushr dropped
        val lost = significand and ((1L shl dropped) - 1) != 0L
        assemble(mantissa, quantum, lost || sticky, negative)
    }
}

/**
 * Rounds `mantissa * 2^(quantum - 1)` to the nearest double, half to even, where [mantissa] carries
 * one bit below the target quantum and [inexact] reports whether anything was discarded below it.
 */
private fun assemble(mantissa: Long, quantum: Int, inexact: Boolean, negative: Boolean): Double {
    var m = mantissa ushr 1
    var q = quantum
    if ((mantissa and 1L) == 1L && (inexact || (m and 1L) == 1L)) m++
    if (m == (1L shl 53)) {
        m = 1L shl 52
        q++
    }
    val bits: Long
    if (m < IMPLICIT_BIT) {
        // Subnormal: q is the fixed subnormal quantum and the significand doubles as the encoding.
        bits = m
    } else {
        val biased = q + 1075
        if (biased >= 2047) {
            return if (negative) Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY
        }
        bits = (biased.toLong() shl 52) or (m - IMPLICIT_BIT)
    }
    val magnitude = Double.fromBits(bits)
    return if (negative) -magnitude else magnitude
}

private fun hexadecimalValue(c: Char): Int = when (c) {
    in '0'..'9' -> c - '0'
    in 'a'..'f' -> c - 'a' + 10
    in 'A'..'F' -> c - 'A' + 10
    else -> -1
}

/**
 * Compares `x * 10^p` against `y * 2^q` exactly, for the `p` and `q` fixed at construction. The
 * decimal scale is folded into a single power of five, so each comparison costs one multiplication
 * by that constant plus a shift.
 */
private class ScaledComparator(p: Int, q: Int) {
    private val powerOfFive = bigPowerOfFive(if (p >= 0) p else -p)
    private val scaleLeft = p >= 0
    private val shiftLeft: Int
    private val shiftRight: Int
    private val left = BigUInt()
    private val right = BigUInt()

    init {
        val difference = p - q
        shiftLeft = if (difference > 0) difference else 0
        shiftRight = if (difference < 0) -difference else 0
    }

    fun compare(x: Long, y: Long): Int {
        left.set(x)
        if (scaleLeft) left.mul(powerOfFive)
        left.shiftLeft(shiftLeft)
        right.set(y)
        if (!scaleLeft) right.mul(powerOfFive)
        right.shiftLeft(shiftRight)
        return left.compareTo(right)
    }
}

/** Compares `m * 2^b` against the fixed decimal `digits * 10^scale`. */
private class ValueComparator(digits: BigUInt, private val scale: Int) {
    private val powerOfFive = bigPowerOfFive(if (scale >= 0) scale else -scale)
    private val scaleCandidate = scale < 0
    private val value = BigUInt().also {
        it.set(digits)
        if (scale >= 0) it.mul(powerOfFive)
    }
    private val left = BigUInt()
    private val right = BigUInt()

    fun compare(m: Long, b: Int): Int {
        left.set(m)
        if (scaleCandidate) left.mul(powerOfFive)
        right.set(value)
        val difference = b - scale
        if (difference >= 0) left.shiftLeft(difference) else right.shiftLeft(-difference)
        return left.compareTo(right)
    }
}

private fun estimateDecimalExponent(significand: Long, binaryExponent: Int): Int {
    val bitLength = 64 - significand.countLeadingZeroBits()
    return (((bitLength + binaryExponent).toLong() * LOG10_OF_2_NUMERATOR) shr 18).toInt() + 1
}

private fun comparePowerOfTenTo(p: Int, y: Long, q: Int): Int = ScaledComparator(p, q).compare(1L, y)

/**
 * Returns v truncated to 17 significant decimal digits, as the integer those digits form. [value] is
 * v in the binary units [comparator] was built for, and [comparator] holds both scales.
 */
private fun truncateToSeventeenDigits(comparator: ScaledComparator, value: Long): Long {
    var low = POWERS_OF_TEN_LONG[16]
    var high = POWERS_OF_TEN_LONG[17] - 1
    while (low < high) {
        val middle = low + (high - low + 1) / 2
        if (comparator.compare(middle, value) <= 0) low = middle else high = middle - 1
    }
    return low
}

/**
 * Returns whichever of the two decimals bracketing v on the grid of [unit] reads back as v — the
 * closer one when both do, the even significand when they are equally close — or 0 when neither
 * does. Both are expressed on the 17-digit grid, so the result is a multiple of [unit].
 */
private fun pickCandidate(
    comparator: ScaledComparator,
    lower: Long,
    unit: Long,
    value: Long,
    lowerBound: Long,
    upperBound: Long,
    closedInterval: Boolean,
): Long {
    val upper = lower + unit
    val lowerReadsBack = readsBack(comparator, lower, lowerBound, upperBound, closedInterval)
    val upperReadsBack = readsBack(comparator, upper, lowerBound, upperBound, closedInterval)
    if (!lowerReadsBack && !upperReadsBack) return 0L
    if (!upperReadsBack) return lower
    if (!lowerReadsBack) return upper
    // Compare the midpoint of the two candidates against v to find the closer one.
    val versusMidpoint = comparator.compare(lower + upper, 2 * value)
    return when {
        versusMidpoint > 0 -> lower
        versusMidpoint < 0 -> upper
        (lower / unit) and 1L == 0L -> lower
        else -> upper
    }
}

private fun readsBack(
    comparator: ScaledComparator,
    candidate: Long,
    lowerBound: Long,
    upperBound: Long,
    closedInterval: Boolean,
): Boolean {
    val versusLower = comparator.compare(candidate, lowerBound)
    if (versusLower < 0 || (versusLower == 0 && !closedInterval)) return false
    val versusUpper = comparator.compare(candidate, upperBound)
    return versusUpper < 0 || (versusUpper == 0 && closedInterval)
}

/**
 * Formats `0.digits * 10^decimalExponent` the way Java lays a double out: plain decimal with at
 * least one digit on each side of the point while the magnitude is in `[1e-3, 1e7)`, and
 * `d.dddEnn` outside it.
 */
private fun renderJava(negative: Boolean, digits: String, decimalExponent: Int): String {
    val builder = StringBuilder(26)
    if (negative) builder.append('-')
    val length = digits.length
    if (decimalExponent in 1..7) {
        if (decimalExponent >= length) {
            builder.append(digits)
            repeat(decimalExponent - length) { builder.append('0') }
            builder.append(".0")
        } else {
            builder.appendRange(digits, 0, decimalExponent)
            builder.append('.')
            builder.appendRange(digits, decimalExponent, length)
        }
    } else if (decimalExponent in -2..0) {
        builder.append("0.")
        repeat(-decimalExponent) { builder.append('0') }
        builder.append(digits)
    } else {
        builder.append(digits[0])
        builder.append('.')
        if (length == 1) builder.append('0') else builder.appendRange(digits, 1, length)
        builder.append('E')
        builder.append(decimalExponent - 1)
    }
    return builder.toString()
}

/**
 * Formats `0.digits * 10^decimalExponent` the way ECMAScript lays a number out: plain decimal while
 * the magnitude is in `[1e-6, 1e21)`, and `de+nn` outside it. Nothing is ever padded — a whole
 * number gets no decimal point, and a single significant digit gets no companion.
 */
private fun renderEcmaScript(negative: Boolean, digits: String, decimalExponent: Int): String {
    val builder = StringBuilder(26)
    if (negative) builder.append('-')
    val length = digits.length
    if (decimalExponent in -5..21) {
        if (decimalExponent >= length) {
            builder.append(digits)
            repeat(decimalExponent - length) { builder.append('0') }
        } else if (decimalExponent > 0) {
            builder.appendRange(digits, 0, decimalExponent)
            builder.append('.')
            builder.appendRange(digits, decimalExponent, length)
        } else {
            builder.append("0.")
            repeat(-decimalExponent) { builder.append('0') }
            builder.append(digits)
        }
    } else {
        builder.append(digits[0])
        if (length > 1) {
            builder.append('.')
            builder.appendRange(digits, 1, length)
        }
        builder.append('e')
        val exponent = decimalExponent - 1
        if (exponent > 0) builder.append('+')
        builder.append(exponent)
    }
    return builder.toString()
}
