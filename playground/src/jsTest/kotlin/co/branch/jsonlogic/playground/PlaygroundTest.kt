package co.branch.jsonlogic.playground

import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.asList
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Drives the mounted app through the DOM, by the same route a reader's click takes. */
class PlaygroundTest {

    private lateinit var host: HTMLElement

    @BeforeTest
    fun mountApp() {
        host = document.createElement("div") as HTMLElement
        host.id = "playground"
        document.body?.appendChild(host)
        Playground().mount()
    }

    @AfterTest
    fun unmountApp() {
        host.remove()
    }

    @Test
    fun pickingAnOperationReplacesTheRuleAndEmptiesTheData() {
        val snippet = OperationGroups.first().operations.first().snippet
        assertTrue(dataField().value.isNotEmpty(), "an empty data editor would make this vacuous")

        operationRows().first().click()

        assertEquals(snippet, ruleField().value)
        assertEquals("", dataField().value)
    }

    private fun operationRows(): List<HTMLElement> =
        host.querySelectorAll(".op-row").asList().map { it as HTMLElement }

    private fun ruleField(): HTMLTextAreaElement = field(0)

    private fun dataField(): HTMLTextAreaElement = field(1)

    private fun field(index: Int): HTMLTextAreaElement =
        host.querySelectorAll(".code textarea").asList()[index] as HTMLTextAreaElement
}
