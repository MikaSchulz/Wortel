package me.eyetealer.wortel.data

import platform.Foundation.NSUserDefaults

private const val GAME_ID_KEY = "wortel.gameId"

actual class GameSessionStorage actual constructor() {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun saveGameId(id: String?) {
        if (id == null) {
            defaults.removeObjectForKey(GAME_ID_KEY)
        } else {
            defaults.setObject(id, GAME_ID_KEY)
        }
    }

    actual fun loadGameId(): String? = defaults.stringForKey(GAME_ID_KEY)
}
