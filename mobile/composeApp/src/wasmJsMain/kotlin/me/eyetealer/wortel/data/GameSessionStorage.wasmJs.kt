package me.eyetealer.wortel.data

import kotlinx.browser.window

private const val GAME_ID_KEY = "wortel.gameId"

actual class GameSessionStorage actual constructor() {
    actual fun saveGameId(id: String?) {
        val storage = window.localStorage
        if (id == null) storage.removeItem(GAME_ID_KEY) else storage.setItem(GAME_ID_KEY, id)
    }

    actual fun loadGameId(): String? = window.localStorage.getItem(GAME_ID_KEY)
}
