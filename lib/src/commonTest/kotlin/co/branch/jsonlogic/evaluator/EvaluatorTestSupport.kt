package co.branch.jsonlogic.evaluator

import co.branch.jsonlogic.ast.JsonLogicArray
import co.branch.jsonlogic.ast.JsonLogicParser
import co.branch.jsonlogic.evaluator.expressions.MissingExpression
import co.branch.jsonlogic.evaluator.expressions.PreEvaluatedArgumentsExpression
import co.branch.jsonlogic.internal.jsonElementToValue
import co.branch.jsonlogic.internal.valueToJsonElement
import kotlinx.serialization.json.JsonElement

/** The only operators this workstream registers; the full default table belongs to the entry point. */
internal val missingExpressions: List<JsonLogicExpression> =
    listOf(MissingExpression.ALL, MissingExpression.SOME)

/** Evaluates [rule] against [data] already in the evaluator's value domain. */
internal fun evaluate(
    rule: String,
    data: Any?,
    expressions: List<JsonLogicExpression> = missingExpressions,
): Any? = JsonLogicEvaluator(expressions).evaluate(JsonLogicParser.parse(rule), data, "$")

/** Evaluates a JSON rule against JSON data, crossing the interop boundary in both directions. */
internal fun evaluateJson(
    rule: JsonElement,
    data: JsonElement,
    expressions: List<JsonLogicExpression> = missingExpressions,
): JsonElement = valueToJsonElement(
    JsonLogicEvaluator(expressions).evaluate(JsonLogicParser.parse(rule), jsonElementToValue(data), "$"),
)

/** Returns [value] whatever it is handed, to feed operations values the AST cannot express. */
internal class ConstantExpression(override val key: String, private val value: Any?) : JsonLogicExpression {
    override fun evaluate(
        evaluator: JsonLogicEvaluator,
        arguments: JsonLogicArray,
        data: Any?,
        jsonPath: String,
    ): Any? = value
}

/** Captures what the evaluator hands a pre-evaluated-arguments operation, and echoes the arguments. */
internal class RecordingExpression(override val key: String = "probe") : PreEvaluatedArgumentsExpression {
    var arguments: List<Any?>? = null
        private set
    var data: Any? = null
        private set
    var jsonPath: String? = null
        private set

    override fun evaluate(arguments: List<Any?>, data: Any?, jsonPath: String): Any? {
        this.arguments = arguments
        this.data = data
        this.jsonPath = jsonPath

        return arguments
    }
}
