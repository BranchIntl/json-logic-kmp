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
internal fun javaStringify(value: Any?): String = stringify(value, ::canonicalDoubleToString, emptyList())

/**
 * Renders [value] the way ECMAScript's `String(value)` would: `null` becomes the literal `"null"`
 * and a number goes through [ecmaDoubleToString], so a whole one carries no decimal point.
 *
 * Used by `cat`, `substr` and `in`, whose results are values a rule can go on to compare. `cat`
 * renders a null argument as the empty string instead of calling this, matching the reference's use
 * of `Array.prototype.join`.
 */
internal fun ecmaStringify(value: Any?): String = stringify(value, ::ecmaDoubleToString, emptyList())

/**
 * Renders [value] with [renderNumber] deciding the form a [Double] takes, naming rather than entering
 * any container in [enclosing] — the ones already being rendered around it.
 *
 * A value here can hold a cycle, so the containers have to be tracked: `reduce` mutates a single
 * context map in place, and a reducer returning a list built around its own data leaves that list and
 * that map holding each other. The names are java.util's, but the reach is not — java.util compares an
 * entry only against the container directly holding it, which a cycle closing through two of them
 * slips past. Only an enclosing container counts, so the same value appearing twice side by side is
 * rendered twice rather than mistaken for a cycle.
 */
private fun stringify(value: Any?, renderNumber: (Double) -> String, enclosing: List<Any>): String = when (value) {
    null -> "null"
    is Double -> renderNumber(value)
    is Boolean -> value.toString()
    is String -> value
    is List<*> -> stringifyList(value, renderNumber, enclosing)
    is Map<*, *> -> stringifyMap(value, renderNumber, enclosing)
    else -> value.toString()
}

private fun stringifyList(value: List<*>, renderNumber: (Double) -> String, enclosing: List<Any>): String {
    if (enclosing.any { it === value }) {
        return "(this Collection)"
    }

    val inside = within(enclosing, value)

    return value.joinToString(prefix = "[", postfix = "]", separator = ", ") { stringify(it, renderNumber, inside) }
}

private fun stringifyMap(value: Map<*, *>, renderNumber: (Double) -> String, enclosing: List<Any>): String {
    if (enclosing.any { it === value }) {
        return "(this Map)"
    }

    val inside = within(enclosing, value)

    return value.entries.joinToString(prefix = "{", postfix = "}", separator = ", ") {
        "${stringify(it.key, renderNumber, inside)}=${stringify(it.value, renderNumber, inside)}"
    }
}

/**
 * [enclosing] with [container] appended — through a singleton, since `enclosing + container` splices a
 * container's own elements into the chain rather than adding the container itself.
 */
private fun within(enclosing: List<Any>, container: Any): List<Any> = enclosing + listOf(container)
