package co.branch.jsonlogic.evaluator

import co.branch.jsonlogic.ast.JsonLogicArray
import co.branch.jsonlogic.evaluator.expressions.ConcatenateExpression
import co.branch.jsonlogic.evaluator.expressions.IfExpression
import co.branch.jsonlogic.evaluator.expressions.LogExpression
import co.branch.jsonlogic.evaluator.expressions.LogicExpression
import co.branch.jsonlogic.evaluator.expressions.MissingExpression
import co.branch.jsonlogic.evaluator.expressions.NotExpression
import co.branch.jsonlogic.evaluator.expressions.SubstringExpression

/** This workstream's own operators, plus [MissingExpression] since several fixtures pair it with `if`. */
internal val controlStringExpressions: List<JsonLogicExpression> = listOf(
    IfExpression.IF,
    IfExpression.TERNARY,
    LogicExpression.AND,
    LogicExpression.OR,
    NotExpression.SINGLE,
    NotExpression.DOUBLE,
    LogExpression.STDOUT,
    ConcatenateExpression.INSTANCE,
    SubstringExpression.INSTANCE,
    MissingExpression.ALL,
    MissingExpression.SOME,
)

/**
 * Records how many times it was evaluated and returns [value] unchanged, so a test can prove a
 * lazy operation such as `if`, `and`, or `or` never evaluated an argument it did not need.
 */
internal class CountingExpression(override val key: String, private val value: Any?) : JsonLogicExpression {
    var evaluationCount = 0
        private set

    override fun evaluate(
        evaluator: JsonLogicEvaluator,
        arguments: JsonLogicArray,
        data: Any?,
        jsonPath: String,
    ): Any? {
        evaluationCount++
        return value
    }
}
