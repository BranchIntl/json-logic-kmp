package co.branch.jsonlogic.evaluator.expressions

import co.branch.jsonlogic.evaluator.ArrayLike
import co.branch.jsonlogic.evaluator.JsonLogicEvaluationException
import co.branch.jsonlogic.evaluator.MapLike
import co.branch.jsonlogic.internal.javaSplitOnDot

/**
 * `missing` and `missing_some`: given key names, reports which of them the data does not provide.
 *
 * `{"missing": ["a", "b"]}` returns the keys among `a` and `b` that are missing, in the order asked
 * for, a repeated key once per repeat. `{"missing_some": [2, ["a", "b", "c"]]}` returns nothing at all
 * once at least 2 of the 3 keys are present, and otherwise reports every missing one.
 *
 * Each key is looked up the way `var` looks it up, and is missing when that gives null or the empty
 * string, as in the reference implementation. So `a.b` is provided by `{"a": {"b": 1}}`, `a` by
 * `{"a": {"b": 1}}` too, and `a.0` by `{"a": [1]}`. Where `var` throws (a segment that is not an index
 * over a list, or a key that is neither a string, a number nor null), the key is missing instead.
 */
class MissingExpression private constructor(private val isSome: Boolean) : PreEvaluatedArgumentsExpression {

    override val key: String = if (isSome) "missing_some" else "missing"

    override fun evaluate(arguments: List<Any?>, data: Any?, jsonPath: String): Any? {
        if (isSome && (arguments.size < 2 || !ArrayLike.isEligible(arguments[1]) || arguments[0] !is Double)) {
            throw JsonLogicEvaluationException(
                "missing_some expects first argument to be an integer and the second " +
                    "argument to be an array",
                jsonPath,
            )
        }

        val options: List<Any?> = if (isSome) ArrayLike.of(arguments[1]) else arguments
        val missing = options.filter { key -> lookUp(key, data).let { it == null || it == "" } }

        if (isSome && options.size - missing.size >= (arguments[0] as Double).toInt()) {
            return emptyList<Any?>()
        }

        return missing
    }

    companion object {
        val ALL = MissingExpression(false)
        val SOME = MissingExpression(true)

        /**
         * Resolves [key] against [data] as `JsonLogicEvaluator`'s `var` does, but answers null where
         * `var` would throw or fall back to its default. Walks only the key's own segments, never
         * the rest of [data]. Mirrors `JsonLogicEvaluator.evaluate(JsonLogicVariable, ...)` and its
         * `evaluatePartialVariable`; change both together.
         */
        private fun lookUp(key: Any?, data: Any?): Any? = when {
            key == null || key == "" -> data
            key is Number -> if (ArrayLike.isEligible(data)) ArrayLike.of(data).getOrNull(key.toInt()) else null
            key is String -> javaSplitOnDot(key).fold(data) { node, segment ->
                when {
                    ArrayLike.isEligible(node) -> segment.toIntOrNull()?.let { ArrayLike.of(node).getOrNull(it) }
                    MapLike.isEligible(node) -> MapLike.of(node)[segment]
                    else -> null
                }
            }
            else -> null
        }
    }
}
