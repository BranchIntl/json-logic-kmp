package co.branch.jsonlogic.playground

import co.branch.jsonlogic.JsonLogic
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OperationsTest {

    /**
     * Every snippet must evaluate against no data at all, which is the state the rule editor is in
     * when someone clicks one. It also proves the operator is actually registered: an unregistered
     * one fails evaluation rather than silently returning something.
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

    /**
     * The reference claims to list every registered operation. `var` is excluded from the count
     * because the parser treats it as rule syntax rather than registering it.
     */
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
