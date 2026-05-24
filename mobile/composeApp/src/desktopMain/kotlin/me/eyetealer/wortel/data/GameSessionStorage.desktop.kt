package me.eyetealer.wortel.data

/**
 * In-memory implementation for the desktop test target. Tests don't
 * exercise the storage round-trip and this keeps the JVM build green
 * without dragging in a file-backed Preferences API.
 */
actual class GameSessionStorage actual constructor() {
    private var id: String? = null
    actual fun saveGameId(id: String?) {
        this.id = id
    }
    actual fun loadGameId(): String? = id
}
