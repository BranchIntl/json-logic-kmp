package co.branch.jsonlogic.evaluator.expressions

import co.branch.jsonlogic.ast.JsonLogicArray
import co.branch.jsonlogic.evaluator.JsonLogicEvaluator
import co.branch.jsonlogic.evaluator.JsonLogicExpression

/** `!=`, the negation of [EqualityExpression] down to its argument count error. */
class InequalityExpression private constructor(private val delegate: EqualityExpression) : JsonLogicExpression {

    override val key: String = "!="

    override fun evaluate(
        evaluator: JsonLogicEvaluator,
        arguments: JsonLogicArray,
        data: Any?,
        jsonPath: String,
    ): Any? {
        val result = delegate.evaluate(evaluator, arguments, data, jsonPath) as Boolean

        return !result
    }

    companion object {
        val INSTANCE = InequalityExpression(EqualityExpression.INSTANCE)
    }
}
