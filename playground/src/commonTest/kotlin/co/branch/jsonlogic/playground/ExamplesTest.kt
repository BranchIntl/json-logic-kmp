package co.branch.jsonlogic.playground

import co.branch.jsonlogic.JsonLogic
import co.branch.jsonlogic.playground.ui.prettyPrint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExamplesTest {

    /** A preset that does not evaluate greets the reader with an error card. */
    @Test
    fun everyExampleEvaluates() {
        val jsonLogic = JsonLogic()

        Examples.forEach { example ->
            val evaluation = evaluate(jsonLogic, example.rule, example.data)

            assertTrue(evaluation.ruleValid, "${example.label}: rule did not parse")
            assertTrue(evaluation.dataValid, "${example.label}: data did not parse")
            assertTrue(
                evaluation.outcome is EvalOutcome.Success,
                "${example.label}: ${(evaluation.outcome as? EvalOutcome.Failure)?.detail}",
            )
        }
    }

    /** A preset whose output changes has stopped demonstrating the thing its label claims. */
    @Test
    fun examplesProduceTheirDocumentedResults() {
        val expected = mapOf(
            "Range check" to "true",
            "Variables" to "\"London\"",
            "Branching" to "\"B\"",
            "Map" to "[\n  1250,\n  700\n]",
            "Filter and reduce" to "19.5",
            "Membership" to "true",
            "Strings" to "\"KMP-0727\"",
            "Missing fields" to "[\n  \"email\",\n  \"phone\"\n]",
            "Truthiness" to "false",
        )
        val jsonLogic = JsonLogic()

        assertEquals(expected.keys, Examples.map { it.label }.toSet())

        val actual = Examples.associate { example ->
            val outcome = evaluate(jsonLogic, example.rule, example.data).outcome
            example.label to (outcome as? EvalOutcome.Success)?.value?.let(::prettyPrint)
        }

        assertEquals(expected, actual)
    }

    @Test
    fun labelsAreUnique() {
        val labels = Examples.map { it.label }

        assertEquals(labels.size, labels.toSet().size, "example labels must be distinct")
    }
}
