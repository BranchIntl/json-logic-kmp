package co.branch.jsonlogic.evaluator.expressions

import co.branch.jsonlogic.ast.JsonLogicArray
import co.branch.jsonlogic.evaluator.JsonLogicEvaluationException
import co.branch.jsonlogic.evaluator.JsonLogicEvaluator
import co.branch.jsonlogic.evaluator.JsonLogicExpression
import co.branch.jsonlogic.internal.truthy

/**
 * `and` and `or`: evaluates its arguments left to right, one at a time, stopping and returning the
 * deciding argument's own value as soon as the outcome is settled — `and` stops at the first falsy
 * value, `or` at the first truthy one — rather than coercing to a [Boolean]. Requires at least one
 * argument. When every argument runs without deciding the outcome, the last one's value is returned.
 */
class LogicExpression private constructor(private val isAnd: Boolean) : JsonLogicExpression {

    override val key: String = if (isAnd) "and" else "or"

    override fun evaluate(
        evaluator: JsonLogicEvaluator,
        arguments: JsonLogicArray,
        data: Any?,
        jsonPath: String,
    ): Any? {
        val elements = arguments.elements

        if (elements.isEmpty()) {
            throw JsonLogicEvaluationException("$key operator expects at least 1 argument", jsonPath)
        }

        var result: Any? = null

        for ((index, element) in elements.withIndex()) {
            result = evaluator.evaluate(element, data, "$jsonPath[$index]")

            val decided = if (isAnd) !truthy(result) else truthy(result)
            if (decided) {
                return result
            }
        }

        return result
    }

    companion object {
        val AND = LogicExpression(true)
        val OR = LogicExpression(false)
    }
}
