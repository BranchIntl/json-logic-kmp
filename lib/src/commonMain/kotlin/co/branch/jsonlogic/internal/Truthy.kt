package co.branch.jsonlogic.internal

/**
 * Reports whether [value] is truthy under JsonLogic's rules: null is false; a boolean is itself; a
 * number is truthy unless it is zero or NaN (an infinity is truthy); a string and a collection are
 * truthy when non-empty. Everything else — a map included, since a map is not a collection — is
 * truthy.
 */
internal fun truthy(value: Any?): Boolean {
    if (value == null) {
        return false
    }

    if (value is Boolean) {
        return value
    }

    if (value is Number) {
        if (value is Double) {
            if (value.isNaN()) {
                return false
            } else if (value.isInfinite()) {
                return true
            }
        }

        if (value is Float) {
            if (value.isNaN()) {
                return false
            } else if (value.isInfinite()) {
                return true
            }
        }

        return value.toDouble() != 0.0
    }

    if (value is String) {
        return value.isNotEmpty()
    }

    if (value is Collection<*>) {
        return value.isNotEmpty()
    }

    return true
}
