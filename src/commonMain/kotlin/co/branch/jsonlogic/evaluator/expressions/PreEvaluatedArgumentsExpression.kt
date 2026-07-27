package co.branch.jsonlogic.evaluator.expressions

import co.branch.jsonlogic.ast.JsonLogicArray
import co.branch.jsonlogic.evaluator.ArrayLike
import co.branch.jsonlogic.evaluator.JsonLogicEvaluator
import co.branch.jsonlogic.evaluator.JsonLogicExpression

/**
 * A [JsonLogicExpression] that only needs its arguments' values: they are evaluated up front, left
 * to right, and handed over as a list.
 *
 * A lone argument that is itself list-like is unwrapped, so `{"op": [[1, 2]]}` and `{"op": [1, 2]}`
 * reach [evaluate] identically. Unwrapping also normalizes the elements' numbers to [Double].
 */
interface PreEvaluatedArgumentsExpression : JsonLogicExpression {

    fun evaluate(arguments: List<Any?>, data: Any?, jsonPath: String): Any?

    override fun evaluate(
        evaluator: JsonLogicEvaluator,
        arguments: JsonLogicArray,
        data: Any?,
        jsonPath: String,
    ): Any? {
        var values: List<Any?> = evaluator.evaluate(arguments, data, jsonPath)

        if (values.size == 1 && ArrayLike.isEligible(values[0])) {
            values = ArrayLike.of(values[0])
        }

        return evaluate(values, data, jsonPath)
    }
}
