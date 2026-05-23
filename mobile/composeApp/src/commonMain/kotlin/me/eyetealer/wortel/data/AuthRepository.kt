package me.eyetealer.wortel.data

import io.github.jan.supabase.auth.auth

/**
 * Auth wrapper. MVP is anonymous-only: app calls `ensureSignedIn()` at startup
 * to obtain a JWT for binding games to a stable (per-install) user.
 */
class AuthRepository(private val client: Supabase = Supabase) {

    suspend fun ensureSignedIn() {
        val auth = client.client.auth
        if (auth.currentUserOrNull() == null) {
            auth.signInAnonymously()
        }
    }

    fun currentUserId(): String? = client.client.auth.currentUserOrNull()?.id

    suspend fun signOut() {
        client.client.auth.signOut()
    }
}
