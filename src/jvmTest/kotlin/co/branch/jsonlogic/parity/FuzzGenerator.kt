package co.branch.jsonlogic.parity

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.random.Random

/** A generated rule together with the data it is meant to be evaluated against. */
internal class FuzzCase(val rule: JsonElement, val data: JsonElement)

/**
 * Generates rule/data pairs from the operator grammar for the differential fuzzer: literals, `var`
 * references over the paths that actually exist in the generated data (plus some that do not), and
 * nested operations at plausible arities, including arities and argument types the operators reject.
 *
 * Two things are deliberately never generated, because the port is known to differ there and the
 * differences are accepted rather than bugs:
 *
 * - **`missing`/`missing_some` inside a comparison.** A one-argument `missing` over data that is not
 *   map-like returns the Java engine's argument-spread wrapper, whose `equals` unconditionally
 *   returns false and which has no `hashCode` of its own. Comparing that value, or matching it with
 *   `in`, therefore answers false in the Java engine (and true with the operands swapped), where this
 *   library — whose value domain has no such wrapper — answers structurally. So no `missing` or
 *   `missing_some` is generated anywhere inside the operands of `==`, `!=`, `===`, `!==` or `in`, nor
 *   inside another `missing`, whose argument set is itself built through `equals` and `hashCode`.
 * - **Anything reaching the Java engine through Gson's lenient parsing.** Every generated rule is a
 *   strict-JSON tree, rendered by Gson itself before the Java engine re-parses it, so the lenient
 *   forms Gson would also accept (unquoted names, single quotes, `NaN` tokens) never arise. This
 *   library's parser accepts only strict JSON.
 *
 * `log` is left out as well: it is fixture-covered, and it writes every evaluation to stdout.
 */
internal class FuzzGenerator(seed: Long) {

    private val random = Random(seed)

    /** Var paths worth referencing for the case being built; reset by every [nextCase]. */
    private var paths: List<String> = ABSENT_PATHS

    fun nextCase(): FuzzCase {
        val data = nextData()
        paths = (pathsIn(data, "") + ABSENT_PATHS).shuffled(random)

        return FuzzCase(rule(MAX_DEPTH, allowMissing = true), data)
    }

    // ---- data ---------------------------------------------------------------------------------

    private fun nextData(): JsonElement = when (random.nextInt(12)) {
        0 -> JsonArray(List(1 + random.nextInt(3)) { dataValue(1) })
        1 -> scalar()
        else -> JsonObject(DATA_KEYS.take(1 + random.nextInt(DATA_KEYS.size)).associateWith { dataValue(2) })
    }

    private fun dataValue(depth: Int): JsonElement = when {
        depth <= 0 -> scalar()
        random.nextInt(5) == 0 -> JsonArray(List(random.nextInt(4)) { dataValue(depth - 1) })
        random.nextInt(5) == 0 -> JsonObject(DATA_KEYS.take(1 + random.nextInt(2)).associateWith { dataValue(depth - 1) })
        else -> scalar()
    }

    private fun pathsIn(element: JsonElement, prefix: String): List<String> = when (element) {
        is JsonObject -> element.entries.flatMap { (key, value) ->
            val path = if (prefix.isEmpty()) key else "$prefix.$key"
            listOf(path) + pathsIn(value, path)
        }

        is JsonArray -> element.mapIndexed { index, value ->
            val path = if (prefix.isEmpty()) "$index" else "$prefix.$index"
            listOf(path) + pathsIn(value, path)
        }.flatten()

        else -> emptyList()
    }

    // ---- rules --------------------------------------------------------------------------------

    private fun rule(depth: Int, allowMissing: Boolean): JsonElement =
        if (depth <= 0 || random.nextInt(6) == 0) leaf() else operation(depth, allowMissing)

    private fun leaf(): JsonElement = when (random.nextInt(8)) {
        0, 1, 2 -> variable()
        3 -> JsonArray(List(random.nextInt(3)) { scalar() })
        else -> scalar()
    }

    private fun operation(depth: Int, allowMissing: Boolean): JsonElement {
        val next = depth - 1

        return when (random.nextInt(if (allowMissing) 15 else 13)) {
            0 -> op(ARITHMETIC.pick(), args(1 + random.nextInt(3), next, allowMissing))
            1 -> op(COMPARISON.pick(), args(2 + random.nextInt(2), next, allowMissing))
            2 -> op(EQUALITY.pick(), args(2, next, allowMissing = false))
            3 -> op(if (random.nextBoolean()) "!" else "!!", rule(next, allowMissing))
            4 -> op(if (random.nextBoolean()) "and" else "or", args(1 + random.nextInt(3), next, allowMissing))
            5 -> op(if (random.nextBoolean()) "if" else "?:", args(2 + random.nextInt(4), next, allowMissing))
            6 -> op("cat", args(random.nextInt(4), next, allowMissing))
            7 -> substr(next, allowMissing)
            8 -> op("merge", args(random.nextInt(4), next, allowMissing))
            9 -> op("in", JsonArray(listOf(rule(next, allowMissing = false), haystack(next))))
            10 -> op(ARRAY_OPS.pick(), JsonArray(listOf(arrayish(next), elementRule(next))))
            11 -> reduce(next)
            12 -> rejected(next, allowMissing)
            13 -> op("missing", JsonArray(List(1 + random.nextInt(3)) { missingKey(next) }))
            else -> op(
                "missing_some",
                JsonArray(
                    listOf(
                        JsonPrimitive(random.nextInt(-1, 4)),
                        JsonArray(List(random.nextInt(4)) { missingKey(next) }),
                    ),
                ),
            )
        }
    }

    private fun args(count: Int, depth: Int, allowMissing: Boolean): JsonArray =
        JsonArray(List(count) { rule(depth, allowMissing) })

    private fun variable(): JsonElement = when (random.nextInt(12)) {
        0 -> op("var", JsonPrimitive(random.nextInt(-1, 4)))
        1 -> op("var", JsonArray(listOf(JsonPrimitive(paths.pick()), scalar())))
        2 -> op("var", JsonArray(emptyList()))
        3 -> op("var", JsonNull)
        else -> op("var", JsonPrimitive(paths.pick()))
    }

    private fun substr(depth: Int, allowMissing: Boolean): JsonElement {
        val subject = if (random.nextInt(3) == 0) rule(depth, allowMissing) else JsonPrimitive(WORDS.pick())
        val arguments = mutableListOf(subject, offset())
        if (random.nextInt(3) != 0) arguments += offset()

        return op("substr", JsonArray(arguments))
    }

    private fun reduce(depth: Int): JsonElement = op(
        "reduce",
        JsonArray(
            listOf(
                arrayish(depth),
                op(
                    if (random.nextInt(4) == 0) COMPARISON.pick() else ARITHMETIC.pick(),
                    JsonArray(listOf(op("var", JsonPrimitive("accumulator")), op("var", JsonPrimitive("current")))),
                ),
                scalar(),
            ),
        ),
    )

    /** An array-shaped argument for the iterating operators, which is not always actually an array. */
    private fun arrayish(depth: Int): JsonElement = when (random.nextInt(5)) {
        0 -> JsonArray(List(random.nextInt(4)) { scalar() })
        1 -> variable()
        2 -> op("merge", args(1 + random.nextInt(2), depth, allowMissing = false))
        3 -> scalar()
        else -> JsonArray(List(1 + random.nextInt(3)) { JsonPrimitive(random.nextInt(-2, 5)) })
    }

    /** A rule for the iterating operators' second argument, evaluated against one element. */
    private fun elementRule(depth: Int): JsonElement = when (random.nextInt(5)) {
        0 -> op("var", JsonPrimitive(""))
        1 -> op(COMPARISON.pick(), JsonArray(listOf(op("var", JsonPrimitive("")), scalar())))
        2 -> op(ARITHMETIC.pick(), JsonArray(listOf(op("var", JsonPrimitive("")), scalar())))
        3 -> op("var", JsonPrimitive(DATA_KEYS.pick()))
        else -> rule(depth, allowMissing = false)
    }

    private fun haystack(depth: Int): JsonElement = when (random.nextInt(4)) {
        0 -> JsonPrimitive(WORDS.pick())
        1 -> JsonArray(List(random.nextInt(4)) { scalar() })
        2 -> rule(depth, allowMissing = false)
        else -> variable()
    }

    /** A key name for `missing`, occasionally a rule that computes one. */
    private fun missingKey(depth: Int): JsonElement =
        if (random.nextInt(5) == 0) rule(depth, allowMissing = false) else JsonPrimitive(paths.pick())

    /** Shapes both engines are expected to reject, so their rejections are diffed too. */
    private fun rejected(depth: Int, allowMissing: Boolean): JsonElement = when (random.nextInt(5)) {
        0 -> op(UNDEFINED_OPERATORS.pick(), args(1 + random.nextInt(2), depth, allowMissing))
        1 -> JsonObject(
            List(2 + random.nextInt(2)) { index -> ARITHMETIC[index % ARITHMETIC.size] to args(1, depth, allowMissing) }
                .toMap(),
        )
        2 -> op("var", JsonArray(listOf(JsonPrimitive(random.nextBoolean()))))
        3 -> op("var", JsonArray(listOf(JsonArray(listOf(JsonPrimitive(paths.pick()))))))
        else -> op(ARRAY_OPS.pick(), args(random.nextInt(2), depth, allowMissing))
    }

    // ---- leaves -------------------------------------------------------------------------------

    private fun scalar(): JsonElement = when (random.nextInt(14)) {
        0 -> JsonPrimitive(0)
        1 -> JsonPrimitive(1)
        2 -> JsonPrimitive(random.nextInt(-6, 7))
        3 -> JsonPrimitive(2.5)
        4 -> JsonPrimitive(-0.5)
        5 -> JsonPrimitive(1e7)
        6 -> JsonPrimitive("")
        7 -> JsonPrimitive("3")
        8 -> JsonPrimitive("-2.5")
        9, 10 -> JsonPrimitive(WORDS.pick())
        11 -> JsonPrimitive(true)
        12 -> JsonPrimitive(false)
        else -> JsonNull
    }

    private fun offset(): JsonElement = when (random.nextInt(8)) {
        0 -> JsonPrimitive(WORDS.pick())
        1 -> JsonNull
        else -> JsonPrimitive(random.nextInt(-5, 6))
    }

    private fun op(key: String, arguments: JsonElement): JsonObject = JsonObject(mapOf(key to arguments))

    private fun <T> List<T>.pick(): T = this[random.nextInt(size)]

    private companion object {
        const val MAX_DEPTH = 3

        val DATA_KEYS = listOf("a", "b", "c", "d")
        val ABSENT_PATHS = listOf("", "z", "a.z", "0", "3", "-1", "a.0", "b.1.c", "1.a")
        val WORDS = listOf("", " ", "x", "apple", "true", "0", "12", "a.b", "-")
        val ARITHMETIC = listOf("+", "-", "*", "/", "%", "min", "max")
        val COMPARISON = listOf(">", ">=", "<", "<=")
        val EQUALITY = listOf("==", "!=", "===", "!==")
        val ARRAY_OPS = listOf("map", "filter", "all", "some", "none")
        val UNDEFINED_OPERATORS = listOf("nope", "Var", "+ ", "reduceRight")
    }
}
