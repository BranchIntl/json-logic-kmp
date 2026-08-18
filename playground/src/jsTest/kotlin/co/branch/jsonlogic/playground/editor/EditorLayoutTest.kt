package co.branch.jsonlogic.playground.editor

import co.branch.jsonlogic.playground.Stylesheet
import co.branch.jsonlogic.playground.ui.el
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLStyleElement
import org.w3c.dom.HTMLTextAreaElement
import kotlin.js.Promise
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/** The face the stylesheet declares, at the size the editors set. */
private const val MonospaceFace = """13px "JetBrains Mono""""

/**
 * The editor's whole design rests on the browser breaking the same text at the same places in two
 * elements, which only the shipped declarations decide, so this suite installs them and measures.
 * The face is one of those declarations and the one every measurement is made of, so its bytes
 * travel inside the stylesheet and nothing here measures until it has loaded.
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
    fun theLayersAgreeOnTheHeightOfAWrappedLine(): Promise<Unit> = withTheFaceLoaded {
        editor.setInitialText("x")
        val oneLine = highlightHeight()

        editor.setInitialText("x".repeat(300))

        assertTrue(highlightHeight() > 3 * oneLine, "the unbroken token has to wrap for this to mean anything")
        assertTrue(
            abs(highlightHeight() - fieldHeight()) <= 1.0,
            "highlight ${highlightHeight()} against textarea ${fieldHeight()}",
        )
    }

    /**
     * A face that does not arrive is substituted rather than reported, and the measurements above
     * would go on passing about some other font.
     */
    @Test
    fun theFaceTheStylesheetDeclaresIsTheOneLoaded(): Promise<Unit> = withTheFaceLoaded {
        val loaded: Boolean = document.asDynamic().fonts.check(MonospaceFace)

        assertTrue(loaded, "the stylesheet's own face is not the one being measured")
    }

    /** Fonts load asynchronously even with their bytes at hand, and a fallback measures differently. */
    private fun withTheFaceLoaded(measure: () -> Unit): Promise<Unit> {
        val loading: Promise<Array<Any?>> = document.asDynamic().fonts.load(MonospaceFace)

        return loading.then { measure() }
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
