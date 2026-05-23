package me.eyetealer.wortel.data

/**
 * Maps backend domain error codes (see _shared/http/errors.ts on the server)
 * to user-facing German strings. Keep both sides in sync — when adding new
 * codes server-side, add a translation here, otherwise the fallback message
 * (often English from the server) leaks into the UI.
 */
object ErrorMessages {
    fun translate(code: String?, fallback: String): String = when (code) {
        "UNKNOWN_WORD" -> "Wort nicht im Wörterbuch."
        "WRONG_LENGTH" -> "Wort muss die richtige Länge haben."
        "GAME_NOT_RUNNING" -> "Spiel ist bereits beendet."
        "GAME_NOT_FOUND" -> "Spiel nicht gefunden."
        "FORBIDDEN" -> "Kein Zugriff auf dieses Spiel."
        "INVALID_WORD_LENGTH" -> "Diese Wortlänge ist nicht verfügbar."
        "INVALID_MAX_ATTEMPTS" -> "Anzahl Versuche ist ungültig."
        "SECRET_LENGTH_MISMATCH" -> "Interner Fehler — bitte später erneut versuchen."
        "INVALID_INPUT" -> "Ungültige Eingabe."
        "CONCURRENCY" -> "Konflikt — bitte erneut versuchen."
        "INTERNAL" -> "Serverfehler — bitte später erneut versuchen."
        else -> fallback
    }
}
