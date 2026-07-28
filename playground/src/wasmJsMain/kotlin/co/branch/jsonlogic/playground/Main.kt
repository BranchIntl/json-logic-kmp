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
                // Not pushState: sharing is not navigation, and would litter the back button.
                window.history.replaceState(null, "", url)
                url
            },
        )
    }
}

private fun shareUrl(state: SharedState): String =
    with(window.location) { "$origin$pathname$search#${ShareLink.encode(state)}" }
