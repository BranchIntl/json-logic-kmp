package co.branch.jsonlogic.evaluator.expressions

import co.branch.jsonlogic.evaluator.JsonLogicEvaluationException
import co.branch.jsonlogic.internal.parseJavaDouble

/**
 * The ordering comparisons `> >= < <=`.
 *
 * Two arguments compare directly; three or more compare as a between, `a < b < c`, on the first
 * three — the rest are still evaluated, but not looked at. Fewer than two is an error, while an
 * argument that is neither a number nor a string a number can be read out of makes the comparison
 * false.
 */
class NumericComparisonExpression private constructor(override val key: String) : PreEvaluatedArgumentsExpression {

    override fun evaluate(arguments: List<Any?>, data: Any?, jsonPath: String): Any? {
        // Convert the arguments to doubles
        val n = minOf(arguments.size, 3)

        if (n < 2) {
            throw JsonLogicEvaluationException("'$key' requires at least 2 arguments", jsonPath)
        }

        val values = DoubleArray(n)

        for (i in 0 until n) {
            val value = arguments[i]

            if (value is String) {
                values[i] = parseJavaDouble(value) ?: return false
            } else if (value !is Number) {
                return false
            } else {
                values[i] = value.toDouble()
            }
        }

        // Handle between comparisons
        if (arguments.size >= 3) {
            return when (key) {
                "<" -> values[0] < values[1] && values[1] < values[2]
                "<=" -> values[0] <= values[1] && values[1] <= values[2]
                ">" -> values[0] > values[1] && values[1] > values[2]
                ">=" -> values[0] >= values[1] && values[1] >= values[2]
                else -> throw JsonLogicEvaluationException("'$key' does not support between comparisons", jsonPath)
            }
        }

        // Handle regular comparisons
        return when (key) {
            "<" -> values[0] < values[1]
            "<=" -> values[0] <= values[1]
            ">" -> values[0] > values[1]
            ">=" -> values[0] >= values[1]
            else -> throw JsonLogicEvaluationException("'$key' is not a comparison expression", jsonPath)
        }
    }

    companion object {
        val GT = NumericComparisonExpression(">")
        val GTE = NumericComparisonExpression(">=")
        val LT = NumericComparisonExpression("<")
        val LTE = NumericComparisonExpression("<=")
    }
}
