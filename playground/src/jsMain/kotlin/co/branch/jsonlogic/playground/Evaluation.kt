package co.branch.jsonlogic.playground

import co.branch.jsonlogic.JsonLogic
import co.branch.jsonlogic.JsonLogicException
import co.branch.jsonlogic.ast.JsonLogicNode
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

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
     * Anything the engine raises outside its own exception hierarchy. Nothing known reaches this, but
     * a browser tab is a bad place to discover otherwise: a custom operation or a rule shape no test
     * covers would take the whole page down instead of filling in one card.
     */
    Unexpected("Unexpected failure"),
}

/**
 * The validity flags are separate from [outcome] because each editor has its own status indicator:
 * a single sequential evaluation reports only the first failure, leaving the other editor claiming
 * text that does not parse is valid.
 */
data class Evaluation(
    val outcome: EvalOutcome,
    val ruleValid: Boolean,
    val dataValid: Boolean,
)

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
 * A [JsonLogicException] built from a cause takes its message from that cause's `toString`, which
 * prefixes a class name; the root cause's own message does not.
 */
private fun Throwable.describe(): String {
    val root = generateSequence(this) { it.cause }.last()

    return root.message?.takeIf { it.isNotBlank() } ?: root::class.simpleName ?: "Unknown error"
}
