package me.eyetealer.wortel.data

/**
 * Tiny key-value store for the single piece of game state we want to
 * survive an app reload: the active game's UUID. Per-platform actuals
 * back this with a native storage primitive:
 *   - wasmJs / web → window.localStorage
 *   - android      → SharedPreferences (TODO: needs Context wiring)
 *   - iOS          → NSUserDefaults    (TODO)
 *
 * The Android / iOS actuals currently fall back to in-memory storage
 * because we have no DI wiring yet for Context / Application — they
 * still satisfy the contract and let the same code run on every target.
 * Web is the only target that survives a full reload right now, which
 * is the one the user notices.
 */
expect class GameSessionStorage() {
    fun saveGameId(id: String?)
    fun loadGameId(): String?
}
