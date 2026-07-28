package co.branch.jsonlogic.evaluator.expressions

import co.branch.jsonlogic.evaluator.JsonLogicEvaluationException
import co.branch.jsonlogic.internal.ecmaStringify

/**
 * `substr`: extracts part of its first argument, rendered the way [ecmaStringify] renders any value —
 * so a null source yields the four characters of `"null"`, and `{"substr": [null, 1]}` is `"ull"`.
 *
 * Requires 2 or 3 arguments; the second must be a number giving the start offset, and the optional
 * third a length, or, when negative, how many characters to drop from the end of what the start
 * offset left. A negative start counts back from the end of the string. Every offset is clamped into
 * range rather than rejected, so no combination of them fails: a start past the end yields the empty
 * string, and a start still negative after the end-relative adjustment yields the whole of it.
 */
class SubstringExpression private constructor() : PreEvaluatedArgumentsExpression {

    override val key: String = "substr"

    override fun evaluate(arguments: List<Any?>, data: Any?, jsonPath: String): Any? {
        if (arguments.size < 2 || arguments.size > 3) {
            throw JsonLogicEvaluationException("substr expects 2 or 3 arguments", jsonPath)
        }

        if (arguments[1] !is Double) {
            throw JsonLogicEvaluationException("second argument to substr must be a number", "$jsonPath[1]")
        }

        if (arguments.size == 3 && arguments[2] !is Double) {
            throw JsonLogicEvaluationException("third argument to substr must be an integer", "$jsonPath[2]")
        }

        val value = ecmaStringify(arguments[0])
        val start = (arguments[1] as Double).toInt()
        val length = if (arguments.size == 3) (arguments[2] as Double).toInt() else null

        // A negative length drops that many characters from the end of what the start offset left,
        // which the reference reaches by taking the substring twice.
        if (length != null && length < 0) {
            val fromStart = substring(value, start, null)

            return substring(fromStart, 0, fromStart.length + length)
        }

        return substring(value, start, length)
    }

    companion object {
        val INSTANCE = SubstringExpression()
    }
}

/**
 * `String.prototype.substr(start, length)`: a negative [start] counts back from the end and stops at
 * the beginning, a null [length] runs to the end, and both are clamped into range so the call always
 * returns a string.
 */
private fun substring(value: String, start: Int, length: Int?): String {
    val from = if (start < 0) (value.length + start).coerceAtLeast(0) else start.coerceAtMost(value.length)
    val count = (length ?: (value.length - from)).coerceIn(0, value.length - from)

    return value.substring(from, from + count)
}
