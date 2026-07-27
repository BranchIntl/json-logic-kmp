package co.branch.jsonlogic.evaluator.expressions

import co.branch.jsonlogic.evaluator.ArrayLike

/**
 * `merge`: concatenates its arguments into one list, in order.
 *
 * A list-like argument contributes its own elements and anything else contributes itself, so
 * `{"merge": [1, [2, 3]]}` is `[1, 2, 3]`. Only one level is flattened: `{"merge": [[[1]], [2]]}` is
 * `[[1], 2]`.
 */
class MergeExpression private constructor() : PreEvaluatedArgumentsExpression {

    override val key: String = "merge"

    override fun evaluate(arguments: List<Any?>, data: Any?, jsonPath: String): Any? =
        arguments.flatMap { argument ->
            if (ArrayLike.isEligible(argument)) ArrayLike.of(argument) else listOf(argument)
        }

    companion object {
        val INSTANCE = MergeExpression()
    }
}
