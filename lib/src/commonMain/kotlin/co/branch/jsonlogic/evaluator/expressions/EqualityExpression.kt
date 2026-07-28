package co.branch.jsonlogic.evaluator.expressions

import co.branch.jsonlogic.evaluator.JsonLogicEvaluationException
import co.branch.jsonlogic.internal.parseJavaDouble
import co.branch.jsonlogic.internal.truthy

/**
 * `==`, comparing its two arguments under JavaScript's loose equality matrix: a string is read as a
 * number to meet a number, a boolean meets a number as 1 or 0 and a string as that string's
 * truthiness, and null is equal to nothing but null.
 *
 * Two operands the matrix has no row for — a list or a map on either side — are equal when both are
 * falsy, which is why two equal lists are not equal to each other unless both are empty.
 */
class EqualityExpression private constructor() : PreEvaluatedArgumentsExpression {

    override val key: String = "=="

    override fun evaluate(arguments: List<Any?>, data: Any?, jsonPath: String): Any? {
        if (arguments.size != 2) {
            throw JsonLogicEvaluationException("equality expressions expect exactly 2 arguments", jsonPath)
        }

        val left = arguments[0]
        val right = arguments[1]

        if (left == null && right == null) {
            return true
        }

        if (left == null || right == null) {
            return false
        }

        // Check numeric loose equality
        if (left is Number && right is Number) {
            // Two numbers meet as boxed values, so they are compared by bits: NaN equals itself and
            // 0.0 does not equal -0.0. Every other row below compares as primitives.
            return left.toDouble().toBits() == right.toDouble().toBits()
        }

        if (left is Number && right is String) {
            return compareNumberToString(left, right)
        }

        if (left is Number && right is Boolean) {
            return compareNumberToBoolean(left, right)
        }

        // Check string loose equality
        if (left is String && right is String) {
            return left == right
        }

        if (left is String && right is Number) {
            return compareNumberToString(right, left)
        }

        if (left is String && right is Boolean) {
            return compareStringToBoolean(left, right)
        }

        // Check boolean loose equality
        if (left is Boolean && right is Boolean) {
            return left == right
        }

        if (left is Boolean && right is Number) {
            return compareNumberToBoolean(right, left)
        }

        if (left is Boolean && right is String) {
            return compareStringToBoolean(right, left)
        }

        // Check non-truthy values
        return !truthy(left) && !truthy(right)
    }

    private fun compareNumberToString(left: Number, right: String): Boolean {
        // A blank string stands for zero, blank being Java's trim(): every character at or below
        // ' ', so a string of control characters is blank and one of non-breaking spaces is not.
        val text = if (right.all { it <= ' ' }) "0" else right
        val parsed = parseJavaDouble(text) ?: return false

        return parsed == left.toDouble()
    }

    private fun compareNumberToBoolean(left: Number, right: Boolean): Boolean {
        if (right) {
            return left.toDouble() == 1.0
        }

        return left.toDouble() == 0.0
    }

    private fun compareStringToBoolean(left: String, right: Boolean): Boolean = truthy(left) == right

    companion object {
        val INSTANCE = EqualityExpression()
    }
}
