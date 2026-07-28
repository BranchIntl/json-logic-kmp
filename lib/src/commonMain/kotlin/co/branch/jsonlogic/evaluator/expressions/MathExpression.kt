package co.branch.jsonlogic.evaluator.expressions

import co.branch.jsonlogic.evaluator.ArrayLike
import co.branch.jsonlogic.internal.parseJavaDouble

/**
 * The arithmetic operations `+ - * / % min max`, each reducing its arguments left to right with
 * [reducer].
 *
 * `-` and `/` stop after two arguments; the others fold every argument they are given. A sole
 * argument is returned as it is, except that `-` negates it and `/` yields null. Any argument that
 * is neither a number nor a string a number can be read out of makes the whole operation null, and
 * so does no argument at all.
 *
 * `+` and `*` additionally accept a list where a number is expected, taking its first element, and
 * repeating that as long as the element is itself a list. An empty list has no first element, so it
 * makes the operation null.
 */
class MathExpression(
    override val key: String,
    private val reducer: (Double, Double) -> Double,
    private val maxArguments: Int = 0,
) : PreEvaluatedArgumentsExpression {

    override fun evaluate(arguments: List<Any?>, data: Any?, jsonPath: String): Any? {
        if (arguments.isEmpty()) {
            return null
        }

        // Collect all of the arguments
        val values = DoubleArray(arguments.size)

        for (i in arguments.indices) {
            var value = arguments[i]

            if (key == "*" || key == "+") {
                while (ArrayLike.isEligible(value)) {
                    val array = ArrayLike.of(value)
                    if (array.isEmpty()) {
                        break
                    }
                    value = array[0]
                }
            }
            if (value is String) {
                values[i] = parseJavaDouble(value) ?: return null
            } else if (value !is Number) {
                return null
            } else {
                values[i] = value.toDouble()
            }
        }

        if (values.size == 1) {
            if (key == "-") {
                return -values[0]
            }

            if (key == "/") {
                return null
            }
        }

        // Reduce the values into a single result
        var accumulator = values[0]

        var i = 1
        while (i < values.size && (i < maxArguments || maxArguments == 0)) {
            accumulator = reducer(accumulator, values[i])
            i++
        }

        return accumulator
    }

    companion object {
        val ADD = MathExpression("+", { a, b -> a + b })
        val SUBTRACT = MathExpression("-", { a, b -> a - b }, 2)
        val MULTIPLY = MathExpression("*", { a, b -> a * b })
        val DIVIDE = MathExpression("/", { a, b -> a / b }, 2)
        val MODULO = MathExpression("%", { a, b -> a % b }, 2)
        val MIN = MathExpression("min", ::smaller)
        val MAX = MathExpression("max", ::larger)

        /*
         * `min` and `max` reduce with java.lang.Math, which departs from `<`/`>` in two places: a NaN
         * operand carries through to the result, and -0.0 is ordered below 0.0. kotlin.math states the
         * NaN half only, so both are spelled out here to hold identically on every target.
         */

        private fun smaller(a: Double, b: Double): Double {
            if (a.isNaN()) return a
            if (a == 0.0 && b == 0.0 && b.toRawBits() < 0) return b

            return if (a <= b) a else b
        }

        private fun larger(a: Double, b: Double): Double {
            if (a.isNaN()) return a
            if (a == 0.0 && b == 0.0 && a.toRawBits() < 0) return b

            return if (a >= b) a else b
        }
    }
}
