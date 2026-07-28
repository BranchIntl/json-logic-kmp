package co.branch.jsonlogic.evaluator.expressions

import co.branch.jsonlogic.internal.truthy

/**
 * `!` and `!!`: negates (`!`) or reflects (`!!`) the truthiness of its sole argument, treating a
 * missing argument as falsy.
 */
class NotExpression private constructor(private val isDoubleBang: Boolean) : PreEvaluatedArgumentsExpression {

    override val key: String = if (isDoubleBang) "!!" else "!"

    override fun evaluate(arguments: List<Any?>, data: Any?, jsonPath: String): Any? {
        val result = if (arguments.isEmpty()) false else truthy(arguments[0])

        return if (isDoubleBang) result else !result
    }

    companion object {
        val SINGLE = NotExpression(false)
        val DOUBLE = NotExpression(true)
    }
}
