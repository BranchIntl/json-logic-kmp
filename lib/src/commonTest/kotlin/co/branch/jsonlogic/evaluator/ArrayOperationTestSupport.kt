package co.branch.jsonlogic.evaluator

import co.branch.jsonlogic.evaluator.expressions.AllExpression
import co.branch.jsonlogic.evaluator.expressions.ArrayHasExpression
import co.branch.jsonlogic.evaluator.expressions.FilterExpression
import co.branch.jsonlogic.evaluator.expressions.InExpression
import co.branch.jsonlogic.evaluator.expressions.MapExpression
import co.branch.jsonlogic.evaluator.expressions.MergeExpression
import co.branch.jsonlogic.evaluator.expressions.MissingExpression
import co.branch.jsonlogic.evaluator.expressions.ReduceExpression

/**
 * The array operations plus `missing`/`missing_some`, which the array tests lean on: a sub-rule that
 * fails for one element and not another needs an operator that rejects some values, and
 * `missing_some` is the only one registered here that does.
 */
internal val arrayExpressions: List<JsonLogicExpression> = listOf(
    MapExpression.INSTANCE,
    FilterExpression.INSTANCE,
    ReduceExpression.INSTANCE,
    AllExpression.INSTANCE,
    ArrayHasExpression.SOME,
    ArrayHasExpression.NONE,
    MergeExpression.INSTANCE,
    InExpression.INSTANCE,
    MissingExpression.ALL,
    MissingExpression.SOME,
)

/** Evaluates [rule] against [data] with every array operation registered. */
internal fun evaluateArrayOp(rule: String, data: Any? = null): Any? = evaluate(rule, data, arrayExpressions)

/**
 * A sub-rule that is truthy when its element is a number — it reports `["a"]` as missing — and throws
 * for every other element. Threading it through an operation shows which elements were reached, and
 * at which jsonPath.
 */
internal const val NUMBERS_ONLY = """{"missing_some": [{"var": ""}, ["a"]]}"""

/** The message [NUMBERS_ONLY] fails with. */
internal const val NUMBERS_ONLY_MESSAGE =
    "missing_some expects first argument to be an integer and the second argument to be an array"
