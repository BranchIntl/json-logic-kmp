package co.branch.jsonlogic.playground

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import co.branch.jsonlogic.playground.share.ShareLink
import co.branch.jsonlogic.playground.share.SharedState
import kotlin.js.ExperimentalWasmJsInterop
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class, ExperimentalWasmJsInterop::class)
fun main() {
    val shared = ShareLink.decode(window.location.hash)

    ComposeViewport(viewportContainerId = "playground") {
        App(
            initial = shared,
            onShare = { state ->
                val url = shareUrl(state)
                // replaceState rather than pushState: sharing is not navigation, and every share
                // would otherwise add an entry the back button has to walk through.
                window.history.replaceState(null, "", url)
                url
            },
        )
    }
}

/** The current address with a fresh fragment, leaving the path and query untouched. */
private fun shareUrl(state: SharedState): String =
    with(window.location) { "$origin$pathname$search#${ShareLink.encode(state)}" }
