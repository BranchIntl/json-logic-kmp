package co.branch.jsonlogic.playground

import co.branch.jsonlogic.playground.share.ShareLink
import co.branch.jsonlogic.playground.share.SharedState
import kotlinx.browser.document
import kotlinx.browser.window
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
    fun addHost() {
        host = document.createElement("div") as HTMLElement
        host.id = "playground"
        document.body?.appendChild(host)
    }

    /** The fragment outlives the page Karma runs every suite on, so it is cleared rather than left. */
    @AfterTest
    fun removeHost() {
        host.remove()
        window.location.hash = ""
    }

    @Test
    fun pickingAnOperationReplacesTheRuleAndEmptiesTheData() {
        mount()
        val snippet = OperationGroups.first().operations.first().snippet
        assertTrue(dataField().value.isNotEmpty(), "an empty data editor would make this vacuous")

        operationRows().first().click()

        assertEquals(snippet, ruleField().value)
        assertEquals("", dataField().value)
    }

    @Test
    fun theExampleTheAppOpensWithIsMarkedOnTheChipRow() {
        mount()

        assertEquals(listOf(Examples.first().label), selectedChipLabels())
    }

    /**
     * A textarea normalises CRLF away, so a link carrying it and the field that received it hold
     * different text, and everything derived from the link — the chip row here, the next shared
     * link in general — has to follow the field.
     */
    @Test
    fun aSharedLinkIsTakenAsTheFieldsReceivedIt() {
        val example = Examples[1]
        val carried = SharedState(example.rule.withCrlf(), example.data.withCrlf())
        window.location.hash = ShareLink.encode(carried)

        mount()

        assertEquals(listOf(example.label), selectedChipLabels())
    }

    @Test
    fun theOperationsDisclosureSaysWhetherItIsOpen() {
        mount()
        val toggle = host.querySelector(".ops-toggle") as HTMLElement
        assertEquals("false", toggle.getAttribute("aria-expanded"))

        toggle.click()

        assertEquals("true", toggle.getAttribute("aria-expanded"))
    }

    private fun mount() = Playground().mount()

    private fun selectedChipLabels(): List<String> =
        host.querySelectorAll(".examples .chip.selected").asList().map { it.textContent.orEmpty() }

    private fun operationRows(): List<HTMLElement> =
        host.querySelectorAll(".op-row").asList().map { it as HTMLElement }

    private fun ruleField(): HTMLTextAreaElement = field(0)

    private fun dataField(): HTMLTextAreaElement = field(1)

    private fun field(index: Int): HTMLTextAreaElement =
        host.querySelectorAll(".code textarea").asList()[index] as HTMLTextAreaElement
}

private fun String.withCrlf(): String = replace("\n", "\r\n")
