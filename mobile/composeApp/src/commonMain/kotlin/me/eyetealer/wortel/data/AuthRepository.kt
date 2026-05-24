package me.eyetealer.wortel.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.first

/**
 * Auth wrapper. MVP is anonymous-only: app calls `ensureSignedIn()` at startup
 * to obtain a JWT for binding games to a stable (per-install) user.
 */
class AuthRepository(private val client: Supabase = Supabase) {

    /**
     * Ensures the Supabase client has finished restoring any persisted
     * session from storage before we read currentUserOrNull / accessToken.
     *
     * Without this wait, code right after `Supabase.client` initialisation
     * sees SessionStatus.Initializing — currentSessionOrNull returns null
     * even though a valid session is sitting in localStorage and about to
     * be loaded. We then build HTTP requests with just the publishable key
     * as Bearer, the server can't resolve a user, and the Edge Function
     * returns 403 for any user-bound game.
     */
    suspend fun ensureSignedIn() {
        val auth = client.client.auth
        auth.sessionStatus.first { it !is SessionStatus.Initializing }
        if (auth.currentUserOrNull() == null) {
            auth.signInAnonymously()
        }
    }

    fun currentUserId(): String? = client.client.auth.currentUserOrNull()?.id

    suspend fun signOut() {
        client.client.auth.signOut()
    }
}
