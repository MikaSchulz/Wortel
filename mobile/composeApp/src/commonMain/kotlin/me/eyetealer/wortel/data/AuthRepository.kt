package me.eyetealer.wortel.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Snapshot of the current Supabase user — surfaced via [AuthRepository.observeUser]
 * so the UI can show "Eingeloggt als …" or fall back to the login screen.
 */
data class UserState(
    val userId: String?,
    val email: String?,
    val displayName: String?,
    val avatarUrl: String?,
    val isAnonymous: Boolean,
) {
    /** True if any session is active — anonymous still counts as authenticated. */
    val isAuthenticated: Boolean get() = userId != null

    companion object {
        val SignedOut = UserState(null, null, null, null, false)
    }
}

class AuthRepository(private val client: Supabase = Supabase) {

    /**
     * Wait until the SDK has finished loading any persisted session from
     * storage. Until this resolves, currentSessionOrNull lies.
     */
    suspend fun awaitInitialized() {
        client.client.auth.sessionStatus.first { it !is SessionStatus.Initializing }
    }

    /** Cold flow that emits a fresh [UserState] every time the session changes. */
    fun observeUser(): Flow<UserState> =
        client.client.auth.sessionStatus.map { status ->
            when (status) {
                is SessionStatus.Authenticated -> status.session.user?.toUserState() ?: UserState.SignedOut
                else -> UserState.SignedOut
            }
        }

    fun currentUserState(): UserState {
        val user = client.client.auth.currentUserOrNull() ?: return UserState.SignedOut
        return user.toUserState()
    }

    /**
     * Kick off a browser-side OAuth redirect to Google. The function returns
     * immediately; Supabase handles the callback on the way back and the
     * session shows up in [observeUser] after the redirect completes.
     *
     * On web we pass the current origin + path explicitly so the OAuth
     * callback lands the user back at e.g. /Wortel/ (and not the GitHub
     * Pages root /). The SDK's default behaviour can collapse to the
     * Site URL configured in Supabase, which strips the path on
     * sub-path-hosted apps. Native targets return null here and rely on
     * their own deep-link wiring.
     */
    suspend fun signInWithGoogle() {
        client.client.auth.signInWith(Google, redirectUrl = currentAppUrl())
    }

    /** Anonymous "guest" mode — gets a stable user_id without an email. */
    suspend fun signInAnonymously() {
        client.client.auth.signInAnonymously()
    }

    suspend fun signOut() {
        client.client.auth.signOut()
    }

    private fun UserInfo.toUserState(): UserState {
        val meta = userMetadata
        val name = meta?.get("full_name")?.asString()
            ?: meta?.get("name")?.asString()
            ?: email
        val provider = appMetadata?.get("provider")?.asString()
        // Supabase tags anonymous sessions with appMetadata.provider == "anonymous".
        val anon = provider == "anonymous"
        return UserState(
            userId = id,
            email = email,
            displayName = name,
            avatarUrl = meta?.get("avatar_url")?.asString(),
            isAnonymous = anon,
        )
    }

    private fun kotlinx.serialization.json.JsonElement.asString(): String? =
        (this as? JsonPrimitive)?.jsonPrimitive?.contentOrNull
}
