package me.eyetealer.wortel.data

import com.russhwolf.settings.Settings

/** The one piece of game state that survives an app restart: the active game's id. */
interface GameSessionStorage {
    fun saveGameId(id: String?)
    fun loadGameId(): String?
}

/** Small UI preferences. */
interface SettingsStorage {
    fun setColorblind(enabled: Boolean)
    fun isColorblind(): Boolean
}

/**
 * Persistent key-value storage on every target, backed by the platform's
 * native store via multiplatform-settings:
 *   - Android → SharedPreferences (Context wired up by androidx-startup)
 *   - iOS     → NSUserDefaults
 *   - web     → window.localStorage
 *   - desktop → java.util.prefs
 *
 * Key names and value encodings match the earlier hand-written storage, so
 * data saved by older app versions keeps working.
 */
class LocalStorage(
    private val settings: Settings = Settings(),
) : GameSessionStorage, SettingsStorage {

    override fun saveGameId(id: String?) {
        if (id == null) settings.remove(GAME_ID_KEY) else settings.putString(GAME_ID_KEY, id)
    }

    override fun loadGameId(): String? = settings.getStringOrNull(GAME_ID_KEY)

    override fun setColorblind(enabled: Boolean) {
        settings.putBoolean(COLORBLIND_KEY, enabled)
    }

    override fun isColorblind(): Boolean = settings.getBoolean(COLORBLIND_KEY, false)

    private companion object {
        const val GAME_ID_KEY = "wortel.gameId"
        const val COLORBLIND_KEY = "wortel.colorblind"
    }
}
