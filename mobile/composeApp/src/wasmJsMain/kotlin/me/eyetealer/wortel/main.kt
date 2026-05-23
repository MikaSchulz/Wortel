package me.eyetealer.wortel

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

/**
 * Web entry point — mounts the shared Compose UI onto a full-page canvas.
 * The same `App()` composable is rendered for Android, iOS, and now web.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        App()
    }
}
