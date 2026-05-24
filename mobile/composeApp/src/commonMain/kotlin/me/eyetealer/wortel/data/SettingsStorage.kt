package me.eyetealer.wortel.data

/**
 * Persists small UI preferences (currently: colorblind palette toggle).
 * Same expect/actual pattern as [GameSessionStorage]:
 *   - wasmJs / web → window.localStorage
 *   - iOS          → NSUserDefaults
 *   - android      → in-memory stub (TODO Context-backed prefs)
 *   - desktop      → in-memory stub
 */
expect class SettingsStorage() {
    fun setColorblind(enabled: Boolean)
    fun isColorblind(): Boolean
}
