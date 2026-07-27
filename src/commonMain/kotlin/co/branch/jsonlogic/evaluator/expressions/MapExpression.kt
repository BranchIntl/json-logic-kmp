package co.branch.jsonlogic.evaluator.expressions

import co.branch.jsonlogic.ast.JsonLogicArray
import co.branch.jsonlogic.evaluator.ArrayLike
import co.branch.jsonlogic.evaluator.JsonLogicEvaluationException
import co.branch.jsonlogic.evaluator.JsonLogicEvaluator
import co.branch.jsonlogic.evaluator.JsonLogicExpression

/**
 * `map`: evaluates its second argument once per element of its first, and collects the results.
 *
 * `{"map": [{"var": "integers"}, {"*": [{"var": ""}, 2]}]}` doubles every entry of `integers`: the
 * element itself is the data the sub-rule sees, so `{"var": ""}` is the element and `{"var": "qty"}`
 * is the element's `qty` field.
 *
 * A first argument that is not list-like — a number, a string, a map, null — yields an empty list
 * rather than an error.
 */
class MapExpression private constructor() : JsonLogicExpression {

    override val key: String = "map"

    override fun evaluate(
        evaluator: JsonLogicEvaluator,
        arguments: JsonLogicArray,
        data: Any?,
        jsonPath: String,
    ): Any? {
        if (arguments.elements.size != 2) {
            throw JsonLogicEvaluationException("map expects exactly 2 arguments", jsonPath)
        }

        val maybeArray = evaluator.evaluate(arguments.elements[0], data, "$jsonPath[0]")

        if (!ArrayLike.isEligible(maybeArray)) {
            return emptyList<Any?>()
        }

        return ArrayLike.of(maybeArray).map { item ->
            evaluator.evaluate(arguments.elements[1], item, "$jsonPath[1]")
        }
    }

    companion object {
        val INSTANCE = MapExpression()
    }
}
