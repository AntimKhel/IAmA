package dev.ritwik.iama.components.blog

import androidx.compose.runtime.*
import dev.ritwik.iama.data.Backend
import dev.ritwik.iama.util.Api
import dev.ritwik.iama.util.Logcat
import dev.ritwik.iama.util.haptic
import dev.ritwik.iama.util.jsObject
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import kotlin.js.Date

/**
 * Blog reactions: reads and claps per post, from the Worker (backend/src/posts.ts). A read counts once the
 * reader is halfway through (or has spent a while on the post); claps are capped at ten per visitor per day.
 */
object PostStats {
    class Totals(val reads: Int, val claps: Int)

    var totals by mutableStateOf<Map<String, Totals>>(emptyMap()); private set
    var mine by mutableStateOf<Map<String, Int>>(emptyMap()); private set
    var maxClaps by mutableStateOf(10); private set
    private var loadedAt = 0.0
    // Sends outlive the composable that triggered them (the clap button restarts its effect on every tap).
    private val scope = kotlinx.coroutines.MainScope()

    fun send(slug: String, count: Int) {
        scope.launch { clap(slug, count) }
    }

    suspend fun loadAll() {
        if (!Backend.enabled || Date.now() - loadedAt < 60_000) return
        loadedAt = Date.now()
        val res = Api.get("/posts") ?: return
        if (!res.ok) return
        val posts = res.body.posts
        val keys = js("Object").keys(posts) as Array<String>
        totals = totals + keys.associateWith { Totals((posts[it].reads as Number).toInt(), (posts[it].claps as Number).toInt()) }
    }

    /** Counts this visitor's read of [slug] (once per session; the Worker also dedupes per day). */
    suspend fun read(slug: String) {
        if (!Backend.enabled) return
        val key = "rr-read-$slug"
        if (runCatching { window.sessionStorage.getItem(key) }.getOrNull() != null) return
        runCatching { window.sessionStorage.setItem(key, "1") }
        apply(slug, Api.post("/posts/$slug/read"))
        Logcat.d("BlogReactions", "read counted: $slug")
    }

    suspend fun clap(slug: String, count: Int) {
        if (!Backend.enabled || count <= 0) return
        apply(slug, Api.post("/posts/$slug/clap", jsObject("count" to count)))
        Logcat.d("BlogReactions", "+$count claps: $slug (today ${mine[slug] ?: 0}/$maxClaps)")
    }

    private fun apply(slug: String, res: dev.ritwik.iama.util.ApiResult?) {
        if (res?.ok != true) return
        val b = res.body
        totals = totals + (slug to Totals((b.reads as Number).toInt(), (b.claps as Number).toInt()))
        mine = mine + (slug to (b.myClaps as Number).toInt())
        maxClaps = (b.maxClaps as? Number)?.toInt() ?: maxClaps
    }
}

/** "/blog/r8-story" → "r8-story" (the post's id on the backend). */
fun slugOf(route: String): String = route.trimEnd('/').substringAfterLast('/')

fun compact(n: Int): String = when {
    n >= 1_000_000 -> "${(n / 100_000) / 10.0}M"
    n >= 10_000 -> "${n / 1000}k"
    n >= 1000 -> "${(n / 100) / 10.0}k"
    else -> n.toString()
}

/** "1.2k reads · 34 claps", once known. */
@Composable
fun PostCounts(slug: String, prefix: String = " · ") {
    val t = PostStats.totals[slug] ?: return
    Text("$prefix${compact(t.reads)} read${if (t.reads == 1) "" else "s"}")
    if (t.claps > 0) Text(" · ${compact(t.claps)} 👏")
}

/**
 * Counts a read once the reader reaches the middle of the post or spends [dwellMs] on it. [scroller] finds
 * the element that scrolls (the window when null); [content] the article whose middle counts.
 */
@Composable
fun ReadTracker(slug: String, dwellMs: Long = 25_000, scroller: () -> HTMLElement? = { null }, content: () -> HTMLElement?) {
    var counted by remember(slug) { mutableStateOf(false) }
    LaunchedEffect(slug) { PostStats.loadAll() }
    LaunchedEffect(slug, counted) {
        if (counted) return@LaunchedEffect
        delay(dwellMs)
        if (document.asDynamic().visibilityState == "visible") counted = true
    }
    DisposableEffect(slug) {
        val target = scroller()
        val check: (Event) -> Unit = {
            val article = content()
            if (article != null && !counted) {
                val r = article.getBoundingClientRect()
                if (r.top + r.height * 0.5 < window.innerHeight) counted = true
            }
        }
        val passive: dynamic = js("({ passive: true })")
        (target ?: window).addEventListener("scroll", check, passive)
        onDispose { (target ?: window).removeEventListener("scroll", check, passive) }
    }
    LaunchedEffect(counted) { if (counted) PostStats.read(slug) }
}

/** The clap button: tap (or keep tapping) to clap, up to ten times a day; sent in one batch. */
@Composable
fun ClapBar(slug: String) {
    if (!Backend.enabled) return
    LaunchedEffect(slug) { PostStats.loadAll() }
    var pending by remember(slug) { mutableStateOf(0) }
    var burst by remember(slug) { mutableStateOf(0) }
    val given = (PostStats.mine[slug] ?: 0) + pending
    val left = PostStats.maxClaps - given
    val total = (PostStats.totals[slug]?.claps ?: 0) + pending

    LaunchedEffect(pending) {
        if (pending == 0) return@LaunchedEffect
        delay(700)
        val n = pending
        pending = 0 // restarts this effect, so the send must not run inside it
        PostStats.send(slug, n)
    }

    Div({ classes("clap-bar") }) {
        Button({
            classes("clap-btn")
            if (left <= 0) classes("done")
            attr("aria-label", if (left <= 0) "You've clapped ${PostStats.maxClaps} times today" else "Clap for this post")
            onClick {
                if (left > 0) {
                    haptic(10)
                    pending++
                    burst++
                }
            }
        }) {
            I({ classes("fa-solid", "fa-hands-clapping") })
            if (burst > 0) key(burst) { Span({ classes("clap-plus") }) { Text("+${given.coerceAtMost(PostStats.maxClaps)}") } }
        }
        Div({ classes("clap-text") }) {
            Div({ classes("clap-total") }) { Text(if (total > 0) "${compact(total)} clap${if (total == 1) "" else "s"}" else "Be the first to clap") }
            Div({ classes("clap-hint") }) {
                Text(
                    when {
                        left <= 0 -> "That's ${PostStats.maxClaps}. Thank you!"
                        given > 0 -> "You've clapped $given. Up to ${PostStats.maxClaps} a day."
                        else -> "Enjoyed it? Clap up to ${PostStats.maxClaps} times."
                    }
                )
            }
        }
    }
}
