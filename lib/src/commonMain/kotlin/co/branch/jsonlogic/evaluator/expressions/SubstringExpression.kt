package co.branch.jsonlogic.evaluator.expressions

import co.branch.jsonlogic.evaluator.JsonLogicEvaluationException
import co.branch.jsonlogic.internal.javaStringify

/**
 * `substr`: extracts a substring of its first argument, rendered the way [javaStringify] renders any
 * value. Requires 2 or 3 arguments; the second must be a number (specifically a [Double] — every
 * number this engine produces already is one, so this is a type check, not a range check) giving the
 * start offset, and the optional third a length (from the start offset) or, when negative, an offset
 * from the end.
 *
 * A negative start or length counts back from the end of the string. Upstream clamps only the
 * 2-argument form's start offset to the empty string when it is still negative after that adjustment,
 * and only the 3-argument form's combination of offsets to the empty string when the start is past
 * the end or past the computed end offset; every other out-of-range combination — including a start
 * that is still negative in the 3-argument form — reaches the underlying substring call and throws,
 * exactly as upstream's does. A null first argument likewise reaches, and throws out of, its rendering
 * step, matching upstream's own un-null-checked `toString()` call.
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

        val value = javaStringify(arguments[0]!!)
        var startIndex: Int
        var endIndex: Int

        if (arguments.size == 2) {
            startIndex = (arguments[1] as Double).toInt()
            endIndex = value.length

            if (startIndex < 0) {
                startIndex += endIndex
            }
            if (startIndex < 0) {
                return ""
            }
        } else {
            if (arguments[2] !is Double) {
                throw JsonLogicEvaluationException("third argument to substr must be an integer", "$jsonPath[2]")
            }

            startIndex = (arguments[1] as Double).toInt()
            if (startIndex < 0) {
                startIndex += value.length
            }

            endIndex = (arguments[2] as Double).toInt()
            if (endIndex < 0) {
                endIndex += value.length
            } else {
                endIndex += startIndex
            }

            if (startIndex > endIndex || endIndex > value.length) {
                return ""
            }
        }

        return value.substring(startIndex, endIndex)
    }

    companion object {
        val INSTANCE = SubstringExpression()
    }
}
