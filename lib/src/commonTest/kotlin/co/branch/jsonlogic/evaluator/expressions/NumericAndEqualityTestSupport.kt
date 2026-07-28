package co.branch.jsonlogic.evaluator.expressions

import co.branch.jsonlogic.evaluator.JsonLogicExpression
import co.branch.jsonlogic.evaluator.evaluate

/**
 * The arithmetic, comparison and equality operations, plus `missing` and `missing_some` so the
 * fixture subset can run every case whose operators these cover.
 */
internal val numericAndEqualityExpressions: List<JsonLogicExpression> = listOf(
    MathExpression.ADD,
    MathExpression.SUBTRACT,
    MathExpression.MULTIPLY,
    MathExpression.DIVIDE,
    MathExpression.MODULO,
    MathExpression.MIN,
    MathExpression.MAX,
    NumericComparisonExpression.GT,
    NumericComparisonExpression.GTE,
    NumericComparisonExpression.LT,
    NumericComparisonExpression.LTE,
    EqualityExpression.INSTANCE,
    InequalityExpression.INSTANCE,
    StrictEqualityExpression.INSTANCE,
    StrictInequalityExpression.INSTANCE,
    MissingExpression.ALL,
    MissingExpression.SOME,
)

/** Evaluates [rule] against [data] with [numericAndEqualityExpressions] registered. */
internal fun evaluateRule(rule: String, data: Any? = null): Any? =
    evaluate(rule, data, numericAndEqualityExpressions)

/** The bits of [rule]'s numeric result, so that NaN and the two zeros are told apart. */
internal fun evaluateToRawBits(rule: String): Long = (evaluateRule(rule) as Double).toRawBits()

/** The JSON string literal holding [text], as an operand is written inside a rule. */
internal fun jsonString(text: String): String = "\"$text\""

/* Operands JSON has no literal for, written as the arithmetic that produces them. */
internal const val nanRule = """{"/": [0, 0]}"""
internal const val infinityRule = """{"/": [1, 0]}"""
internal const val negativeZeroRule = """{"-": [0]}"""
