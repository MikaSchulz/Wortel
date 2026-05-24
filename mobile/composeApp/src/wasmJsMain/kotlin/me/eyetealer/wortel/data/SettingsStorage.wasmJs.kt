package me.eyetealer.wortel.data

import kotlinx.browser.window

private const val COLORBLIND_KEY = "wortel.colorblind"

actual class SettingsStorage actual constructor() {
    actual fun setColorblind(enabled: Boolean) {
        window.localStorage.setItem(COLORBLIND_KEY, enabled.toString())
    }

    actual fun isColorblind(): Boolean =
        window.localStorage.getItem(COLORBLIND_KEY) == "true"
}
