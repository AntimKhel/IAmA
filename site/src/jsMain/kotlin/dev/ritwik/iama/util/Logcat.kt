package dev.ritwik.iama.util

import androidx.compose.runtime.mutableStateListOf
import kotlinx.browser.window
import org.w3c.dom.events.Event
import kotlin.js.Date

/**
 * The site's own logcat: real events (pages displayed, apps started, network calls, slow frames, errors),
 * written as Android log lines and shown by the Logcat app. Nothing here is invented: every line is
 * something that just happened in this tab.
 */
object Logcat {
    enum class Level { V, D, I, W, E }

    class Line(val id: Int, val time: Double, val level: Level, val tag: String, val message: String)

    const val PACKAGE = "dev.ritwik.iama"

    /** A stand-in process id for this tab, so lines look like `4321  4321 I Tag: …`. */
    val pid: Int = (2000..9999).random()

    private const val MAX_LINES = 800
    val lines = mutableStateListOf<Line>()
    private var seq = 0

    fun log(level: Level, tag: String, message: String) {
        lines += Line(++seq, Date.now(), level, tag, message)
        if (lines.size > MAX_LINES) lines.removeRange(0, lines.size - MAX_LINES)
    }

    fun v(tag: String, message: String) = log(Level.V, tag, message)
    fun d(tag: String, message: String) = log(Level.D, tag, message)
    fun i(tag: String, message: String) = log(Level.I, tag, message)
    fun w(tag: String, message: String) = log(Level.W, tag, message)
    fun e(tag: String, message: String) = log(Level.E, tag, message)

    private var installed = false

    /** Hooks the browser events worth logging. Called once, from the page layout. */
    fun install() {
        if (installed) return
        installed = true

        // Like ActivityTaskManager's "Displayed …: +412ms", from the browser's real first-paint timing.
        val paint = runCatching {
            (window.performance.asDynamic().getEntriesByType("paint") as Array<dynamic>)
                .firstOrNull { it.name == "first-contentful-paint" }?.startTime as? Double
        }.getOrNull()
        i("ActivityTaskManager", "Displayed $PACKAGE/.MainActivity: +${(paint ?: window.performance.now()).toInt()}ms")

        window.addEventListener("error", { event: Event ->
            val ev = event.asDynamic()
            val msg = ev.message as? String ?: return@addEventListener
            e("chromium", "Uncaught ${msg.removePrefix("Uncaught ")} (${(ev.filename as? String)?.substringAfterLast('/') ?: "?"}:${ev.lineno})")
        })
        window.addEventListener("unhandledrejection", { event: Event ->
            e("chromium", "Unhandled promise rejection: ${event.asDynamic().reason}")
        })

        // Slow frames, reported by the browser itself (no polling loop): the Choreographer's famous warning.
        observe("long-animation-frame") { entry ->
            val duration = entry.duration as Double
            val skipped = (duration / 16.67).toInt() - 1
            if (skipped >= 3) {
                w("Choreographer", "Skipped $skipped frames!  The application may be doing too much work on its main thread.")
            }
        }
    }

    /** Subscribes to a PerformanceObserver entry type; returns the observer (to disconnect), or null if unsupported. */
    fun observe(type: String, onEntry: (dynamic) -> Unit): dynamic {
        val ctor = window.asDynamic().PerformanceObserver ?: return null
        val supported = ctor.supportedEntryTypes as? Array<String>
        if (supported == null || type !in supported) return null
        val callback: (dynamic) -> Unit = { list -> (list.getEntries() as Array<dynamic>).forEach(onEntry) }
        return runCatching {
            val observer = js("Reflect").construct(ctor, arrayOf(callback))
            observer.observe(jsObject("type" to type, "buffered" to false))
            observer
        }.getOrNull()
    }
}

/** "1.2 kB" / "312 B" for network log lines. */
fun formatBytes(bytes: Int): String = when {
    bytes < 1024 -> "$bytes B"
    else -> "${((bytes / 102.4).toInt() / 10.0)} kB"
}
