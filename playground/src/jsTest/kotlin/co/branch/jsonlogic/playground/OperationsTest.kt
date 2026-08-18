package co.branch.jsonlogic.playground

import co.branch.jsonlogic.JsonLogic
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OperationsTest {

    /**
     * No data is the state the rule editor is in when someone clicks a snippet. Evaluating also
     * proves the operator is registered, since an unregistered one fails.
     */
    @Test
    fun everySnippetEvaluatesWithoutData() {
        val jsonLogic = JsonLogic()

        AllOperations.forEach { operation ->
            val evaluation = evaluate(jsonLogic, operation.snippet, "")

            assertTrue(
                evaluation.outcome is EvalOutcome.Success,
                "${operation.symbol}: ${(evaluation.outcome as? EvalOutcome.Failure)?.detail}",
            )
        }
    }

    /** `var` is excluded from the count: the parser treats it as rule syntax, not an operation. */
    @Test
    fun listsAllThirtyFourRegisteredOperations() {
        val registered = AllOperations.filterNot { it.symbol == "var" }

        assertEquals(34, registered.size)
    }

    @Test
    fun symbolsAreUnique() {
        val symbols = AllOperations.map { it.symbol }

        assertEquals(symbols.size, symbols.toSet().size, "operator symbols must be distinct")
    }
}
