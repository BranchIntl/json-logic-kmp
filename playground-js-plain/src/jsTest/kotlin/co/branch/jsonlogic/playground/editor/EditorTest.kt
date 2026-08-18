package co.branch.jsonlogic.playground.editor

import kotlinx.browser.document
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.asList
import org.w3c.dom.events.Event
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class EditorTest {

    private val changes = mutableListOf<String>()
    private val inputTypes = mutableListOf<String>()

    private lateinit var editor: Editor
    private lateinit var field: HTMLTextAreaElement

    @BeforeTest
    fun mountEditor() {
        editor = Editor("Rule") { changes.add(it) }
        document.body?.appendChild(editor.root)
        field = editor.root.querySelector("textarea") as HTMLTextAreaElement
        field.addEventListener("input", { inputTypes.add(it.inputType) })
    }

    @AfterTest
    fun unmountEditor() {
        editor.root.remove()
    }

    @Test
    fun replacingTheTextIsAnInsertion() {
        editor.setInitialText("""{"a": 1}""")

        editor.setText("""{"b": 2}""")

        assertEquals("""{"b": 2}""", field.value)
        assertEquals(listOf("insertText"), inputTypes)
        assertEquals(listOf("""{"b": 2}"""), changes)
    }

    /** Inserting an empty string is a no-op in some engines, so emptying has to be a deletion. */
    @Test
    fun emptyingTheTextClearsTheField() {
        editor.setInitialText("""{"a": 1}""")

        editor.setText("")

        assertEquals("", field.value)
        assertEquals(1, inputTypes.size, "expected one edit, got $inputTypes")
        assertEquals(listOf(""), changes)
    }

    @Test
    fun everyLogicalLineIsNumbered() {
        editor.setInitialText("a\nbb\n")

        assertEquals(listOf("1", "2", "3"), lineNumbers())
    }

    /** A composition can end without a final input event, and the layers still have to agree. */
    @Test
    fun endingACompositionRepaintsFromTheField() {
        editor.setInitialText("")

        field.value = "あ"
        field.dispatchEvent(Event("compositionend"))

        assertEquals(listOf("あ"), changes)
        assertEquals(listOf("1"), lineNumbers())
    }

    @Test
    fun onlyTheFieldIsReadToAScreenReader() {
        assertEquals("Rule", field.getAttribute("aria-label"))
        assertEquals("true", editor.root.querySelector(".highlight")?.getAttribute("aria-hidden"))
    }

    @Test
    fun theFieldOptsOutOfTheKeyboardsCorrections() {
        assertEquals("off", field.getAttribute("autocorrect"))
        assertEquals("none", field.getAttribute("autocapitalize"))
        assertEquals("ltr", field.getAttribute("dir"))
    }

    @Test
    fun theReplacementCanBeUndone() {
        editor.setInitialText("before")

        editor.setText("after")
        field.focus()
        document.execCommand("undo", false, "")

        assertEquals("before", field.value)
    }

    private fun lineNumbers(): List<String> =
        editor.root.querySelectorAll(".line .ln").asList().map { it.textContent.orEmpty() }
}

private val Event.inputType: String
    get() = asDynamic().inputType as String
