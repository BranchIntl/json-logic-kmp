package co.branch.jsonlogic.evaluator.expressions

import co.branch.jsonlogic.ast.JsonLogicArray
import co.branch.jsonlogic.evaluator.ArrayLike
import co.branch.jsonlogic.evaluator.JsonLogicEvaluationException
import co.branch.jsonlogic.evaluator.JsonLogicEvaluator
import co.branch.jsonlogic.evaluator.JsonLogicExpression

/**
 * `reduce`: folds its first argument into a single value with its second, starting from its third.
 *
 * `{"reduce": [{"var": "integers"}, {"+": [{"var": "current"}, {"var": "accumulator"}]}, 0]}` sums
 * `integers`. The reducer's data is a map with exactly the keys `current` (the element) and
 * `accumulator` (the fold so far), so the outer data is not reachable from it, while the initial
 * accumulator — evaluated up front, whether or not the fold runs — is evaluated against that outer
 * data.
 *
 * The same map is handed to every iteration and updated in place, so a reducer that returns its own
 * data returns a map that holds itself under `accumulator`.
 *
 * A first argument that is not list-like yields the initial accumulator.
 */
class ReduceExpression private constructor() : JsonLogicExpression {

    override val key: String = "reduce"

    override fun evaluate(
        evaluator: JsonLogicEvaluator,
        arguments: JsonLogicArray,
        data: Any?,
        jsonPath: String,
    ): Any? {
        if (arguments.elements.size != 3) {
            throw JsonLogicEvaluationException("reduce expects exactly 3 arguments", jsonPath)
        }

        val maybeArray = evaluator.evaluate(arguments.elements[0], data, "$jsonPath[0]")
        val accumulator = evaluator.evaluate(arguments.elements[2], data, "$jsonPath[2]")

        if (!ArrayLike.isEligible(maybeArray)) {
            return accumulator
        }

        val context = mutableMapOf<String, Any?>("accumulator" to accumulator)

        for (item in ArrayLike.of(maybeArray)) {
            context["current"] = item
            context["accumulator"] = evaluator.evaluate(arguments.elements[1], context, "$jsonPath[1]")
        }

        return context["accumulator"]
    }

    companion object {
        val INSTANCE = ReduceExpression()
    }
}
