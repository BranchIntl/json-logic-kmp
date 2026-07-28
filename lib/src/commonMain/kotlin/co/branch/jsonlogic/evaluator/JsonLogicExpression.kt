package co.branch.jsonlogic.evaluator

import co.branch.jsonlogic.ast.JsonLogicArray

/**
 * A single JsonLogic operation, registered under [key] and invoked whenever a rule contains an
 * object with that key.
 *
 * Arguments arrive unevaluated so an operation can decide what to evaluate and in which context —
 * `if` skips untaken branches, `map` re-evaluates its body per element. Operations that just need
 * their arguments' values should implement [co.branch.jsonlogic.evaluator.expressions.PreEvaluatedArgumentsExpression]
 * instead.
 *
 * Values passed in and returned are the evaluator's own domain: [Double], [String], [Boolean],
 * `null`, [List] and [Map].
 */
interface JsonLogicExpression {

    /** The operator this expression is registered under, e.g. `+` or `missing_some`. */
    val key: String

    /**
     * Evaluates this operation.
     *
     * [jsonPath] locates this operation inside the rule (e.g. `$.if[0].+`) and must be extended,
     * never replaced, when recursing through [evaluator], so a thrown
     * [JsonLogicEvaluationException] points at the failing sub-expression.
     */
    fun evaluate(
        evaluator: JsonLogicEvaluator,
        arguments: JsonLogicArray,
        data: Any?,
        jsonPath: String,
    ): Any?
}
