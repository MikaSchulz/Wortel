package me.eyetealer.wortel.data

/**
 * Returns the URL the OAuth provider should redirect back to after the
 * user finishes signing in. On web this is the current page's origin
 * plus path (e.g. https://mikaschulz.github.io/Wortel/) so the callback
 * lands the user back on the actual app, not the GitHub Pages root.
 * Non-web targets return null and rely on platform-specific deep-link
 * handling configured elsewhere.
 */
expect fun currentAppUrl(): String?
