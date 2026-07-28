package co.branch.jsonlogic.evaluator.expressions

import co.branch.jsonlogic.internal.ecmaStringify

/**
 * `cat`: joins every argument's rendering into one string, with no separator.
 *
 * A null argument contributes nothing rather than the text `"null"`, and never fails: the reference
 * implementation joins its arguments with `Array.prototype.join`, which renders null as the empty
 * string. Every other value renders through [ecmaStringify], so a whole number joins in without a
 * decimal point — `1` and `-2` rather than `1.0` and `-2.0`.
 */
class ConcatenateExpression private constructor() : PreEvaluatedArgumentsExpression {

    override val key: String = "cat"

    override fun evaluate(arguments: List<Any?>, data: Any?, jsonPath: String): Any? =
        arguments.joinToString(separator = "") { if (it == null) "" else ecmaStringify(it) }

    companion object {
        val INSTANCE = ConcatenateExpression()
    }
}
