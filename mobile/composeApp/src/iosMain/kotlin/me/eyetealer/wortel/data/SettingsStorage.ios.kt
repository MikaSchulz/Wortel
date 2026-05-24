package me.eyetealer.wortel.data

import platform.Foundation.NSUserDefaults

private const val COLORBLIND_KEY = "wortel.colorblind"

actual class SettingsStorage actual constructor() {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun setColorblind(enabled: Boolean) {
        defaults.setBool(enabled, COLORBLIND_KEY)
    }

    actual fun isColorblind(): Boolean = defaults.boolForKey(COLORBLIND_KEY)
}
