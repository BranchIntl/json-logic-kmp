package co.branch.jsonlogic.playground

import co.branch.jsonlogic.JsonLogic
import co.branch.jsonlogic.playground.editor.Editor
import co.branch.jsonlogic.playground.editor.highlightInto
import co.branch.jsonlogic.playground.share.ShareLink
import co.branch.jsonlogic.playground.share.SharedState
import co.branch.jsonlogic.playground.ui.ChevronIcon
import co.branch.jsonlogic.playground.ui.MoonIcon
import co.branch.jsonlogic.playground.ui.SunIcon
import co.branch.jsonlogic.playground.ui.button
import co.branch.jsonlogic.playground.ui.clear
import co.branch.jsonlogic.playground.ui.el
import co.branch.jsonlogic.playground.ui.icon
import co.branch.jsonlogic.playground.ui.withText
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.get

private const val RepositoryUrl = "https://github.com/BranchIntl/json-logic-kmp"

private const val Subtitle =
    "json-logic-kmp compiled to JavaScript — the same evaluator that ships to JVM, Android and iOS."

private const val FootnoteText =
    "Every number is an IEEE 754 double, so 0.1 + 0.2 evaluates to 0.30000000000000004. " +
        "Infinity and NaN come back as bare literals, which JSON has no token for."

/**
 * The whole app, wired by hand: every piece of derived UI has an explicit update path, since there
 * is no runtime to diff a rendered tree against the previous one.
 */
internal class Playground {

    // Built once: the constructor registers all 34 default operations.
    private val jsonLogic = JsonLogic()

    private val shared = ShareLink.decode(window.location.hash)
    private var ruleText = shared?.rule ?: Examples.first().rule
    private var dataText = shared?.data ?: Examples.first().data
    private val systemDark = window.matchMedia("(prefers-color-scheme: dark)")
    private var darkOverride: Boolean? = null
    private var referenceExpanded = false
    private var debounce = 0
    private var shareReset = 0

    private lateinit var ruleEditor: Editor
    private lateinit var dataEditor: Editor
    private lateinit var ruleStatus: HTMLElement
    private lateinit var dataStatus: HTMLElement
    private lateinit var resultBody: HTMLElement
    private lateinit var resultType: HTMLElement
    private lateinit var themeButton: HTMLElement
    private lateinit var shareButton: HTMLElement
    private lateinit var exampleChips: List<Pair<Example, HTMLElement>>
    private lateinit var opsToggle: HTMLElement
    private lateinit var opsBody: HTMLElement

    fun mount() {
        val root = document.getElementById("playground") ?: return
        root.clear()

        val app = el("div", "app")
        app.appendChild(header())
        app.appendChild(examplesRow())
        app.appendChild(editors())
        app.appendChild(operationsReference())
        app.appendChild(el("div", "footnote").withText(FootnoteText))
        root.appendChild(app)

        applyTheme()
        // Someone who changes their appearance setting at sundown has said what they want, and a
        // page left open since morning should hear it. Until they say otherwise here: a reader who
        // has used the toggle has said something more specific, which the system does not override.
        systemDark.addEventListener("change", { if (darkOverride == null) applyTheme() })

        ruleEditor.setInitialText(ruleText)
        dataEditor.setInitialText(dataText)
        // The fields, not the strings handed to them, are what everything downstream reads: a
        // textarea normalises the line endings it is given, so a shared link carrying CRLF would
        // otherwise leave this holding text the reader cannot see, and Share handing it back out.
        ruleText = ruleEditor.text
        dataText = dataEditor.text

        // The load path writes the fields without going through their input handler, so the two
        // things that handler keeps current are brought up to date here instead.
        syncExampleChips()
        evaluateNow()
    }

    // ---- header ----

    private fun header(): HTMLElement {
        val header = el("div", "header")
        val titles = el("div", "titles")
        titles.appendChild(el("h1").withText("JsonLogic Playground"))
        titles.appendChild(el("div", "subtitle").withText(Subtitle))
        header.appendChild(titles)

        shareButton = button("chip selected", "Share") {
            val url = shareUrl(SharedState(ruleText, dataText))
            // Not pushState: sharing is not navigation, and would litter the back button.
            window.history.replaceState(null, "", url)
            copyShareLink(url)
        }
        header.appendChild(shareButton)

        // Empty until applyTheme, which is what decides which of the two icons it holds.
        themeButton = button("icon-button", "") {
            darkOverride = !dark
            applyTheme()
        }
        themeButton.setAttribute("aria-label", "Toggle theme")
        header.appendChild(themeButton)

        val github = document.createElement("a") as HTMLAnchorElement
        github.className = "text-button"
        github.href = RepositoryUrl
        github.target = "_blank"
        github.textContent = "GitHub"
        header.appendChild(github)

        return header
    }

    /**
     * The clipboard is a courtesy here and the address bar is the guarantee, so the button says
     * which of the two the reader got. `navigator.clipboard` is absent outside a secure context,
     * where reaching for it throws; where it is present the write can still be refused, and a
     * refusal arrives only as a rejected promise.
     */
    private fun copyShareLink(url: String) {
        val write = runCatching { window.navigator.clipboard.writeText(url) }.getOrNull()
        if (write == null) {
            reportShare("In the URL")
        } else {
            write.then({ reportShare("Copied") }, { reportShare("In the URL") })
        }
    }

    private fun reportShare(outcome: String) {
        shareButton.textContent = outcome
        window.clearTimeout(shareReset)
        shareReset = window.setTimeout({ shareButton.textContent = "Share" }, 1600)
    }

    private val dark: Boolean get() = darkOverride ?: systemDark.matches

    private fun applyTheme() {
        document.documentElement?.setAttribute("data-theme", if (dark) "dark" else "light")
        themeButton.clear()
        themeButton.appendChild(icon(if (dark) SunIcon else MoonIcon))
    }

    private fun shareUrl(state: SharedState): String =
        with(window.location) { "$origin$pathname$search#${ShareLink.encode(state)}" }

    // ---- examples ----

    private fun examplesRow(): HTMLElement {
        val row = el("div", "examples")
        row.appendChild(el("span", "label").withText("Examples"))

        exampleChips = Examples.map { example ->
            val chip = button("chip", example.label) {
                ruleEditor.setText(example.rule)
                dataEditor.setText(example.data)
            }
            row.appendChild(chip)
            example to chip
        }

        return row
    }

    private fun syncExampleChips() {
        exampleChips.forEach { (example, chip) ->
            val active = example.rule == ruleText && example.data == dataText
            chip.className = if (active) "chip selected" else "chip"
        }
    }

    // ---- editors and result ----

    private fun editors(): HTMLElement {
        val editors = el("div", "editors")
        val left = el("div", "left")

        ruleEditor = Editor("Rule") { text ->
            ruleText = text
            onTextChanged()
        }
        dataEditor = Editor("Data") { text ->
            dataText = text
            onTextChanged()
        }
        ruleStatus = el("div", "status")
        dataStatus = el("div", "status")
        resultType = el("span", "type-name")
        resultBody = el("div", "result")

        left.appendChild(panel("Rule", ruleStatus, ruleEditor.root))
        left.appendChild(panel("Data", dataStatus, dataEditor.root))
        editors.appendChild(left)
        editors.appendChild(panel("Result", resultType, resultBody))

        return editors
    }

    /** The label row sits outside the border, so an editor can fill the content area edge to edge. */
    private fun panel(label: String, trailing: HTMLElement, body: HTMLElement): HTMLElement {
        val panel = el("div", "panel")
        val head = el("div", "panel-head")
        head.appendChild(el("span", "label").withText(label))
        head.appendChild(trailing)
        panel.appendChild(head)

        val bodyBox = el("div", "panel-body")
        bodyBox.appendChild(body)
        panel.appendChild(bodyBox)

        return panel
    }

    private fun onTextChanged() {
        syncExampleChips()
        window.clearTimeout(debounce)
        debounce = window.setTimeout({ evaluateNow() }, 120)
    }

    private fun evaluateNow() {
        val evaluation = evaluate(jsonLogic, ruleText, dataText)
        renderStatus(ruleStatus, evaluation.ruleValid)
        renderStatus(dataStatus, evaluation.dataValid)
        renderResult(evaluation.outcome)
    }

    private fun renderStatus(host: HTMLElement, valid: Boolean) {
        host.clear()
        val dot = el("div", "dot")
        dot.style.background = if (valid) "var(--ok)" else "var(--error)"
        host.appendChild(dot)
        host.appendChild(el("span", "text").withText(if (valid) "ok" else "syntax error"))
    }

    private fun renderResult(outcome: EvalOutcome) {
        resultBody.clear()
        resultType.textContent = ""

        when (outcome) {
            EvalOutcome.Empty ->
                resultBody.appendChild(
                    el("div", "placeholder").withText("Enter a rule to see its result."),
                )

            is EvalOutcome.Success -> {
                resultType.textContent = outcome.value.typeName()
                val pre = el("pre", "mono")
                highlightInto(pre, prettyPrint(outcome.value))
                resultBody.appendChild(pre)
            }

            is EvalOutcome.Failure -> {
                val failure = el("div", "failure")
                failure.appendChild(el("div", "kind").withText(outcome.kind.label))
                failure.appendChild(el("div", "detail mono").withText(outcome.detail))
                outcome.jsonPath?.let { path ->
                    failure.appendChild(el("div", "path mono").withText("at $path"))
                }
                resultBody.appendChild(failure)
            }
        }
    }

    // ---- operations reference ----

    /** The full operator list, collapsed by default so it never competes with the editors for space. */
    private fun operationsReference(): HTMLElement {
        val host = el("div")
        opsBody = el("div", "ops-body")

        opsToggle = el("button", "ops-toggle")
        opsToggle.appendChild(icon(ChevronIcon))
        opsToggle.appendChild(el("span", "label").withText("Operations"))
        opsToggle.addEventListener("click", {
            referenceExpanded = !referenceExpanded
            syncReference()
        })
        syncReference()

        OperationGroups.forEach { group ->
            val groupBox = el("div", "ops-group")
            groupBox.appendChild(el("span", "name").withText(group.name))
            val rows = el("div", "ops-rows")
            group.operations.forEach { operation ->
                val row = el("button", "op-row")
                val cell = el("span", "symbol-cell")
                cell.appendChild(el("span", "symbol mono").withText(operation.symbol))
                row.appendChild(cell)
                row.appendChild(el("span", "summary").withText(operation.summary))
                row.addEventListener("click", { selectOperation(operation.snippet) })
                rows.appendChild(row)
            }
            groupBox.appendChild(rows)
            opsBody.appendChild(groupBox)
        }

        host.appendChild(opsToggle)
        host.appendChild(opsBody)

        return host
    }

    /** The three faces of one piece of state: the chevron's angle, the panel, and what is announced. */
    private fun syncReference() {
        opsToggle.className = if (referenceExpanded) "ops-toggle expanded" else "ops-toggle"
        opsToggle.setAttribute("aria-expanded", referenceExpanded.toString())
        opsBody.style.display = if (referenceExpanded) "block" else "none"
    }

    /**
     * Every snippet is written to evaluate against no data, so the data editor is emptied rather
     * than left holding values that belong to the rule being replaced: at best they are ignored, at
     * worst a `var` inside the snippet reads them and the result looks like the snippet's own.
     */
    private fun selectOperation(snippet: String) {
        ruleEditor.setText(snippet)
        dataEditor.setText("")
    }
}
