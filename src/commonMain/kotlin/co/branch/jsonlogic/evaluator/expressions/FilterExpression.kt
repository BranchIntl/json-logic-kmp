package co.branch.jsonlogic.evaluator.expressions

import co.branch.jsonlogic.ast.JsonLogicArray
import co.branch.jsonlogic.evaluator.ArrayLike
import co.branch.jsonlogic.evaluator.JsonLogicEvaluationException
import co.branch.jsonlogic.evaluator.JsonLogicEvaluator
import co.branch.jsonlogic.evaluator.JsonLogicExpression
import co.branch.jsonlogic.internal.truthy

/**
 * `filter`: keeps the elements of its first argument for which its second argument is truthy.
 *
 * `{"filter": [{"var": "integers"}, {"%": [{"var": ""}, 2]}]}` keeps the odd entries: the element
 * itself is the data the sub-rule sees. The elements that survive are returned as they were, not as
 * the sub-rule's results.
 *
 * A first argument that is not list-like is an error.
 */
class FilterExpression private constructor() : JsonLogicExpression {

    override val key: String = "filter"

    override fun evaluate(
        evaluator: JsonLogicEvaluator,
        arguments: JsonLogicArray,
        data: Any?,
        jsonPath: String,
    ): Any? {
        if (arguments.elements.size != 2) {
            throw JsonLogicEvaluationException("filter expects exactly 2 arguments", jsonPath)
        }

        val maybeArray = evaluator.evaluate(arguments.elements[0], data, "$jsonPath[0]")

        if (!ArrayLike.isEligible(maybeArray)) {
            throw JsonLogicEvaluationException("first argument to filter must be a valid array", "$jsonPath[0]")
        }

        return ArrayLike.of(maybeArray).filter { item ->
            truthy(evaluator.evaluate(arguments.elements[1], item, "$jsonPath[1]"))
        }
    }

    companion object {
        val INSTANCE = FilterExpression()
    }
}
