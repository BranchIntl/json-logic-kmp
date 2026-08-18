package co.branch.jsonlogic.playground.ui

import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement

internal fun el(tag: String, className: String = ""): HTMLElement {
    val element = document.createElement(tag) as HTMLElement
    if (className.isNotEmpty()) element.className = className

    return element
}

internal fun HTMLElement.withText(text: String): HTMLElement {
    textContent = text

    return this
}

internal fun button(className: String, label: String, onClick: () -> Unit): HTMLElement {
    val element = document.createElement("button") as HTMLButtonElement
    element.className = className
    element.textContent = label
    element.addEventListener("click", { onClick() })

    return element
}

internal fun Element.clear() {
    while (firstChild != null) removeChild(firstChild!!)
}
