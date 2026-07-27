package co.branch.jsonlogic.evaluator

/*
 * The two shape tests the expressions dispatch on. The engine this library ports admits Java arrays
 * and Gson trees here as well, through adapters; every value in this port has already crossed the
 * JsonElement boundary, so list-like means exactly Iterable and map-like means exactly Map.
 */

/** The list-like half of the value domain: `[]` access, `size`, and iteration. */
internal object ArrayLike {

    fun isEligible(value: Any?): Boolean = value is Iterable<*>

    /**
     * Snapshots [value] as a list with every element normalized through
     * [JsonLogicEvaluator.transform], so numbers read out of it are always [Double].
     */
    fun of(value: Any?): List<Any?> = when (value) {
        is Iterable<*> -> value.map { JsonLogicEvaluator.transform(it) }
        else -> throw IllegalArgumentException("ArrayLike only works with iterables")
    }
}

/** The map-like half of the value domain: keyed lookup and key iteration. */
internal object MapLike {

    fun isEligible(value: Any?): Boolean = value is Map<*, *>

    @Suppress("UNCHECKED_CAST")
    fun of(value: Any?): Map<Any?, Any?> = when (value) {
        is Map<*, *> -> value as Map<Any?, Any?>
        else -> throw IllegalArgumentException("MapLike only works with maps")
    }
}
