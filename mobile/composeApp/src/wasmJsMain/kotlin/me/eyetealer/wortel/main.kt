package me.eyetealer.wortel

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

/**
 * Web entry point — mounts the shared Compose UI onto a full-page canvas.
 * The same `App()` composable is rendered for Android, iOS, and now web.
 *
 * We remove the static loading element before mounting so it doesn't
 * overlay the Compose canvas (it uses position:fixed which would otherwise
 * block both visibility and pointer events).
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    document.getElementById("loading")?.remove()
    ComposeViewport(document.body!!) {
        App()
    }
}
