package co.branch.jsonlogic.evaluator.expressions

import co.branch.jsonlogic.evaluator.JsonLogicEvaluationException

/**
 * `===`, comparing its two arguments without any coercion: values of different kinds are never
 * equal, two numbers compare as primitives, and everything else compares by equality of contents,
 * so two lists holding equal elements are equal.
 */
class StrictEqualityExpression private constructor() : PreEvaluatedArgumentsExpression {

    override val key: String = "==="

    override fun evaluate(arguments: List<Any?>, data: Any?, jsonPath: String): Any? {
        if (arguments.size != 2) {
            throw JsonLogicEvaluationException("equality expressions expect exactly 2 arguments", jsonPath)
        }

        val left = arguments[0]
        val right = arguments[1]

        if (left is Number && right is Number) {
            // Two numbers compare as IEEE primitives: NaN is not even equal to itself, and 0.0 and
            // -0.0 are equal.
            return left.toDouble() == right.toDouble()
        }

        if (left === right) {
            return true
        }

        return left != null && left == right
    }

    companion object {
        val INSTANCE = StrictEqualityExpression()
    }
}
