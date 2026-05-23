package me.eyetealer.wortel.data

/**
 * Static config for the live Supabase project. The publishable key is safe to
 * embed — RLS policies on `public.games` enforce ownership.
 *
 * If you need staging/preview environments later, switch this to read from
 * a BuildConfig or platform-specific resource.
 */
object SupabaseConfig {
    const val URL: String = "https://lmmjbpgfnuxbcdvjtdkm.supabase.co"
    const val PUBLISHABLE_KEY: String =
        "sb_publishable_NwY2X2Dj3T7O1d1-hWgOVQ_BFx1CYys"
    const val FUNCTIONS_BASE: String = "$URL/functions/v1"
}
