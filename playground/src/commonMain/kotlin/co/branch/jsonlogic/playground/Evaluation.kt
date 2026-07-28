package co.branch.jsonlogic.playground

import co.branch.jsonlogic.JsonLogic
import co.branch.jsonlogic.JsonLogicException
import co.branch.jsonlogic.ast.JsonLogicNode
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/** What the result panel shows. */
sealed interface EvalOutcome {

    /** No rule has been entered yet. */
    data object Empty : EvalOutcome

    data class Success(val value: JsonElement) : EvalOutcome

    data class Failure(val kind: FailureKind, val detail: String, val jsonPath: String?) : EvalOutcome
}

enum class FailureKind(val label: String) {
    RuleSyntax("Rule is not valid JsonLogic"),
    DataSyntax("Data is not valid JSON"),
    Evaluation("Evaluation failed"),

    /**
     * An exception the engine does not model, which callers still have to survive: `cat` and
     * `substr` throw a bare `NullPointerException` on a null operand, and `substr` throws on a
     * start/length pair that runs past the end of the string.
     */
    Unexpected("Unexpected failure"),
}

/**
 * One evaluation pass: the result, plus whether each editor's text parsed.
 *
 * The two validity flags are tracked separately from [outcome] because both editors have their own
 * status indicator, and a single sequential evaluation would only ever report the first failure —
 * leaving the other editor showing "valid" for text that is not.
 */
data class Evaluation(
    val outcome: EvalOutcome,
    val ruleValid: Boolean,
    val dataValid: Boolean,
) {
    companion object {
        val Blank = Evaluation(EvalOutcome.Empty, ruleValid = true, dataValid = true)
    }
}

fun evaluate(jsonLogic: JsonLogic, ruleText: String, dataText: String): Evaluation {
    var rule: JsonLogicNode? = null
    var ruleFailure: EvalOutcome.Failure? = null
    if (ruleText.isNotBlank()) {
        try {
            rule = jsonLogic.parse(ruleText)
        } catch (e: JsonLogicException) {
            ruleFailure = EvalOutcome.Failure(FailureKind.RuleSyntax, e.describe(), e.jsonPath)
        }
    }

    var data: JsonElement? = null
    var dataFailure: EvalOutcome.Failure? = null
    if (dataText.isNotBlank()) {
        try {
            data = Json.parseToJsonElement(dataText)
        } catch (e: SerializationException) {
            dataFailure = EvalOutcome.Failure(FailureKind.DataSyntax, e.describe(), null)
        }
    }

    val outcome = when {
        ruleFailure != null -> ruleFailure
        dataFailure != null -> dataFailure
        rule == null -> EvalOutcome.Empty
        else -> apply(jsonLogic, rule, data)
    }

    return Evaluation(outcome, ruleValid = ruleFailure == null, dataValid = dataFailure == null)
}

private fun apply(jsonLogic: JsonLogic, rule: JsonLogicNode, data: JsonElement?): EvalOutcome =
    try {
        EvalOutcome.Success(jsonLogic.apply(rule, data))
    } catch (e: JsonLogicException) {
        EvalOutcome.Failure(FailureKind.Evaluation, e.describe(), e.jsonPath)
    } catch (e: Throwable) {
        EvalOutcome.Failure(FailureKind.Unexpected, e.describe(), null)
    }

/**
 * The most specific message available.
 *
 * A [JsonLogicException] built from a cause takes its own message from that cause's `toString`,
 * which prefixes the text with a class name; reading the root cause's message directly avoids it.
 */
private fun Throwable.describe(): String {
    val root = generateSequence(this) { it.cause }.last()

    return root.message?.takeIf { it.isNotBlank() } ?: root::class.simpleName ?: "Unknown error"
}
