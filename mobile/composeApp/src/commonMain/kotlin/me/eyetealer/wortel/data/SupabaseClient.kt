package me.eyetealer.wortel.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions

/**
 * Singleton Supabase client. Holds Auth (anonymous sign-in) + Functions
 * (Edge Function invocation). RLS-bound Postgrest is also wired up for
 * future direct table queries (e.g. user stats).
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
            install(Functions)
        }
    }
}
