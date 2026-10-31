package dev.ritwik.iama.components.widgets

import dev.ritwik.iama.components.phone.PhoneState
import dev.ritwik.iama.components.phone.AppHost
import dev.ritwik.iama.components.phone.Apps
import androidx.compose.runtime.*
import dev.ritwik.iama.data.Backend
import dev.ritwik.iama.util.Api
import dev.ritwik.iama.util.prefersReducedMotion
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.*
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Unique-visitor count from the Worker. The first page view of a browser session registers the
 * visit (`POST /visit`, counted at most once a day server-side); later views just read `/stats`.
 */
object VisitorStats {
    private const val SESSION_KEY = "rr-visit-counted"

    var visitors by mutableStateOf<Int?>(null)
        private set
    private var started = false

    suspend fun load() {
        if (started || !Backend.enabled) return
        started = true
        val alreadyCounted = runCatching { window.sessionStorage.getItem(SESSION_KEY) != null }.getOrDefault(false)
        val res = if (alreadyCounted) Api.get("/stats") else Api.post("/visit")
        if (res?.ok == true) {
            visitors = (res.body.visitors as Number).toInt()
            runCatching { window.sessionStorage.setItem(SESSION_KEY, "1") }
        } else {
            started = false // allow a retry on the next page
        }
    }
}

/** Counts up to [target] once, then stays; formatted with the visitor's locale separators. */
@Composable
fun CountUp(target: Int, durationMs: Int = 1400) {
    var shown by remember { mutableStateOf(if (prefersReducedMotion) target else 0) }
    LaunchedEffect(target) {
        if (prefersReducedMotion) {
            shown = target
            return@LaunchedEffect
        }
        val from = shown
        val start = window.performance.now()
        while (true) {
            val t = ((window.performance.now() - start) / durationMs).coerceIn(0.0, 1.0)
            val eased = 1 - (1 - t).pow(3)
            shown = (from + (target - from) * eased).roundToInt()
            if (t >= 1.0) break
            kotlinx.coroutines.delay(16)
        }
    }
    Text(shown.asDynamic().toLocaleString() as String)
}

/** Footer badge: "● 12,431 visitors since Oct 2026". Renders nothing until the count is known. */
@Composable
fun VisitorBadge() {
    val count = VisitorStats.visitors ?: return
    // Opens the visitor map (the Stats app).
    Button({
        classes("visitor-badge")
        attr("title", "Unique visitors, counted once a day. No cookies. Tap for the map.")
        attr("data-origin", "")
        onClick { e -> PhoneState.open(Apps.stats, AppHost.FULL, e.target as? org.w3c.dom.Element) }
    }) {
        Span({ classes("live") })
        B { CountUp(count) }
        Text(" ${if (count == 1) "visitor" else "visitors"} since ${Backend.COUNTING_SINCE}")
        I({ classes("fa-solid", "fa-earth-asia") })
    }
}
