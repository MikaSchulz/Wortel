package me.eyetealer.wortel.data

/**
 * In-memory stand-in for Android. Survives recompositions but not process
 * death. Swap for a Context-backed SharedPreferences once a DI surface is
 * available — the API contract stays the same.
 */
actual class GameSessionStorage actual constructor() {
    private var id: String? = null
    actual fun saveGameId(id: String?) {
        this.id = id
    }
    actual fun loadGameId(): String? = id
}
