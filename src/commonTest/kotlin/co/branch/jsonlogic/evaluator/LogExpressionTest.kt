package co.branch.jsonlogic.evaluator

import co.branch.jsonlogic.evaluator.expressions.LogExpression
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Pins `log`'s argument-count check, its passthrough return value, and its sink rendering. */
class LogExpressionTest {

    @Test
    fun requiresExactlyOneArgument() {
        val exception = assertFailsWith<JsonLogicEvaluationException> {
            evaluate("""{"log": []}""", null, controlStringExpressions)
        }

        assertEquals("log operator requires exactly 1 argument", exception.message)
        assertEquals("$.log", exception.jsonPath)
    }

    @Test
    fun returnsItsArgumentUnchanged() {
        assertEquals("apple", evaluate("""{"log": ["apple"]}""", null, controlStringExpressions))
        assertEquals(1.0, evaluate("""{"log": [1]}""", null, controlStringExpressions))
    }

    @Test
    fun sinkReceivesTheJavaRenderingOfTheValue() {
        val messages = mutableListOf<String>()
        val log = LogExpression(sink = { messages.add(it) })
        val expressions = listOf(log)

        evaluate("""{"log": [1]}""", null, expressions)
        evaluate("""{"log": [1.5]}""", null, expressions)
        evaluate("""{"log": ["apple"]}""", null, expressions)
        evaluate("""{"log": [true]}""", null, expressions)
        evaluate("""{"log": [null]}""", null, expressions)

        assertEquals(
            listOf(
                "JsonLogic: 1.0",
                "JsonLogic: 1.5",
                "JsonLogic: apple",
                "JsonLogic: true",
                "JsonLogic: null",
            ),
            messages,
        )
    }

    @Test
    fun aListValueRendersInJavaCollectionFormatWithoutIntegerStripping() {
        // A sole list argument would be unwrapped into log's own argument list
        // (PreEvaluatedArgumentsExpression), so a second argument keeps the list intact as the
        // value under test here.
        val messages = mutableListOf<String>()
        val log = LogExpression(sink = { messages.add(it) })

        evaluate("""{"log": [[1, "a"], null]}""", null, listOf(log))

        assertEquals(listOf("JsonLogic: [1.0, a]"), messages)
    }

    @Test
    fun aMapValueRendersInJavaMapFormat() {
        val messages = mutableListOf<String>()
        val log = LogExpression(sink = { messages.add(it) })
        val data = mapOf("a" to 1.0, "b" to "x")

        evaluate("""{"log": [{"var": ""}, null]}""", data, listOf(log))

        assertEquals(listOf("JsonLogic: {a=1.0, b=x}"), messages)
    }

    @Test
    fun theDefaultSinkIsStdout() {
        // STDOUT is upstream's default PrintStream(System.out) instance ported to println; there is
        // nothing more specific to assert without capturing the process's real stdout, so this just
        // pins that the companion default exists and behaves like any other instance.
        assertEquals("apple", LogExpression.STDOUT.evaluate(listOf("apple"), null, "$"))
    }
}
