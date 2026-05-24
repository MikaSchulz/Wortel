package me.eyetealer.wortel.data

actual class SettingsStorage actual constructor() {
    private var colorblind: Boolean = false
    actual fun setColorblind(enabled: Boolean) {
        colorblind = enabled
    }
    actual fun isColorblind(): Boolean = colorblind
}
