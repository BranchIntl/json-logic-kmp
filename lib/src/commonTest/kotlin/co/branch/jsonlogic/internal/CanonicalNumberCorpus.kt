package co.branch.jsonlogic.internal

/**
 * A 64-bit linear congruential generator, so that every target walks the exact same corpus and a
 * failure on one lane reproduces on all of them.
 */
internal class SeededBits(private var state: Long) {
    fun next(): Long {
        state = state * 6364136223846793005L + 1442695040888963407L
        return state xor (state ushr 29)
    }
}

internal const val CORPUS_SEED: Long = 0x2545F4914F6CDD1DL

/**
 * Returns a corpus of finite doubles: the smallest subnormals, both ends of every binary exponent,
 * and [randomCount] random bit patterns.
 *
 * The smallest subnormals matter disproportionately. Their rounding intervals are so wide relative
 * to the value that a single significant digit already reads back, which is the one case where
 * Java's shortest-decimal rule looks at two-digit decimals as well.
 */
internal fun canonicalNumberCorpus(randomCount: Int = 10_000): DoubleArray {
    val values = ArrayList<Double>(randomCount + 5_000)
    for (bits in 1L..600L) {
        values.add(Double.fromBits(bits))
        values.add(-Double.fromBits(bits))
    }
    for (exponentBits in 0..2046) {
        val leading = exponentBits.toLong() shl 52
        val sign = if (exponentBits % 2 == 0) 1.0 else -1.0
        values.add(sign * Double.fromBits(leading))
        values.add(sign * Double.fromBits(leading or 0xFFFFFFFFFFFFFL))
        values.add(sign * Double.fromBits(leading or 0x8000000000000L))
    }
    val random = SeededBits(CORPUS_SEED)
    var produced = 0
    while (produced < randomCount) {
        val value = Double.fromBits(random.next())
        if (value.isFinite()) {
            values.add(value)
            produced++
        }
    }
    return values.toDoubleArray()
}
