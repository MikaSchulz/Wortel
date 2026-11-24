package me.eyetealer.wortel.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient

/**
 * Singleton Supabase client. Only Auth is installed: game traffic goes to
 * the `games` Edge Function through [GameRepository]'s own Ktor client, and
 * tables are never queried directly (RLS keeps them closed anyway).
 */
object Supabase {
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SupabaseConfig.URL,
            supabaseKey = SupabaseConfig.PUBLISHABLE_KEY,
        ) {
            install(Auth) {
                alwaysAutoRefresh = true
                autoLoadFromStorage = true
            }
        }
    }
}
