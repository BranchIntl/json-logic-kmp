package co.branch.jsonlogic.internal

/*
 * Renderings of a domain value as text, one per operator family that needs one.
 *
 * The two differ only in how they render a number. Both render a `List` and a `Map` the way
 * `java.util.AbstractCollection` and `java.util.AbstractMap` do — `[a, b]` and `{k=v}` — where the
 * JsonLogic reference implementation would give `a,b` and `[object Object]`.
 */

/**
 * Renders [value] the way Java's string concatenation (`"" + value`, which routes through
 * `String.valueOf`) would, numbers included: `null` becomes the literal `"null"` and a [Double] goes
 * through [canonicalDoubleToString].
 *
 * Used by `log`, whose output is diagnostic text rather than a value any rule can observe.
 */
internal fun javaStringify(value: Any?): String = when (value) {
    null -> "null"
    is Double -> canonicalDoubleToString(value)
    is Boolean -> value.toString()
    is String -> value
    is List<*> -> value.joinToString(prefix = "[", postfix = "]", separator = ", ") { javaStringify(it) }
    is Map<*, *> -> value.entries.joinToString(prefix = "{", postfix = "}", separator = ", ") {
        "${javaStringify(it.key)}=${javaStringify(it.value)}"
    }
    else -> value.toString()
}

/**
 * Renders [value] the way ECMAScript's `String(value)` would: `null` becomes the literal `"null"`
 * and a number goes through [ecmaDoubleToString], so a whole one carries no decimal point.
 *
 * Used by `cat` and `substr`, whose results are values a rule can go on to compare. `cat` renders a
 * null argument as the empty string instead of calling this, matching the reference's use of
 * `Array.prototype.join`.
 */
internal fun ecmaStringify(value: Any?): String = when (value) {
    null -> "null"
    is Double -> ecmaDoubleToString(value)
    is Boolean -> value.toString()
    is String -> value
    is List<*> -> value.joinToString(prefix = "[", postfix = "]", separator = ", ") { ecmaStringify(it) }
    is Map<*, *> -> value.entries.joinToString(prefix = "{", postfix = "}", separator = ", ") {
        "${ecmaStringify(it.key)}=${ecmaStringify(it.value)}"
    }
    else -> value.toString()
}
