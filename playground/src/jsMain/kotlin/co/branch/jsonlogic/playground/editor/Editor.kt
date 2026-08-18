package co.branch.jsonlogic.playground.editor

import co.branch.jsonlogic.playground.ui.clear
import co.branch.jsonlogic.playground.ui.el
import co.branch.jsonlogic.playground.ui.withText
import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.Node

/**
 * A transparent textarea laid exactly over a highlighted copy of the same text. The highlighted
 * copy is the only thing in flow, so it sizes the pair together and the caret always lands on the
 * glyph under it. Both layers soft-wrap at the same width, so neither one scrolls sideways.
 *
 * One block per logical line carries that line's number as an out-of-flow child, which puts the
 * number wherever the browser put the top of the line and asks nothing of this code.
 *
 * @param label what a screen reader announces the field as.
 */
internal class Editor(label: String, private val onChange: (String) -> Unit) {

    val root: HTMLElement = el("div", "editor")

    /**
     * What the field holds, which is not always what was written to it: a textarea normalises the
     * line endings of anything given to it, so text that arrived with CRLF is read back with LF.
     */
    val text: String get() = textArea.value

    private val highlight = el("div", "highlight")
    private val textArea = document.createElement("textarea") as HTMLTextAreaElement

    private var painted: String? = null
    private var gutterDigits = 0

    init {
        textArea.spellcheck = false
        textArea.setAttribute("autocomplete", "off")
        // A phone keyboard would otherwise capitalise the first word of a line and curl the
        // quotes, and neither a capital nor a typographic quote is JSON.
        textArea.setAttribute("autocorrect", "off")
        textArea.setAttribute("autocapitalize", "none")
        // The highlighted copy is laid out left to right; a field that took the reader's own
        // direction instead would break its lines somewhere else.
        textArea.setAttribute("dir", "ltr")
        textArea.setAttribute("aria-label", label)

        textArea.addEventListener("input", { handleInput() })
        // Insurance against an engine that ends a composition without a final input event. The
        // handler reads the field rather than the event, so a second run changes nothing.
        textArea.addEventListener("compositionend", { handleInput() })

        // This layer holds a copy of what the field holds, and a reader who is being read to
        // wants the rule once.
        highlight.setAttribute("aria-hidden", "true")

        val code = el("div", "code")
        code.appendChild(highlight)
        code.appendChild(textArea)
        root.appendChild(code)
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
     *
     * A replacement never leaves a reader in a field they were not already in: focus goes back to
     * whatever held it, and is dropped rather than kept when that hand-back does not take.
     */
    fun setText(text: String) {
        if (textArea.value == text) return

        // The command edits whatever holds focus, so the field has to hold it for the length of
        // the edit and hand it back afterwards — a reader who reached a chip by keyboard keeps
        // their place in the tab order.
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

        // Handing it back is not enough on its own. An engine that does not focus a button when
        // one is tapped leaves `activeElement` reporting the `<body>`, and `focus()` on a `<body>`
        // does nothing, so the field would keep what it borrowed — on a phone, the keyboard rising
        // over the result on every chip and every operation row.
        focused?.focus()
        if (focused != textArea && document.activeElement == textArea) textArea.blur()
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

    /** Repaints from the field, the one text both layers are certain to agree on. */
    private fun render() {
        val text = textArea.value
        if (text == painted) return
        painted = text

        val lines = highlightLines(text)
        sizeGutterFor(lines.size)

        val fragment = document.createDocumentFragment()
        lines.forEachIndexed { index, spans ->
            val line = el("div", "line")
            line.appendChild(el("span", "ln").withText((index + 1).toString()))
            spans.forEach { line.appendChild(it.node()) }
            fragment.appendChild(line)
        }

        highlight.clear()
        highlight.appendChild(fragment)
    }

    private fun sizeGutterFor(lines: Int) {
        val digits = lines.toString().length
        if (digits == gutterDigits) return

        gutterDigits = digits
        root.style.setProperty("--gutter-digits", digits.toString())
    }
}

/**
 * Appends [text] to [host] as one node per painted run, leaving the characters themselves
 * untouched. The result panel colours its output the same way, which is why this is not the
 * editor's own.
 */
internal fun highlightInto(host: Element, text: String) {
    highlightSpans(text).forEach { host.appendChild(it.node()) }
}

private fun HighlightSpan.node(): Node =
    kind?.let { el("span", it.cssClass).withText(text) } ?: document.createTextNode(text)

private val JsonTokenKind.cssClass: String
    get() = when (this) {
        JsonTokenKind.Key -> "tok-key"
        JsonTokenKind.StringValue -> "tok-string"
        JsonTokenKind.Number -> "tok-number"
        JsonTokenKind.Literal -> "tok-literal"
        JsonTokenKind.Punctuation -> "tok-punctuation"
        JsonTokenKind.Unknown -> "tok-invalid"
    }
