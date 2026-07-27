package co.branch.jsonlogic.evaluator.expressions

import co.branch.jsonlogic.evaluator.ArrayLike
import co.branch.jsonlogic.evaluator.JsonLogicEvaluationException
import co.branch.jsonlogic.evaluator.MapLike

/**
 * `missing` and `missing_some`: given key names, reports which of them the data does not provide.
 *
 * `{"missing": ["a", "b"]}` returns the subset of `a` and `b` that is absent, in the order asked for
 * and without repeats. `{"missing_some": [2, ["a", "b", "c"]]}` returns nothing at all once at least
 * 2 of the 3 keys are present, and otherwise reports every absent one.
 *
 * Keys are matched against the data's flattened paths, so `a.b` is provided by `{"a": {"b": 1}}`.
 * Only nested maps are flattened: a key can never reach into a list.
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

        if (!MapLike.isEligible(data)) {
            if (isSome) {
                if ((arguments[0] as Double).toInt() <= 0) {
                    return emptyList<Any?>()
                }
                return arguments[1]
            }
            return arguments
        }

        val map = MapLike.of(data)
        val options: List<Any?> = if (isSome) ArrayLike.of(arguments[1]) else arguments
        val providedKeys = flatKeys(map)
        val requiredKeys = LinkedHashSet<Any?>(options)

        requiredKeys.removeAll(providedKeys) // Keys that I need but do not have

        if (isSome && options.size - requiredKeys.size >= (arguments[0] as Double).toInt()) {
            return emptyList<Any?>()
        }

        return requiredKeys.toList()
    }

    companion object {
        val ALL = MissingExpression(false)
        val SOME = MissingExpression(true)

        /**
         * Given a map structure such as `{a: {b: 1}, c: 2}`, returns the set `["a.b", "c"]`. A
         * nested map contributes only its own leaves, so an empty one contributes no key at all.
         */
        private fun flatKeys(map: Map<Any?, Any?>, prefix: String = ""): Set<String> {
            val keys = LinkedHashSet<String>()

            for ((key, value) in map) {
                if (MapLike.isEligible(value)) {
                    keys.addAll(flatKeys(MapLike.of(value), "$prefix$key."))
                } else {
                    keys.add("$prefix$key")
                }
            }

            return keys
        }
    }
}
