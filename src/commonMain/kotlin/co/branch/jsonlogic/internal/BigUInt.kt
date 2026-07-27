package co.branch.jsonlogic.internal

/**
 * A mutable unsigned integer of arbitrary size, held as a little-endian base-2^32 magnitude.
 *
 * Only the operations the exact decimal/binary conversions in `CanonicalNumber.kt` need are
 * provided: assignment, multiplication, left shift and comparison. Division is deliberately
 * absent — both conversions are expressed as comparisons between exactly scaled integers, which
 * keeps the arithmetic exact and the implementation small enough to audit.
 */
internal class BigUInt(initialCapacity: Int = 4) {
    private var words = IntArray(if (initialCapacity < 2) 2 else initialCapacity)

    /** Number of significant words; `words[size - 1]` is never zero. */
    private var size = 0

    /** Scratch buffer for [mul], swapped in place so repeated multiplies do not allocate. */
    private var product = IntArray(0)

    fun set(value: Long) {
        size = 0
        if (value == 0L) return
        words[0] = value.toInt()
        words[1] = (value ushr 32).toInt()
        size = if (words[1] != 0) 2 else 1
    }

    fun set(other: BigUInt) {
        ensureCapacity(other.size)
        other.words.copyInto(words, 0, 0, other.size)
        size = other.size
    }

    /** `this = this * factor + addend`, for `0 < factor <= 2^31 - 1` and `0 <= addend <= 2^31 - 1`. */
    fun mulAdd(factor: Int, addend: Int) {
        if (size == 0) {
            if (addend != 0) set(addend.toLong())
            return
        }
        val f = factor.toLong()
        var carry = addend.toLong()
        for (i in 0 until size) {
            // Each step stays below 2^63: (2^32 - 1) * (2^31 - 1) + (2^31 - 1) < 2^63.
            val p = (words[i].toLong() and 0xFFFFFFFFL) * f + carry
            words[i] = p.toInt()
            carry = p ushr 32
        }
        if (carry != 0L) {
            ensureCapacity(size + 1)
            words[size] = carry.toInt()
            size++
        }
    }

    fun mul(other: BigUInt) {
        val an = size
        val bn = other.size
        if (an == 0 || bn == 0) {
            size = 0
            return
        }
        val n = an + bn
        if (product.size < n) product = IntArray(n) else product.fill(0, 0, n)
        val a = words
        val b = other.words
        for (i in 0 until an) {
            val ai = a[i].toLong() and 0xFFFFFFFFL
            if (ai == 0L) continue
            var carry = 0L
            for (j in 0 until bn) {
                // The sum can reach 2^64 - 1, so it is read back as an unsigned 64-bit pattern:
                // `toInt()` keeps the low word and `ushr 32` the carry.
                val p = ai * (b[j].toLong() and 0xFFFFFFFFL) +
                    (product[i + j].toLong() and 0xFFFFFFFFL) + carry
                product[i + j] = p.toInt()
                carry = p ushr 32
            }
            var k = i + bn
            while (carry != 0L) {
                val p = (product[k].toLong() and 0xFFFFFFFFL) + carry
                product[k] = p.toInt()
                carry = p ushr 32
                k++
            }
        }
        val spent = words
        words = product
        product = spent
        size = n
        trim()
    }

    fun shiftLeft(bits: Int) {
        if (size == 0 || bits == 0) return
        val wordShift = bits ushr 5
        val bitShift = bits and 31
        val newSize = size + wordShift + 1
        ensureCapacity(newSize)
        if (bitShift == 0) {
            for (i in size - 1 downTo 0) words[i + wordShift] = words[i]
            words[size + wordShift] = 0
        } else {
            words[size + wordShift] = words[size - 1] ushr (32 - bitShift)
            for (i in size - 1 downTo 1) {
                words[i + wordShift] = (words[i] shl bitShift) or (words[i - 1] ushr (32 - bitShift))
            }
            words[wordShift] = words[0] shl bitShift
        }
        for (i in 0 until wordShift) words[i] = 0
        size = newSize
        trim()
    }

    fun compareTo(other: BigUInt): Int {
        if (size != other.size) return if (size < other.size) -1 else 1
        for (i in size - 1 downTo 0) {
            val a = words[i]
            val b = other.words[i]
            if (a != b) {
                return if ((a.toLong() and 0xFFFFFFFFL) < (b.toLong() and 0xFFFFFFFFL)) -1 else 1
            }
        }
        return 0
    }

    private fun trim() {
        while (size > 0 && words[size - 1] == 0) size--
    }

    private fun ensureCapacity(n: Int) {
        if (words.size >= n) return
        var capacity = words.size
        while (capacity < n) capacity *= 2
        words = words.copyOf(capacity)
    }
}

/** 5^0 .. 5^13; 5^13 is the largest power of five below 2^31. */
private val SMALL_POWERS_OF_FIVE = intArrayOf(
    1, 5, 25, 125, 625, 3125, 15625, 78125, 390625, 1953125, 9765625, 48828125, 244140625,
    1220703125,
)

/** Returns 5^[n] for `n >= 0`. */
internal fun bigPowerOfFive(n: Int): BigUInt {
    // 5^n needs n * log2(5) / 32 words; the +2 covers the rounding and the two-word minimum.
    val result = BigUInt(n * 3 / 40 + 2)
    result.set(1L)
    var remaining = n
    while (remaining >= 13) {
        result.mulAdd(SMALL_POWERS_OF_FIVE[13], 0)
        remaining -= 13
    }
    if (remaining > 0) result.mulAdd(SMALL_POWERS_OF_FIVE[remaining], 0)
    return result
}
