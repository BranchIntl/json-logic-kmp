package co.branch.jsonlogic.evaluator.expressions

import co.branch.jsonlogic.ast.JsonLogicArray
import co.branch.jsonlogic.evaluator.ArrayLike
import co.branch.jsonlogic.evaluator.JsonLogicEvaluationException
import co.branch.jsonlogic.evaluator.JsonLogicEvaluator
import co.branch.jsonlogic.evaluator.JsonLogicExpression
import co.branch.jsonlogic.internal.truthy

/**
 * `some` and `none`: whether its second argument is truthy for at least one element of its first, or
 * for none of them. Both stop at the first element for which it is truthy.
 *
 * `{"some": [{"var": "items"}, {">=": [{"var": "qty"}, 1]}]}` asks whether any item has a `qty` of at
 * least 1: the element itself is the data the sub-rule sees. An empty array satisfies `none` and not
 * `some`, and a null first argument — which is what an absent `var` resolves to — is read as an empty
 * one.
 *
 * A first argument that is neither null nor list-like is an error.
 */
class ArrayHasExpression private constructor(private val isSome: Boolean) : JsonLogicExpression {

    override val key: String = if (isSome) "some" else "none"

    override fun evaluate(
        evaluator: JsonLogicEvaluator,
        arguments: JsonLogicArray,
        data: Any?,
        jsonPath: String,
    ): Any? {
        if (arguments.elements.size != 2) {
            throw JsonLogicEvaluationException("$key expects exactly 2 arguments", jsonPath)
        }

        val maybeArray = evaluator.evaluate(arguments.elements[0], data, "$jsonPath[0]")

        if (maybeArray == null) {
            return !isSome
        }

        if (!ArrayLike.isEligible(maybeArray)) {
            throw JsonLogicEvaluationException("first argument to $key must be a valid array", "$jsonPath[0]")
        }

        for (item in ArrayLike.of(maybeArray)) {
            if (truthy(evaluator.evaluate(arguments.elements[1], item, "$jsonPath[1]"))) {
                return isSome
            }
        }

        return !isSome
    }

    companion object {
        val SOME = ArrayHasExpression(true)
        val NONE = ArrayHasExpression(false)
    }
}
