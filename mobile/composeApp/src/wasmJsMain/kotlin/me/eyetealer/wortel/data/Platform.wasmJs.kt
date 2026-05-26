package me.eyetealer.wortel.data

import kotlinx.browser.window

/**
 * origin + pathname only — we deliberately strip any existing query/hash
 * because the OAuth callback will append its own #access_token=... that
 * we do not want concatenated onto a stale fragment.
 */
actual fun currentAppUrl(): String? {
    val loc = window.location
    return loc.origin + loc.pathname
}
