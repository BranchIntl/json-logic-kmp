package co.branch.jsonlogic

import co.branch.jsonlogic.ast.JsonLogicNode
import co.branch.jsonlogic.ast.JsonLogicParseException
import co.branch.jsonlogic.ast.JsonLogicParser
import co.branch.jsonlogic.evaluator.JsonLogicEvaluationException
import co.branch.jsonlogic.evaluator.expressions.PreEvaluatedArgumentsExpression
import co.branch.jsonlogic.fixtures.jsonSemanticEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Exercises [JsonLogic]'s public surface directly, independent of the fixture corpus. */
class JsonLogicApiTest {

    /** One evaluable rule per key the default constructor registers, in the same order as [JsonLogic]'s init block. */
    private val ruleByOperator = linkedMapOf(
        "+" to """{"+": [1, 2]}""",
        "-" to """{"-": [3, 1]}""",
        "*" to """{"*": [2, 3]}""",
        "/" to """{"/": [6, 2]}""",
        "%" to """{"%": [5, 2]}""",
        "min" to """{"min": [1, 2]}""",
        "max" to """{"max": [1, 2]}""",
        ">" to """{">": [2, 1]}""",
        ">=" to """{">=": [2, 2]}""",
        "<" to """{"<": [1, 2]}""",
        "<=" to """{"<=": [2, 2]}""",
        "if" to """{"if": [true, 1, 2]}""",
        "?:" to """{"?:": [true, 1, 2]}""",
        "==" to """{"==": [1, 1]}""",
        "!=" to """{"!=": [1, 2]}""",
        "===" to """{"===": [1, 1]}""",
        "!==" to """{"!==": [1, 2]}""",
        "!" to """{"!": [true]}""",
        "!!" to """{"!!": [true]}""",
        "and" to """{"and": [true, true]}""",
        "or" to """{"or": [false, true]}""",
        "log" to """{"log": ["hi"]}""",
        "map" to """{"map": [[1, 2], {"+": [{"var": ""}, 1]}]}""",
        "filter" to """{"filter": [[1, 2], {">": [{"var": ""}, 1]}]}""",
        "reduce" to """{"reduce": [[1, 2], {"+": [{"var": "current"}, {"var": "accumulator"}]}, 0]}""",
        "all" to """{"all": [[1, 2], {">": [{"var": ""}, 0]}]}""",
        "some" to """{"some": [[1, 2], {">": [{"var": ""}, 1]}]}""",
        "none" to """{"none": [[1, 2], {">": [{"var": ""}, 5]}]}""",
        "merge" to """{"merge": [1, [2, 3]]}""",
        "in" to """{"in": [1, [1, 2]]}""",
        "cat" to """{"cat": ["a", "b"]}""",
        "substr" to """{"substr": ["hello", 1]}""",
        "missing" to """{"missing": ["a"]}""",
        "missing_some" to """{"missing_some": [1, ["a", "b"]]}""",
    )

    @Test
    fun defaultConstructorRegistersAllThirtyFourOperations() {
        assertEquals(34, ruleByOperator.size, "one rule per operation JsonLogic() registers by default")

        val jsonLogic = JsonLogic()

        for ((operator, rule) in ruleByOperator) {
            try {
                jsonLogic.apply(rule, null)
            } catch (e: Exception) {
                throw AssertionError("operator '$operator' failed to evaluate against rule=$rule", e)
            }
        }
    }

    @Test
    fun addOperation_lambdaOverloadRegistersACustomOperator() {
        val jsonLogic = JsonLogic()

        jsonLogic.addOperation("greet") { args -> "Hello ${args[0]}!" }

        val result = jsonLogic.apply("""{"greet": ["json-logic"]}""", null)

        assertEquals("Hello json-logic!", result.jsonPrimitive.content)
    }

    @Test
    fun addOperation_lambdaOverloadAcceptsAnUppercaseKey() {
        val jsonLogic = JsonLogic()

        jsonLogic.addOperation("Greet") { args -> "Hello ${args[0]}!" }

        val result = jsonLogic.apply("""{"Greet": ["json-logic"]}""", null)

        assertEquals("Hello json-logic!", result.jsonPrimitive.content)
    }

    @Test
    fun addOperation_expressionOverloadRegistersACustomOperator() {
        val jsonLogic = JsonLogic()
        val double = object : PreEvaluatedArgumentsExpression {
            override val key: String = "double"
            override fun evaluate(arguments: List<Any?>, data: Any?, jsonPath: String): Any? =
                (arguments[0] as Double) * 2
        }

        jsonLogic.addOperation(double)

        val result = jsonLogic.apply("""{"double": [21]}""", null)

        assertEquals(42.0, result.jsonPrimitive.content.toDouble())
    }

    @Test
    fun addOperation_returnsThisForChaining() {
        val jsonLogic = JsonLogic()

        val returned = jsonLogic.addOperation("noop") { it }

        assertTrue(returned === jsonLogic, "addOperation must return the same instance it was called on")
    }

    /**
     * Pins the engine this library ports: its `expressions` field is a plain `Map`, and
     * `addOperation` calls `Map.put`, so two registrations under the same key resolve last-wins —
     * the second registration's expression is the one later `apply` calls reach.
     */
    @Test
    fun addOperation_duplicateKeyResolvesLastWins() {
        val jsonLogic = JsonLogic()

        jsonLogic.addOperation("dup") { "first" }
        jsonLogic.addOperation("dup") { "second" }

        val result = jsonLogic.apply("""{"dup": []}""", null)

        assertEquals("second", result.jsonPrimitive.content)
    }

    @Test
    fun addOperation_duplicateKeyAgainstADefaultOperationOverridesIt() {
        val jsonLogic = JsonLogic()

        // "+" is registered by the default constructor; re-registering it must override, not stack.
        jsonLogic.addOperation("+") { "overridden" }

        val result = jsonLogic.apply("""{"+": [1, 2]}""", null)

        assertEquals("overridden", result.jsonPrimitive.content)
    }

    @Test
    fun parseThenApplyPreParsedNodeMatchesApplyingTheString() {
        val jsonLogic = JsonLogic()
        val rule = """{"+": [{"var": "a"}, {"var": "b"}]}"""
        val data = Json.parseToJsonElement("""{"a": 1, "b": 2}""")

        val node: JsonLogicNode = jsonLogic.parse(rule)
        val viaPreParsedNode = jsonLogic.apply(node, data)
        val viaStringConvenience = jsonLogic.apply(rule, data)

        assertTrue(jsonSemanticEquals(viaPreParsedNode, viaStringConvenience))
        assertEquals(3.0, viaPreParsedNode.jsonPrimitive.content.toDouble())
    }

    /** A rule that already is a [kotlinx.serialization.json.JsonElement] pre-parses off the instance too. */
    @Test
    fun parseThenApplyPreParsedNodeMatchesApplyingTheJsonElement() {
        val jsonLogic = JsonLogic()
        val rule = Json.parseToJsonElement("""{"+": [{"var": "a"}, {"var": "b"}]}""")
        val data = Json.parseToJsonElement("""{"a": 1, "b": 2}""")

        val node: JsonLogicNode = jsonLogic.parse(rule)

        assertTrue(jsonSemanticEquals(jsonLogic.apply(node, data), jsonLogic.apply(rule, data)))
        assertEquals(node, jsonLogic.parse(rule, maxDepth = JsonLogicParser.DEFAULT_MAX_DEPTH))
    }

    @Test
    fun parseRejectsARuleDeeperThanTheGivenBound() {
        val jsonLogic = JsonLogic()
        val rule = """{"+": [1, {"+": [1, 1]}]}"""

        assertFailsWith<JsonLogicParseException> { jsonLogic.parse(rule, maxDepth = 2) }
        assertFailsWith<JsonLogicParseException> {
            jsonLogic.parse(Json.parseToJsonElement(rule), maxDepth = 2)
        }
    }

    @Test
    fun applyStringConvenienceDefaultsDataToNull() {
        val jsonLogic = JsonLogic()

        val result = jsonLogic.apply("""{"var": ["missing", "fallback"]}""")

        assertEquals("fallback", result.jsonPrimitive.content)
    }

    @Test
    fun applyJsonElementRuleParsesAndEvaluates() {
        val jsonLogic = JsonLogic()
        val ruleElement = Json.parseToJsonElement("""{"==": [1, 1]}""")

        val result = jsonLogic.apply(ruleElement, null)

        assertEquals("true", result.jsonPrimitive.content)
    }

    @Test
    fun applyWithNullDataAndWithExplicitJsonNullBehaveTheSame() {
        val jsonLogic = JsonLogic()
        val rule = """{"var": ["a", "fallback"]}"""

        val withKotlinNull = jsonLogic.apply(rule, null)
        val withExplicitJsonNull = jsonLogic.apply(rule, JsonNull)

        assertTrue(jsonSemanticEquals(withKotlinNull, withExplicitJsonNull))
        assertEquals("fallback", withKotlinNull.jsonPrimitive.content)
    }

    @Test
    fun truthyCompanionMatchesInternalTruthyRules() {
        assertFalse(JsonLogic.truthy(null))
        assertFalse(JsonLogic.truthy(false))
        assertTrue(JsonLogic.truthy(true))
        assertFalse(JsonLogic.truthy(0.0))
        assertTrue(JsonLogic.truthy(1.0))
        assertFalse(JsonLogic.truthy(Double.NaN))
        assertTrue(JsonLogic.truthy(Double.POSITIVE_INFINITY))
        assertFalse(JsonLogic.truthy(""))
        assertTrue(JsonLogic.truthy("a"))
        assertFalse(JsonLogic.truthy(emptyList<Any?>()))
        assertTrue(JsonLogic.truthy(listOf(1.0)))
        assertTrue(JsonLogic.truthy(emptyMap<Any?, Any?>()))
    }

    @Test
    fun undefinedOperationThrowsWithUpstreamMessageAndRootJsonPath() {
        val jsonLogic = JsonLogic()

        val exception = runCatching { jsonLogic.apply("""{"nope": [1, 2]}""", null) }
            .exceptionOrNull()

        assertTrue(exception is JsonLogicEvaluationException, "expected a JsonLogicEvaluationException, got $exception")
        assertEquals("Undefined operation 'nope'", exception.message)
        assertEquals("$", exception.jsonPath)
    }
}
