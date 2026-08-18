package co.branch.jsonlogic.playground.editor

import co.branch.jsonlogic.playground.Stylesheet
import co.branch.jsonlogic.playground.ui.el
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLStyleElement
import org.w3c.dom.HTMLTextAreaElement
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The editor's whole design rests on the browser breaking the same text at the same places in two
 * elements, which only the shipped declarations decide, so this suite installs them and measures.
 */
class EditorLayoutTest {

    private lateinit var style: HTMLStyleElement
    private lateinit var host: HTMLElement
    private lateinit var editor: Editor

    @BeforeTest
    fun mountEditor() {
        style = document.createElement("style") as HTMLStyleElement
        style.textContent = Stylesheet
        document.head?.appendChild(style)

        // Narrow, and free to be as tall as it likes, so a long line has to wrap and the layers
        // are measured at their natural heights.
        host = el("div")
        host.style.width = "320px"
        document.body?.appendChild(host)

        editor = Editor("Rule") {}
        host.appendChild(editor.root)
    }

    @AfterTest
    fun unmountEditor() {
        host.remove()
        style.remove()
    }

    @Test
    fun theLayersAgreeOnTheHeightOfAWrappedLine() {
        editor.setInitialText("x")
        val oneLine = highlightHeight()

        editor.setInitialText("x".repeat(300))

        assertTrue(highlightHeight() > 3 * oneLine, "the unbroken token has to wrap for this to mean anything")
        assertTrue(
            abs(highlightHeight() - fieldHeight()) <= 1.0,
            "highlight ${highlightHeight()} against textarea ${fieldHeight()}",
        )
    }

    private fun highlightHeight(): Double =
        (editor.root.querySelector(".highlight") as HTMLElement).getBoundingClientRect().height

    /**
     * The field is stretched to the highlight layer's box, so its own content height only shows
     * once that box is collapsed — otherwise a field that wrapped to fewer rows reads as agreeing.
     */
    private fun fieldHeight(): Double {
        val field = editor.root.querySelector("textarea") as HTMLTextAreaElement
        field.style.height = "0"
        val height = field.scrollHeight.toDouble()
        field.style.height = ""

        return height
    }
}
