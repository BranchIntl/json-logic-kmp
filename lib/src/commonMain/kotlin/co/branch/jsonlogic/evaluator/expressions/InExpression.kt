package co.branch.jsonlogic.evaluator.expressions

import co.branch.jsonlogic.evaluator.ArrayLike
import co.branch.jsonlogic.internal.ecmaStringify

/**
 * `in`: whether the first argument is contained in the second.
 *
 * A string second argument makes this a substring test — `{"in": ["Spring", "Springfield"]}` is true
 * — against the first argument's rendering, so a number matches the digits ECMAScript writes it with
 * (`{"in": [1, "a1b"]}` is true) and null matches the text `"null"`. A list-like second argument makes
 * it a membership test, by value and to any depth, so a list can be found inside a list of lists.
 *
 * Membership neither coerces nor compares numbers numerically: `1` is not found in `["1.0"]`, NaN is
 * found in `[NaN]`, and `-0.0` is not found in `[0.0]`. A second argument that is neither a string nor
 * list-like, or fewer than two arguments, is false rather than an error.
 */
class InExpression private constructor() : PreEvaluatedArgumentsExpression {

    override val key: String = "in"

    override fun evaluate(arguments: List<Any?>, data: Any?, jsonPath: String): Any? {
        if (arguments.size < 2) {
            return false
        }

        val needle = arguments[0]
        val haystack = arguments[1]

        if (haystack is String) {
            return haystack.contains(ecmaStringify(needle))
        }

        if (ArrayLike.isEligible(haystack)) {
            return ArrayLike.of(haystack).contains(needle)
        }

        return false
    }

    companion object {
        val INSTANCE = InExpression()
    }
}
