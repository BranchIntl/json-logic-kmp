package co.branch.jsonlogic

import co.branch.jsonlogic.ast.JsonLogicNode
import co.branch.jsonlogic.ast.JsonLogicParseException
import co.branch.jsonlogic.ast.JsonLogicParser
import co.branch.jsonlogic.evaluator.JsonLogicEvaluationException
import co.branch.jsonlogic.evaluator.JsonLogicEvaluator
import co.branch.jsonlogic.evaluator.JsonLogicExpression
import co.branch.jsonlogic.evaluator.expressions.AllExpression
import co.branch.jsonlogic.evaluator.expressions.ArrayHasExpression
import co.branch.jsonlogic.evaluator.expressions.ConcatenateExpression
import co.branch.jsonlogic.evaluator.expressions.EqualityExpression
import co.branch.jsonlogic.evaluator.expressions.FilterExpression
import co.branch.jsonlogic.evaluator.expressions.IfExpression
import co.branch.jsonlogic.evaluator.expressions.InExpression
import co.branch.jsonlogic.evaluator.expressions.InequalityExpression
import co.branch.jsonlogic.evaluator.expressions.LogExpression
import co.branch.jsonlogic.evaluator.expressions.LogicExpression
import co.branch.jsonlogic.evaluator.expressions.MapExpression
import co.branch.jsonlogic.evaluator.expressions.MathExpression
import co.branch.jsonlogic.evaluator.expressions.MergeExpression
import co.branch.jsonlogic.evaluator.expressions.MissingExpression
import co.branch.jsonlogic.evaluator.expressions.NotExpression
import co.branch.jsonlogic.evaluator.expressions.NumericComparisonExpression
import co.branch.jsonlogic.evaluator.expressions.PreEvaluatedArgumentsExpression
import co.branch.jsonlogic.evaluator.expressions.ReduceExpression
import co.branch.jsonlogic.evaluator.expressions.StrictEqualityExpression
import co.branch.jsonlogic.evaluator.expressions.StrictInequalityExpression
import co.branch.jsonlogic.evaluator.expressions.SubstringExpression
import co.branch.jsonlogic.internal.jsonElementToValue
import co.branch.jsonlogic.internal.truthy as isTruthy
import co.branch.jsonlogic.internal.valueToJsonElement
import kotlinx.serialization.json.JsonElement

/**
 * The public entry point for parsing and evaluating JsonLogic rules.
 *
 * A new instance registers the same 34 default operations as the engine this library ports, in
 * the same order, and is immediately usable:
 * ```
 * val jsonLogic = JsonLogic()
 * jsonLogic.apply("""{"==": [1, 1]}""", null) // JsonPrimitive(true)
 * ```
 *
 * **Failure modes.** [parse] and every `apply` overload throw [JsonLogicParseException] when the
 * rule is not well-formed JSON or violates the operation-object shape (an object with more than
 * one key), and [JsonLogicEvaluationException] when evaluation itself fails — an unregistered
 * operator, a wrong argument count, a value of the wrong shape. Both extend [JsonLogicException],
 * itself an unchecked [RuntimeException]: unlike the engine this library ports, whose
 * `JsonLogicException` is a checked exception that every caller must declare or catch, nothing
 * here needs a `throws` clause or a `try` block to compile.
 *
 * **Thread safety.** Finish configuring an instance — the default registrations plus any
 * [addOperation] calls — on a single thread before sharing it with others. Once configuration is
 * complete and the instance has been safely published to other threads (configure it before
 * starting the threads or coroutines that use it, or hand it off through a mechanism that
 * establishes a happens-before edge — a synchronized accessor, an atomic/volatile reference, a
 * channel, thread start/join), concurrent `apply` calls share no mutable state and need no further
 * synchronization. Calling [addOperation] concurrently with another [addOperation] call, or with
 * an in-flight `apply`, is not supported: [addOperation] mutates a shared map and reassigns the
 * evaluator field with no locking, so two concurrent registrations can race and silently lose one
 * operation, and neither write carries any cross-thread visibility guarantee on its own.
 *
 * **The Infinity/NaN sharp edge.** A result of positive infinity, negative infinity, or NaN is
 * returned as a [JsonElement] holding the literal text `Infinity`, `-Infinity`, or `NaN` — JSON has
 * no token for any of them, but this engine can still produce them (e.g. `{"/": [1, 0]}`). Reading
 * the returned element's fields back out in Kotlin works fine, but a consumer who re-encodes it
 * through a standard JSON writer — including the one this library uses internally — will emit text
 * that most JSON parsers reject.
 */
class JsonLogic {

    private val expressions: MutableMap<String, JsonLogicExpression> = mutableMapOf()
    private lateinit var evaluator: JsonLogicEvaluator

    init {
        addOperation(MathExpression.ADD)
        addOperation(MathExpression.SUBTRACT)
        addOperation(MathExpression.MULTIPLY)
        addOperation(MathExpression.DIVIDE)
        addOperation(MathExpression.MODULO)
        addOperation(MathExpression.MIN)
        addOperation(MathExpression.MAX)
        addOperation(NumericComparisonExpression.GT)
        addOperation(NumericComparisonExpression.GTE)
        addOperation(NumericComparisonExpression.LT)
        addOperation(NumericComparisonExpression.LTE)
        addOperation(IfExpression.IF)
        addOperation(IfExpression.TERNARY)
        addOperation(EqualityExpression.INSTANCE)
        addOperation(InequalityExpression.INSTANCE)
        addOperation(StrictEqualityExpression.INSTANCE)
        addOperation(StrictInequalityExpression.INSTANCE)
        addOperation(NotExpression.SINGLE)
        addOperation(NotExpression.DOUBLE)
        addOperation(LogicExpression.AND)
        addOperation(LogicExpression.OR)
        addOperation(LogExpression.STDOUT)
        addOperation(MapExpression.INSTANCE)
        addOperation(FilterExpression.INSTANCE)
        addOperation(ReduceExpression.INSTANCE)
        addOperation(AllExpression.INSTANCE)
        addOperation(ArrayHasExpression.SOME)
        addOperation(ArrayHasExpression.NONE)
        addOperation(MergeExpression.INSTANCE)
        addOperation(InExpression.INSTANCE)
        addOperation(ConcatenateExpression.INSTANCE)
        addOperation(SubstringExpression.INSTANCE)
        addOperation(MissingExpression.ALL)
        addOperation(MissingExpression.SOME)
    }

    /**
     * Registers a custom operation under [name], built from a plain function over its
     * already-evaluated arguments — the convenience form for an operation that has no need to
     * control which of its arguments are evaluated, or in what data context.
     *
     * Registering under a [name] that is already registered replaces the existing operation:
     * whichever registration happens last wins, matching the engine this library ports.
     *
     * @return this instance, for chaining further registrations.
     */
    fun addOperation(name: String, function: (List<Any?>) -> Any?): JsonLogic =
        addOperation(object : PreEvaluatedArgumentsExpression {
            override val key: String = name
            override fun evaluate(arguments: List<Any?>, data: Any?, jsonPath: String): Any? = function(arguments)
        })

    /**
     * Registers [expression] under its own [JsonLogicExpression.key], rebuilding the complete
     * evaluator snapshot immediately: the rebuilt table is visible to further calls on this thread
     * as soon as this method returns, but making it visible to other threads requires the safe
     * publication described in this class's KDoc.
     *
     * Registering under a key that is already registered replaces the existing operation:
     * whichever registration happens last wins, matching the engine this library ports.
     *
     * @return this instance, for chaining further registrations.
     */
    fun addOperation(expression: JsonLogicExpression): JsonLogic {
        expressions[expression.key] = expression
        evaluator = JsonLogicEvaluator(expressions)

        return this
    }

    /** Parses [rule] into a [JsonLogicNode] tree, throwing [JsonLogicParseException] on failure. */
    fun parse(rule: String): JsonLogicNode = JsonLogicParser.parse(rule)

    /**
     * Parses [rule] and evaluates it against [data] (or against no data at all, when null),
     * returning the result as a [JsonElement].
     */
    fun apply(rule: String, data: JsonElement? = null): JsonElement = apply(parse(rule), data)

    /**
     * Parses [rule] and evaluates it against [data] (or against no data at all, when null),
     * returning the result as a [JsonElement].
     */
    fun apply(rule: JsonElement, data: JsonElement?): JsonElement = apply(JsonLogicParser.parse(rule), data)

    /**
     * Evaluates the already-parsed [rule] against [data] (or against no data at all, when null),
     * returning the result as a [JsonElement]. The rule's root is reported as jsonPath `$`.
     */
    fun apply(rule: JsonLogicNode, data: JsonElement?): JsonElement {
        val value = data?.let(::jsonElementToValue)

        return valueToJsonElement(evaluator.evaluate(rule, value, "$"))
    }

    companion object {
        /** Reports whether [value] is truthy under JsonLogic's rules. */
        fun truthy(value: Any?): Boolean = isTruthy(value)
    }
}
