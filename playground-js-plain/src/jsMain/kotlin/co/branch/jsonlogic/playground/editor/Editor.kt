package co.branch.jsonlogic.playground.editor

import co.branch.jsonlogic.playground.ui.clear
import co.branch.jsonlogic.playground.ui.el
import co.branch.jsonlogic.playground.ui.withText
import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLTextAreaElement

/**
 * A transparent textarea laid exactly over a highlighted `<pre>` of the same text. The `<pre>` is
 * the only element in flow, so it sizes the pair together and the caret always lands on the glyph
 * under it. One shared scroll container holds the gutter as well, so nothing needs scroll sync.
 */
internal class Editor(private val onChange: (String) -> Unit) {

    val root: HTMLElement = el("div", "editor")

    private val gutter = el("div", "gutter")
    private val highlight = el("pre", "highlight")
    private val textArea = document.createElement("textarea") as HTMLTextAreaElement

    init {
        textArea.spellcheck = false
        textArea.setAttribute("autocapitalize", "off")
        textArea.setAttribute("autocomplete", "off")
        textArea.setAttribute("wrap", "off")
        textArea.addEventListener("input", { handleInput() })

        val code = el("div", "code")
        code.appendChild(highlight)
        code.appendChild(textArea)

        val row = el("div", "editor-row")
        row.appendChild(gutter)
        row.appendChild(code)
        root.appendChild(row)
    }

    /** Loads the first text, where there is no history to keep and no reason to take focus. */
    fun setInitialText(text: String) {
        textArea.value = text
        render()
    }

    /**
     * Replaces the whole text through the browser's own editing pipeline, so that the replacement
     * joins the undo stack rather than emptying it. Assigning `value` discards every entry, which
     * would make each example chip and each operation row a one-way door out of whatever the
     * reader had typed. Any later editing feature — Tab-to-indent is the obvious one — has to come
     * through here for the same reason, never through `preventDefault` and an assignment.
     */
    fun setText(text: String) {
        if (textArea.value == text) return

        // Whether a button takes focus when it is clicked differs by engine, so focus is put back
        // where it was; that is what keeps a chip tap from raising the keyboard on a phone.
        val focused = document.activeElement as? HTMLElement
        textArea.focus()
        textArea.setSelectionRange(0, textArea.value.length)

        // Inserting an empty string is a documented no-op in some engines, so emptying the field
        // is a deletion rather than an insertion of nothing.
        document.execCommand(if (text.isEmpty()) "delete" else "insertText", false, text)

        // The command can be refused, and it can be accepted and still do nothing, so the text
        // itself is the post-condition rather than the reported outcome. Correct text is worth
        // more than an undo entry. The command fires `input` on its own; an assignment does not.
        if (textArea.value != text) {
            textArea.value = text
            handleInput()
        }

        textArea.setSelectionRange(0, 0)
        root.scrollTop = 0.0
        focused?.focus()
    }

    /**
     * Reads the field instead of being handed the new text, so that nothing on the input path can
     * write `value` or the selection. Either write ends an in-progress composition, and a handler
     * with no text to write cannot make that mistake by accident.
     */
    private fun handleInput() {
        render()
        onChange(textArea.value)
    }

    private fun render() {
        val text = textArea.value
        gutter.textContent = gutterText(text)
        highlight.clear()
        highlightInto(highlight, text)
        // <pre> generates no line box for a trailing newline; the textarea does. A zero-width
        // character on the last line keeps the two the same height.
        highlight.appendChild(document.createTextNode("​"))
    }
}

private fun gutterText(text: String): String {
    val lines = text.count { it == '\n' } + 1
    val width = lines.toString().length

    return (1..lines).joinToString("\n") { it.toString().padStart(width) }
}

/**
 * Appends [text] as one styled span per token, leaving the characters themselves untouched. The
 * result panel colours its output through the same walk, which is why this is not the editor's own.
 */
internal fun highlightInto(host: Element, text: String) {
    var cursor = 0

    JsonTokenizer.tokenize(text).forEach { token ->
        // Whitespace produces no token, so the gaps between them are appended unstyled.
        if (token.start > cursor) {
            host.appendChild(document.createTextNode(text.substring(cursor, token.start)))
        }
        host.appendChild(el("span", token.kind.cssClass).withText(text.substring(token.start, token.endExclusive)))
        cursor = token.endExclusive
    }

    if (cursor < text.length) {
        host.appendChild(document.createTextNode(text.substring(cursor)))
    }
}

private val JsonTokenKind.cssClass: String
    get() = when (this) {
        JsonTokenKind.Key -> "tok-key"
        JsonTokenKind.StringValue -> "tok-string"
        JsonTokenKind.Number -> "tok-number"
        JsonTokenKind.Literal -> "tok-literal"
        JsonTokenKind.Punctuation -> "tok-punctuation"
        JsonTokenKind.Unknown -> "tok-invalid"
    }
