package co.branch.jsonlogic.evaluator.expressions

import co.branch.jsonlogic.ast.JsonLogicArray
import co.branch.jsonlogic.evaluator.ArrayLike
import co.branch.jsonlogic.evaluator.JsonLogicEvaluationException
import co.branch.jsonlogic.evaluator.JsonLogicEvaluator
import co.branch.jsonlogic.evaluator.JsonLogicExpression
import co.branch.jsonlogic.internal.truthy

/**
 * `all`: reports whether its second argument is truthy for every element of its first, stopping at
 * the first element for which it is not.
 *
 * `{"all": [{"var": "integers"}, {">=": [{"var": ""}, 1]}]}` asks whether every entry is at least 1:
 * the element itself is the data the sub-rule sees. An empty array is false, and so is a null first
 * argument — which is what an absent `var` resolves to.
 *
 * A first argument that is neither null nor list-like is an error.
 */
class AllExpression private constructor() : JsonLogicExpression {

    override val key: String = "all"

    override fun evaluate(
        evaluator: JsonLogicEvaluator,
        arguments: JsonLogicArray,
        data: Any?,
        jsonPath: String,
    ): Any? {
        if (arguments.elements.size != 2) {
            throw JsonLogicEvaluationException("all expects exactly 2 arguments", jsonPath)
        }

        val maybeArray = evaluator.evaluate(arguments.elements[0], data, "$jsonPath[0]")

        if (maybeArray == null) {
            return false
        }

        if (!ArrayLike.isEligible(maybeArray)) {
            throw JsonLogicEvaluationException("first argument to all must be a valid array", jsonPath)
        }

        val array = ArrayLike.of(maybeArray)

        if (array.isEmpty()) {
            return false
        }

        // Every element's sub-evaluation reports the same path index, 1, so an error raised on the
        // third element still reports "[1]": the engine this library ports never advances the index it
        // declares here, and its error paths are part of the contract.
        for (item in array) {
            if (!truthy(evaluator.evaluate(arguments.elements[1], item, "$jsonPath[1]"))) {
                return false
            }
        }

        return true
    }

    companion object {
        val INSTANCE = AllExpression()
    }
}
