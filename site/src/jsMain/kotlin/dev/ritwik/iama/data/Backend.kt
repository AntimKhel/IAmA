package dev.ritwik.iama.data

import kotlinx.browser.window

/**
 * Where the visitor counter + guestbook API lives (Cloudflare Worker, see /backend).
 * Locally it's `wrangler dev`; in production it's the deployed workers.dev URL.
 * While [PROD_API] is empty the features stay hidden on the live site instead of erroring.
 */
object Backend {
    // Filled in after `npx wrangler deploy` (backend/setup.sh prints the URL).
    private const val PROD_API = "https://iama-api.raj-ritwik96.workers.dev"

    // Cloudflare Turnstile site key (public). Locally: Cloudflare's always-pass test key.
    private const val PROD_TURNSTILE_SITE_KEY = "0x4AAAAAAFkUMaaZ-i9ppFkb"
    private const val TEST_TURNSTILE_SITE_KEY = "1x00000000000000000000AA"

    private val isLocal: Boolean
        get() = window.location.hostname.let { it == "localhost" || it == "127.0.0.1" }

    val apiBase: String?
        get() = if (isLocal) "http://127.0.0.1:8787" else PROD_API.ifEmpty { null }

    val turnstileSiteKey: String
        get() = if (isLocal) TEST_TURNSTILE_SITE_KEY else PROD_TURNSTILE_SITE_KEY

    val enabled: Boolean get() = apiBase != null && turnstileSiteKey.isNotEmpty()

    /** First day the counter ran, shown as "since …". */
    const val COUNTING_SINCE = "Oct 2026"
}
