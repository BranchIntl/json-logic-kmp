package co.branch.jsonlogic.playground.editor

import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.asList
import org.w3c.dom.events.Event
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class EditorTest {

    private val changes = mutableListOf<String>()
    private val inputTypes = mutableListOf<String>()

    private lateinit var editor: Editor
    private lateinit var field: HTMLTextAreaElement

    /** Stands in for an example chip, the kind of control that asks for a replacement. */
    private lateinit var chip: HTMLElement

    @BeforeTest
    fun mountEditor() {
        editor = Editor("Rule") { changes.add(it) }
        document.body?.appendChild(editor.root)
        field = editor.root.querySelector("textarea") as HTMLTextAreaElement
        field.addEventListener("input", { inputTypes.add(it.inputType) })
        chip = document.createElement("button") as HTMLElement
        document.body?.appendChild(chip)
    }

    /** Removing both focusable elements hands focus back to `<body>` for the next test. */
    @AfterTest
    fun unmountEditor() {
        editor.root.remove()
        chip.remove()
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

    /**
     * A reader undoes with Cmd-Z, and Karma cannot deliver one: a key event built in script is
     * untrusted, so it runs no editing command, and the command a real Cmd-Z carries is attached
     * to the event by the browser outside the page. What is reachable is the stack that shortcut
     * pops, so this covers the entry being on it and the edit coming back — not the shortcut
     * itself, which was measured against the built page rather than here.
     *
     * The replacement drops the focus it borrowed, so undoing from here also covers the entry
     * surviving that.
     */
    @Test
    fun theReplacementCanBeUndone() {
        editor.setInitialText("before")
        (document.activeElement as? HTMLElement)?.blur()

        editor.setText("after")
        assertNotEquals<Any?>(field, document.activeElement, "the field kept focus, so nothing is blurred")
        document.execCommand("undo", false, "")

        assertEquals("before", field.value)
        // The model and the highlighted copy follow an undo only because the command fires input.
        assertEquals("before", changes.last())
    }

    /**
     * An engine that does not focus a button when one is tapped leaves `<body>` holding focus, and
     * focus cannot be moved onto a `<body>`, so a field that kept what it borrowed for the command
     * would raise a phone's keyboard on every chip and every operation row.
     */
    @Test
    fun theReplacementLeavesAnUnfocusedFieldUnfocused() {
        editor.setInitialText("before")
        (document.activeElement as? HTMLElement)?.blur()

        editor.setText("after")

        assertNotEquals<Any?>(field, document.activeElement)
    }

    @Test
    fun theReplacementPutsFocusBackOnTheControlThatAskedForIt() {
        editor.setInitialText("before")
        chip.focus()

        editor.setText("after")

        assertEquals<Any?>(chip, document.activeElement)
    }

    @Test
    fun theReplacementKeepsFocusInAFieldThatHadIt() {
        editor.setInitialText("before")
        field.focus()

        editor.setText("after")

        assertEquals<Any?>(field, document.activeElement)
    }

    private fun lineNumbers(): List<String> =
        editor.root.querySelectorAll(".line .ln").asList().map { it.textContent.orEmpty() }
}

private val Event.inputType: String
    get() = asDynamic().inputType as String
