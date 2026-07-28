package co.branch.jsonlogic.evaluator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** Pins node dispatch, the operator table, argument pre-evaluation, and json-path threading. */
class JsonLogicEvaluatorTest {

    @Test
    fun primitiveNodesEvaluateToTheirValues() {
        assertEquals(17.0, evaluate("17", null))
        assertEquals(3.14, evaluate("3.14", null))
        assertEquals("apple", evaluate("\"apple\"", null))
        assertEquals(true, evaluate("true", null))
        assertEquals(false, evaluate("false", null))
        assertNull(evaluate("null", null))
    }

    @Test
    fun arrayNodeEvaluatesToAListOfItsElements() {
        assertEquals(listOf(1.0, "a", true, null), evaluate("""[1, "a", true, null]""", null))
        assertEquals(emptyList<Any?>(), evaluate("[]", null))
    }

    @Test
    fun arrayElementsShareTheEnclosingData() {
        assertEquals(listOf("x", "x"), evaluate("""[{"var": "a"}, {"var": "a"}]""", mapOf("a" to "x")))
    }

    @Test
    fun unknownOperatorFailsWhereItAppears() {
        val exception = assertFailsWith<JsonLogicEvaluationException> {
            evaluate("""{"nope": [1]}""", null)
        }

        assertEquals("Undefined operation 'nope'", exception.message)
        assertEquals("$", exception.jsonPath)
    }

    @Test
    fun jsonPathGrowsAsEvaluationDescends() {
        val nested = assertFailsWith<JsonLogicEvaluationException> {
            evaluate("""{"missing": [1, {"nope": 1}]}""", null)
        }

        assertEquals("Undefined operation 'nope'", nested.message)
        assertEquals("$.missing[1]", nested.jsonPath)

        val variable = assertFailsWith<JsonLogicEvaluationException> {
            evaluate("""{"missing": [{"var": [[1, 2]]}]}""", emptyMap<String, Any?>())
        }

        assertEquals("$.missing[0].var[0]", variable.jsonPath)
    }

    @Test
    fun operationsAreCalledWithTheirOwnJsonPath() {
        val probe = RecordingExpression()

        evaluate("""{"probe": [1]}""", null, listOf(probe))

        assertEquals("$.probe", probe.jsonPath)
    }

    @Test
    fun operationsSeeTheEnclosingData() {
        val probe = RecordingExpression()
        val data = mapOf("a" to 1)

        evaluate("""{"probe": [1]}""", data, listOf(probe))

        assertEquals(data, probe.data)
    }

    @Test
    fun preEvaluatedArgumentsUnwrapASoleListArgument() {
        val probe = RecordingExpression()

        evaluate("""{"probe": [[1, 2]]}""", null, listOf(probe))
        assertEquals(listOf(1.0, 2.0), probe.arguments)

        evaluate("""{"probe": [1, 2]}""", null, listOf(probe))
        assertEquals(listOf(1.0, 2.0), probe.arguments)

        evaluate("""{"probe": "x"}""", null, listOf(probe))
        assertEquals(listOf("x"), probe.arguments)

        evaluate("""{"probe": [[1, 2], [3]]}""", null, listOf(probe))
        assertEquals(listOf(listOf(1.0, 2.0), listOf(3.0)), probe.arguments)
    }

    @Test
    fun unwrappingASoleListArgumentNormalizesItsNumbers() {
        val probe = RecordingExpression()

        evaluate("""{"probe": {"var": ""}}""", listOf(1, 2L), listOf(probe))

        assertEquals(listOf(1.0, 2.0), probe.arguments)
    }

    @Test
    fun transformNormalizesEveryNumberToDouble() {
        assertEquals(1.0, JsonLogicEvaluator.transform(1))
        assertEquals(1.0, JsonLogicEvaluator.transform(1L))
        assertEquals(1.5, JsonLogicEvaluator.transform(1.5f))
        assertEquals(1.0, JsonLogicEvaluator.transform(1.0))
        assertEquals("1", JsonLogicEvaluator.transform("1"))
        assertEquals(true, JsonLogicEvaluator.transform(true))
        assertNull(JsonLogicEvaluator.transform(null))
        assertEquals(listOf(1), JsonLogicEvaluator.transform(listOf(1)))
    }

    @Test
    fun theOperatorTableIsIndexedByExpressionKey() {
        val expressions = listOf(ConstantExpression("one", 1.0), ConstantExpression("two", 2.0))

        assertEquals(1.0, evaluate("""{"one": []}""", null, expressions))
        assertEquals(2.0, evaluate("""{"two": []}""", null, expressions))
    }
}
