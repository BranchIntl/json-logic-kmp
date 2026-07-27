package co.branch.jsonlogic.evaluator.expressions

import co.branch.jsonlogic.evaluator.ArrayLike
import co.branch.jsonlogic.internal.canonicalDoubleToString

/**
 * `in`: whether the first argument is contained in the second.
 *
 * A string second argument makes this a substring test — `{"in": ["Spring", "Springfield"]}` is true
 * — against the first argument's rendering, so a number matches the digits of its `Double` form:
 * `{"in": [1, "a1.0b"]}` is true. A list-like second argument makes it a membership test, by value
 * and to any depth, so a list can be found inside a list of lists.
 *
 * Membership neither coerces nor compares numbers numerically: `1` is not found in `["1.0"]`, NaN is
 * found in `[NaN]`, and `-0.0` is not found in `[0.0]`. A second argument that is neither a string nor
 * list-like, or fewer than two arguments, is false rather than an error.
 */
class InExpression private constructor() : PreEvaluatedArgumentsExpression {

    override val key: String = "in"

    override fun evaluate(arguments: List<Any?>, data: Any?, jsonPath: String): Any? {
        if (arguments.size < 2) {
            return false
        }

        val needle = arguments[0]
        val haystack = arguments[1]

        if (haystack is String) {
            if (needle == null) {
                return false
            }

            return haystack.contains(javaToString(needle))
        }

        if (ArrayLike.isEligible(haystack)) {
            return ArrayLike.of(haystack).contains(needle)
        }

        return false
    }

    companion object {
        val INSTANCE = InExpression()
    }
}

/**
 * Renders [value] the way the JVM renders the engine's value domain: a number through Java's own
 * `Double.toString`, a collection as `[a, b]`, a map as `{k=v}`, and each of their entries the same
 * way again. Kotlin's `Double.toString` is documented as platform-dependent, so it cannot stand in.
 *
 * A map that holds itself renders that entry as `(this Map)`, as java.util does — `reduce` hands its
 * reducer a context map that can end up its own accumulator, and a value from there can reach here.
 */
private fun javaToString(value: Any?): String = when (value) {
    is Double -> canonicalDoubleToString(value)
    is Collection<*> -> value.joinToString(", ", "[", "]") { element -> javaToString(element) }
    is Map<*, *> -> value.entries.joinToString(", ", "{", "}") { (entryKey, entryValue) ->
        val rendered = if (entryValue === value) "(this Map)" else javaToString(entryValue)

        "${javaToString(entryKey)}=$rendered"
    }
    else -> value.toString()
}
