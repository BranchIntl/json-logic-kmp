package co.branch.jsonlogic.parity

import kotlinx.serialization.json.JsonElement
import co.branch.jsonlogic.JsonLogicException as PortedJsonLogicException
import io.github.jamsesso.jsonlogic.JsonLogicException as OracleJsonLogicException

/** What one engine did with one case: a value, or a throwable of any kind. */
internal sealed interface Outcome<out T> {
    data class Returned<T>(val value: T) : Outcome<T>
    data class Threw(val error: Throwable) : Outcome<Nothing>
}

/**
 * Runs [block] as one engine's turn at a case. Catches [Throwable] rather than the engine's declared
 * exception type, because an engine escaping with an undeclared runtime failure where the other
 * returns is exactly the kind of divergence this gate exists to catch.
 */
internal inline fun <T> outcomeOf(block: () -> T): Outcome<T> = try {
    Outcome.Returned(block())
} catch (error: Throwable) {
    Outcome.Threw(error)
}

/**
 * The comparable identity of a failure. `type` is the exception's simple name, which the port shares
 * with the engine it ports (`JsonLogicParseException`, `JsonLogicEvaluationException`) even though
 * the packages differ; `jsonPath` is present only for the engines' own exception type, so two
 * unrelated runtime failures still compare on type and message.
 */
internal data class ErrorSignature(val type: String, val message: String?, val jsonPath: String?) {
    override fun toString(): String {
        val path = jsonPath?.let { " at \"$it\"" } ?: ""

        return "$type: \"$message\"$path"
    }
}

internal fun oracleErrorSignature(error: Throwable): ErrorSignature = ErrorSignature(
    type = error.javaClass.simpleName,
    message = error.message,
    jsonPath = (error as? OracleJsonLogicException)?.jsonPath,
)

internal fun portedErrorSignature(error: Throwable): ErrorSignature = ErrorSignature(
    type = error.javaClass.simpleName,
    message = error.message,
    jsonPath = (error as? PortedJsonLogicException)?.jsonPath,
)

/** How the two engines disagreed about a case, or null when they agreed. */
internal enum class DisagreementKind(val description: String) {
    ORACLE_THREW_ONLY("the Java engine threw where the port returned"),
    PORT_THREW_ONLY("the port threw where the Java engine returned"),
    VALUES_DIFFER("both returned, values differ"),
    ERRORS_DIFFER("both threw, failures differ"),
}

/**
 * Compares one case's two outcomes. Failures are compared by [ErrorSignature], so the gate holds the
 * port to the Java engine's exact message and jsonPath, not merely to failing in the same place.
 */
internal fun disagreementBetween(
    oracle: Outcome<Any?>,
    ported: Outcome<JsonElement>,
): DisagreementKind? = when {
    oracle is Outcome.Returned && ported is Outcome.Returned ->
        DisagreementKind.VALUES_DIFFER.takeIf { !ParityComparator.matches(oracle.value, ported.value) }

    oracle is Outcome.Threw && ported is Outcome.Threw ->
        DisagreementKind.ERRORS_DIFFER.takeIf {
            oracleErrorSignature(oracle.error) != portedErrorSignature(ported.error)
        }

    oracle is Outcome.Threw -> DisagreementKind.ORACLE_THREW_ONLY

    else -> DisagreementKind.PORT_THREW_ONLY
}

/**
 * The full diagnostic for one disagreement: enough to reproduce it by hand against either engine.
 * [label] locates the case, [rule] is the rule text as the Java engine received it, and [data] is
 * the data as the fixture files spell it.
 */
internal fun disagreementReport(
    label: String,
    rule: String,
    data: String,
    oracle: Outcome<Any?>,
    ported: Outcome<JsonElement>,
    kind: DisagreementKind,
): String = buildString {
    appendLine("$label — ${kind.description}")
    appendLine("    rule:   $rule")
    appendLine("    data:   $data")
    appendLine("    java:   ${describeOracleOutcome(oracle)}")
    append("    kotlin: ${describePortedOutcome(ported)}")
}

private fun describeOracleOutcome(outcome: Outcome<Any?>): String = when (outcome) {
    is Outcome.Returned -> "returned ${describeJavaValue(outcome.value)}"
    is Outcome.Threw -> "threw ${oracleErrorSignature(outcome.error)}"
}

private fun describePortedOutcome(outcome: Outcome<JsonElement>): String = when (outcome) {
    is Outcome.Returned -> "returned ${outcome.value}"
    is Outcome.Threw -> "threw ${portedErrorSignature(outcome.error)}"
}
