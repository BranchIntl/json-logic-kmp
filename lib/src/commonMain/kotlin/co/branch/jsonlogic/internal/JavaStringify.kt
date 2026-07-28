package co.branch.jsonlogic.internal

/**
 * Renders a domain value the way Java's string concatenation (`"" + value`, which routes through
 * `String.valueOf`) would: `null` becomes the literal `"null"`, a [Double] goes through
 * [canonicalDoubleToString] rather than the platform's own `Double.toString`, and a [List] or [Map]
 * is rendered the way `java.util.AbstractCollection` and `java.util.AbstractMap` render themselves —
 * `[a, b]` and `{k=v}` — with every element or entry rendered by the same rule, recursively.
 *
 * Shared by every expression that stringifies an operand the way `cat` and `log` do (`substr`'s own
 * first-argument rendering also matches this once the null case, which upstream instead lets crash,
 * is handled separately).
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
