package co.branch.jsonlogic.evaluator

import co.branch.jsonlogic.ast.JsonLogicArray
import co.branch.jsonlogic.ast.JsonLogicBoolean
import co.branch.jsonlogic.ast.JsonLogicNode
import co.branch.jsonlogic.ast.JsonLogicNull
import co.branch.jsonlogic.ast.JsonLogicNumber
import co.branch.jsonlogic.ast.JsonLogicOperation
import co.branch.jsonlogic.ast.JsonLogicString
import co.branch.jsonlogic.ast.JsonLogicVariable
import co.branch.jsonlogic.internal.javaSplitOnDot

/**
 * Marks a `var` path segment that is absent, as opposed to present with a null value: the former
 * falls back to the `var` default, the latter resolves to null. Confined to this file — the two
 * functions that produce it also consume it, so it can never reach an expression or a caller.
 */
private object Missing

/**
 * Evaluates a parsed rule against a data value, dispatching each operation to the
 * [JsonLogicExpression] registered under its operator.
 *
 * Both the data and the result are the evaluator's own value domain: [Double], [String], [Boolean],
 * `null`, [List] and [Map]. Every entry point takes a `jsonPath` locating the node inside the rule
 * (`$` at the root) and extends it as it recurses, so a [JsonLogicEvaluationException] names the
 * sub-expression that failed.
 */
class JsonLogicEvaluator(expressions: Map<String, JsonLogicExpression>) {

    private val expressions: Map<String, JsonLogicExpression> = expressions.toMap()

    /** Builds the operator table by indexing [expressions] on [JsonLogicExpression.key]. */
    constructor(expressions: Collection<JsonLogicExpression>) : this(expressions.associateBy { it.key })

    /** Evaluates any node, dispatching on its kind. Pass `$` as [jsonPath] for a whole rule. */
    fun evaluate(node: JsonLogicNode, data: Any?, jsonPath: String): Any? = when (node) {
        is JsonLogicVariable -> evaluate(node, data, "$jsonPath.var")
        is JsonLogicArray -> evaluate(node, data, jsonPath)
        is JsonLogicOperation -> evaluate(node, data, jsonPath)
        is JsonLogicString -> node.value
        is JsonLogicNumber -> node.value
        is JsonLogicBoolean -> node.value
        JsonLogicNull -> null
    }

    /** Evaluates every element of [array] in the same [data] context, in order. */
    fun evaluate(array: JsonLogicArray, data: Any?, jsonPath: String): List<Any?> =
        array.elements.mapIndexed { index, element -> evaluate(element, data, "$jsonPath[$index]") }

    /** Hands [operation]'s unevaluated arguments to the expression registered under its operator. */
    fun evaluate(operation: JsonLogicOperation, data: Any?, jsonPath: String): Any? {
        val handler = expressions[operation.operator]
            ?: throw JsonLogicEvaluationException("Undefined operation '${operation.operator}'", jsonPath)

        return handler.evaluate(this, operation.arguments, data, "$jsonPath.${operation.operator}")
    }

    /**
     * Resolves a `var` reference: a numeric key indexes into list-like data, a string key walks a
     * dot-separated path through maps and lists, and an empty key yields [data] itself. A key that
     * is absent anywhere along the path falls back to the variable's default value, whereas a key
     * that is present with a null value resolves to null.
     *
     * The default value is always evaluated against null data, so it cannot itself reference [data].
     */
    fun evaluate(variable: JsonLogicVariable, data: Any?, jsonPath: String): Any? {
        val defaultValue = evaluate(variable.defaultValue, null, "$jsonPath[1]")

        if (data == null) {
            return defaultValue
        }

        val key = evaluate(variable.key, data, "$jsonPath[0]")

        if (key == null) {
            // The engine this library ports reaches the same result through an Optional whose
            // orElse argument Java evaluates eagerly, even though data is known to be non-null: a
            // default expression with side effects runs a second time here, so it does so here too.
            evaluate(variable.defaultValue, null, "$jsonPath[1]")

            return transform(data)
        }

        if (key is Number) {
            val index = key.toInt()

            if (ArrayLike.isEligible(data)) {
                val list = ArrayLike.of(data)

                if (index >= 0 && index < list.size) {
                    return transform(list[index])
                }
            }

            return defaultValue
        }

        if (key is String) {
            if (key.isEmpty()) {
                return data
            }

            var result: Any? = data

            for (partial in javaSplitOnDot(key)) {
                result = evaluatePartialVariable(partial, result, "$jsonPath[0]")

                if (result === Missing) {
                    return defaultValue
                } else if (result == null) {
                    return null
                }
            }

            return result
        }

        throw JsonLogicEvaluationException("var first argument must be null, number, or string", "$jsonPath[0]")
    }

    /**
     * Resolves one segment of a dotted `var` path, returning [Missing] when the segment names an
     * out-of-range index or an absent map key, and null when [data] is neither list-like nor a map
     * and so cannot be traversed at all.
     */
    private fun evaluatePartialVariable(key: String, data: Any?, jsonPath: String): Any? {
        if (ArrayLike.isEligible(data)) {
            val list = ArrayLike.of(data)
            val index = key.toIntOrNull() ?: throw notAnIndex(key, jsonPath)

            if (index < 0 || index >= list.size) {
                return Missing
            }

            return transform(list[index])
        }

        if (MapLike.isEligible(data)) {
            val map = MapLike.of(data)

            return if (map.containsKey(key)) transform(map[key]) else Missing
        }

        return null
    }

    companion object {
        /** Normalizes numbers to [Double], leaving every other value untouched. */
        fun transform(value: Any?): Any? = if (value is Number) value.toDouble() else value

        /**
         * Indexing list-like data with a non-numeric path segment surfaces as the failure of an
         * `Integer.parseInt`, reported through the exception's `cause` and therefore rendered as the
         * Java exception's `toString()`. That text is part of the engine's error contract, so it is
         * spelled out rather than read back from a Kotlin exception, whose class name and rendering
         * vary per target.
         */
        private fun notAnIndex(key: String, jsonPath: String): JsonLogicEvaluationException {
            val message = "For input string: \"$key\""

            return JsonLogicEvaluationException(
                "java.lang.NumberFormatException: $message",
                NumberFormatException(message),
                jsonPath,
            )
        }
    }
}
