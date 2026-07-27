package co.branch.jsonlogic.evaluator.expressions

import co.branch.jsonlogic.internal.canonicalDoubleToString
import co.branch.jsonlogic.internal.javaStringify

/**
 * `cat`: joins every argument's rendering into one string, with no separator.
 *
 * A [Double] that is a whole number renders as its integer part (`1.0` and `-2.0` join in as `1` and
 * `-2`) rather than the full decimal — matching upstream, which special-cases any `Double` whose own
 * `toString` ends in `.0` and prints its `intValue()` instead. Only whole-number magnitudes below
 * `1e7` take this path: past that, [canonicalDoubleToString] switches to scientific notation (e.g.
 * `1.0E7`), which does not end in `.0`. Every other value renders through [javaStringify].
 */
class ConcatenateExpression private constructor() : PreEvaluatedArgumentsExpression {

    override val key: String = "cat"

    override fun evaluate(arguments: List<Any?>, data: Any?, jsonPath: String): Any? =
        arguments.joinToString(separator = "") { stringify(it) }

    private fun stringify(value: Any?): String {
        if (value is Double) {
            val rendered = canonicalDoubleToString(value)
            return if (rendered.endsWith(".0")) value.toInt().toString() else rendered
        }

        return javaStringify(value)
    }

    companion object {
        val INSTANCE = ConcatenateExpression()
    }
}
