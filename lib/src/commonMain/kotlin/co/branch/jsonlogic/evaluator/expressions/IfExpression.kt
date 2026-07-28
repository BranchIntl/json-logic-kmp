package co.branch.jsonlogic.evaluator.expressions

import co.branch.jsonlogic.ast.JsonLogicArray
import co.branch.jsonlogic.evaluator.JsonLogicEvaluator
import co.branch.jsonlogic.evaluator.JsonLogicExpression
import co.branch.jsonlogic.internal.truthy

/**
 * `if` and `?:`: evaluates its arguments lazily, one at a time, so an untaken branch never runs —
 * this is why it implements [JsonLogicExpression] directly rather than
 * [PreEvaluatedArgumentsExpression].
 *
 * With one argument, that argument is the result. With two, the second is the result when the first
 * is truthy and the result is otherwise null. With three or more, arguments are read as
 * `condition, result, condition, result, ..., [else]`: the first pair whose condition is truthy wins,
 * a trailing unpaired argument is the `else` taken when every condition was falsy, and with no
 * trailing argument the result is null when every condition was falsy.
 */
class IfExpression private constructor(override val key: String) : JsonLogicExpression {

    override fun evaluate(
        evaluator: JsonLogicEvaluator,
        arguments: JsonLogicArray,
        data: Any?,
        jsonPath: String,
    ): Any? {
        val elements = arguments.elements

        if (elements.isEmpty()) {
            return null
        }

        if (elements.size == 1) {
            return evaluator.evaluate(elements[0], data, "$jsonPath[0]")
        }

        if (elements.size == 2) {
            return if (truthy(evaluator.evaluate(elements[0], data, "$jsonPath[0]"))) {
                evaluator.evaluate(elements[1], data, "$jsonPath[1]")
            } else {
                null
            }
        }

        var index = 0
        while (index < elements.size - 1) {
            val condition = elements[index]
            val resultIfTrue = elements[index + 1]

            if (truthy(evaluator.evaluate(condition, data, "$jsonPath[$index]"))) {
                return evaluator.evaluate(resultIfTrue, data, "$jsonPath[${index + 1}]")
            }

            index += 2
        }

        if (elements.size % 2 == 0) {
            return null
        }

        val lastIndex = elements.size - 1
        return evaluator.evaluate(elements[lastIndex], data, "$jsonPath[$lastIndex]")
    }

    companion object {
        val IF = IfExpression("if")
        val TERNARY = IfExpression("?:")
    }
}
