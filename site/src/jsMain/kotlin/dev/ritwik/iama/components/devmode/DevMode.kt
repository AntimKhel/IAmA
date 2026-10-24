package dev.ritwik.iama.components.devmode

import androidx.compose.runtime.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent

/** The two looks each home-page section can take: the default, and the "dev mode" easter egg. */
enum class Variant(val code: String) {
    MATERIAL("a"),
    TERMINAL("b"),
}

/**
 * Dev mode re-skins every home-page section as terminal windows (adb, git log, htop, neofetch…).
 * Toggled by the small button at the bottom of the page, or the backtick key. Remembered locally.
 */
object DevMode {
    private const val KEY = "rr-dev-mode"

    var enabled by mutableStateOf(runCatching { window.localStorage.getItem(KEY) == "1" }.getOrDefault(false))
        private set

    /** Shown while the "sudo" transition plays. */
    var transitioning by mutableStateOf(false)
        internal set

    val variant: Variant get() = if (enabled) Variant.TERMINAL else Variant.MATERIAL

    internal fun apply(on: Boolean) {
        enabled = on
        document.documentElement?.classList?.toggle("dev-mode", on)
        runCatching { if (on) window.localStorage.setItem(KEY, "1") else window.localStorage.removeItem(KEY) }
    }

    fun toggle() {
        if (!transitioning) transitioning = true
    }
}

private val enterLines = listOf(
    "$ sudo portfolio --mode=dev",
    "[sudo] password for recruiter: ********",
    "✓ access granted",
    "→ swapping UI for terminals…",
)

private val exitLines = listOf(
    "$ exit",
    "→ restoring Material You…",
    "✓ welcome back",
)

/** Small quirky switch at the very end of the home page + the transition overlay. */
@Composable
fun DevModeSwitch() {
    LaunchedEffect(Unit) { document.documentElement?.classList?.toggle("dev-mode", DevMode.enabled) }

    DisposableEffect(Unit) {
        val onKey: (Event) -> Unit = { e ->
            val k = e as KeyboardEvent
            val tag = (k.target as? org.w3c.dom.Element)?.tagName?.lowercase()
            if (k.key == "`" && tag != "input" && tag != "textarea" && !k.metaKey && !k.ctrlKey) DevMode.toggle()
        }
        window.addEventListener("keydown", onKey)
        onDispose { window.removeEventListener("keydown", onKey) }
    }

    Div({ classes("dev-switch-wrap") }) {
        Button({
            classes("dev-switch")
            if (DevMode.enabled) classes("on")
            attr("title", "Shortcut: ` (backtick)")
            onClick { DevMode.toggle() }
        }) {
            Span({ classes("dev-led") })
            Span({ classes("dev-label") }) { Text(if (DevMode.enabled) "exit dev mode" else "enter dev mode") }
            Span({ classes("dev-key") }) { Text("`") }
        }
        Div({ classes("dev-caption") }) {
            Text(if (DevMode.enabled) "You're seeing the source of truth. Press ` to go back." else "For engineers who'd rather read a terminal.")
        }
    }

    if (DevMode.transitioning) DevModeTransition()
}

@Composable
private fun DevModeTransition() {
    val entering = !DevMode.enabled
    val lines = if (entering) enterLines else exitLines
    var shown by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        for (i in lines.indices) {
            delay(if (i == 0) 120 else 230)
            shown = i + 1
        }
        delay(260)
        DevMode.apply(entering)
        delay(420)
        DevMode.transitioning = false
    }
    Div({ classes("dev-overlay"); if (!entering) classes("exit"); attr("aria-hidden", "true") }) {
        Div({ classes("dev-overlay-glitch") })
        Div({ classes("dev-overlay-term") }) {
            lines.take(shown).forEach { Div({ classes("l") }) { Text(it) } }
            Span({ classes("cursor") })
        }
    }
}
