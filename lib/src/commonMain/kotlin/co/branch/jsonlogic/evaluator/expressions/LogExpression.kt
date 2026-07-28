package co.branch.jsonlogic.evaluator.expressions

import co.branch.jsonlogic.evaluator.JsonLogicEvaluationException
import co.branch.jsonlogic.internal.javaStringify

/**
 * `log`: writes its sole argument to [sink], prefixed with `JsonLogic: `, and returns that argument
 * unchanged. Requires exactly 1 argument.
 *
 * Upstream writes to a `PrintStream` (`System.out` by default); this port takes a plain sink instead
 * so callers on every target can capture or redirect the output, with [STDOUT] wired to [println] to
 * match the default.
 */
class LogExpression(private val sink: (String) -> Unit = ::println) : PreEvaluatedArgumentsExpression {

    override val key: String = "log"

    override fun evaluate(arguments: List<Any?>, data: Any?, jsonPath: String): Any? {
        if (arguments.isEmpty()) {
            throw JsonLogicEvaluationException("log operator requires exactly 1 argument", jsonPath)
        }

        val value = arguments[0]
        sink("JsonLogic: ${javaStringify(value)}")

        return value
    }

    companion object {
        val STDOUT = LogExpression()
    }
}
