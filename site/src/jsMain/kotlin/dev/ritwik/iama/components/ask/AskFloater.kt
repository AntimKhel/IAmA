package dev.ritwik.iama.components.ask

import androidx.compose.runtime.*
import com.varabyte.kobweb.core.rememberPageContext
import dev.ritwik.iama.components.phone.AppHost
import dev.ritwik.iama.components.phone.Apps
import dev.ritwik.iama.components.phone.PhoneState
import dev.ritwik.iama.data.Backend
import dev.ritwik.iama.util.UiState
import dev.ritwik.iama.util.prefersReducedMotion
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.maxLength
import org.jetbrains.compose.web.attributes.onSubmit
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.Element
import org.w3c.dom.events.Event

/**
 * An always-there way into Ask Ritwik, for visitors who never tap the phone's Ask icon.
 * Three designs to compare live; pick one with `?fab=bar|orb|nav` (remembered in this browser).
 */
enum class AskFab { BAR, ORB, NAV }

object AskFloaterState {
    private const val KEY = "rr-askfab"

    val style: AskFab by lazy {
        val fromUrl = Regex("[?&]fab=(bar|orb|nav)").find(window.location.search)?.groupValues?.get(1)
        if (fromUrl != null) runCatching { window.localStorage.setItem(KEY, fromUrl) }
        val name = fromUrl ?: runCatching { window.localStorage.getItem(KEY) }.getOrNull() ?: "bar"
        AskFab.entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: AskFab.BAR
    }
}

/** Questions the floaters type out / tease with. Each one is answerable from the site. */
val askTeasers = listOf(
    "What did Ritwik build at Jio?",
    "How did he get R8 obfuscation to 82%?",
    "Has he led teams?",
    "What is JioAirSync?",
    "What's his experience with AI agents?",
    "Is he open to new roles?",
)

fun openAsk(from: Element?, question: String? = null) {
    PhoneState.open(Apps.ask, AppHost.FULL, from, question?.trim()?.takeIf { it.isNotEmpty() })
}

/** Types [phrases] out one after another, looping; frozen on the current phrase while [paused]. */
@Composable
fun rememberTypewriter(phrases: List<String>, paused: Boolean): String {
    var text by remember { mutableStateOf(if (prefersReducedMotion) phrases.first() else "") }
    LaunchedEffect(paused) {
        if (paused) return@LaunchedEffect
        var i = phrases.indexOfFirst { it.startsWith(text) && text.isNotEmpty() }.coerceAtLeast(0)
        while (true) {
            val phrase = phrases[i]
            if (prefersReducedMotion) {
                text = phrase
                delay(3500)
            } else {
                while (text.length < phrase.length) { text = phrase.take(text.length + 1); delay(42) }
                delay(2200)
                while (text.isNotEmpty()) { text = text.dropLast(1); delay(18) }
                delay(250)
            }
            i = (i + 1) % phrases.size
        }
    }
    return text
}

/**
 * Whether a floater should show right now: never over an open app or the contact sheet, never during the
 * first-visit boot, and (for the bottom bar) not while the desktop hero is mid-scroll, where the phone and
 * the launcher's own search bar sit at the bottom of the screen.
 */
@Composable
private fun rememberFloaterVisible(avoidHero: Boolean): Boolean {
    var ready by remember { mutableStateOf(false) }
    var inHero by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (document.documentElement?.classList?.contains("rr-booting") == true) delay(200)
        delay(900)
        ready = true
    }
    if (avoidHero) {
        DisposableEffect(Unit) {
            val update: (Event) -> Unit = {
                val scene = document.querySelector(".rr-scene")
                inHero = if (scene == null || window.innerWidth < 900) false else {
                    val r = scene.getBoundingClientRect()
                    r.top < -40 && r.bottom > window.innerHeight + 40
                }
            }
            val passive: dynamic = js("({passive: true})")
            window.addEventListener("scroll", update, passive)
            window.addEventListener("resize", update, passive)
            update(Event("scroll"))
            onDispose {
                window.removeEventListener("scroll", update, passive)
                window.removeEventListener("resize", update, passive)
            }
        }
    }
    return ready && !inHero && PhoneState.app == null && !UiState.contactOpen
}

/** Floating variants (the nav variant lives in [AskNavChip]). Placed once, in PageLayout. */
@Composable
fun AskFloater() {
    if (!Backend.enabled) return
    val path = rememberPageContext().route.path
    if (path.endsWith("/admin")) return
    when (AskFloaterState.style) {
        AskFab.BAR -> AskBar()
        AskFab.ORB -> AskOrb()
        AskFab.NAV -> Unit
    }
}

// ---------------------------------------------------------------------------
// A: bottom bar with a live text box
// ---------------------------------------------------------------------------

@Composable
private fun AskBar() {
    val visible = rememberFloaterVisible(avoidHero = true)
    var draft by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    val hint = rememberTypewriter(askTeasers, paused = focused || draft.isNotEmpty())

    DisposableEffect(Unit) {
        document.documentElement?.classList?.add("has-askbar")
        onDispose { document.documentElement?.classList?.remove("has-askbar") }
    }

    Form(attrs = {
        classes("askfab-bar")
        if (visible) classes("on")
        attr("data-origin", "")
        attr("aria-hidden", (!visible).toString())
        onSubmit { e ->
            e.preventDefault()
            // An empty bar asks the question it's showing.
            openAsk(e.target as? Element, draft.ifBlank { askTeasers.firstOrNull { hint.isNotEmpty() && it.startsWith(hint) } })
            draft = ""
        }
    }) {
        Span({ classes("askfab-spark"); attr("aria-hidden", "true") }) { I({ classes("fa-solid", "fa-wand-magic-sparkles") }) }
        Span({ classes("askfab-label") }) { Text("Ask AI") }
        Input(InputType.Text) {
            value(draft)
            placeholder(hint.ifEmpty { " " })
            maxLength(300)
            attr("aria-label", "Ask anything about Ritwik")
            attr("autocomplete", "off")
            attr("enterkeyhint", "send")
            if (!visible) attr("tabindex", "-1")
            onInput { draft = it.value }
            onFocus { focused = true }
            onBlur { focused = false }
        }
        Button({
            classes("askfab-send")
            attr("type", "submit")
            attr("aria-label", "Ask")
            if (!visible) attr("tabindex", "-1")
        }) { I({ classes("fa-solid", "fa-arrow-up") }) }
    }
}

// ---------------------------------------------------------------------------
// B: corner orb with a teaser bubble
// ---------------------------------------------------------------------------

private const val ORB_DISMISSED = "rr-askorb-dismissed"

@Composable
private fun AskOrb() {
    val visible = rememberFloaterVisible(avoidHero = false)
    var bubble by remember { mutableStateOf(false) }
    var teaser by remember { mutableStateOf(0) }

    LaunchedEffect(visible) {
        if (!visible) { bubble = false; return@LaunchedEffect }
        if (runCatching { window.sessionStorage.getItem(ORB_DISMISSED) }.getOrNull() != null) return@LaunchedEffect
        delay(2500)
        bubble = true
        while (true) {
            delay(5000)
            teaser = (teaser + 1) % askTeasers.size
        }
    }

    Div({
        classes("askfab-orb-wrap")
        if (visible) classes("on")
        attr("aria-hidden", (!visible).toString())
    }) {
        if (bubble) {
            Div({ classes("askfab-bubble") }) {
                Button({
                    classes("askfab-bubble-q")
                    attr("data-origin", "")
                    onClick { e -> openAsk(e.target as? Element, askTeasers[teaser]) }
                }) {
                    Span({ classes("k") }) { Text("Ask me") }
                    key(teaser) { Span({ classes("q") }) { Text(askTeasers[teaser]) } }
                }
                Button({
                    classes("askfab-bubble-x")
                    attr("aria-label", "Hide suggestion")
                    onClick {
                        bubble = false
                        runCatching { window.sessionStorage.setItem(ORB_DISMISSED, "1") }
                    }
                }) { I({ classes("fa-solid", "fa-xmark") }) }
            }
        }
        Button({
            classes("askfab-orb")
            attr("data-origin", "")
            attr("aria-label", "Ask AI about Ritwik")
            if (!visible) attr("tabindex", "-1")
            onClick { e -> openAsk(e.target as? Element) }
        }) {
            Span({ classes("ring"); attr("aria-hidden", "true") })
            I({ classes("fa-solid", "fa-wand-magic-sparkles") })
        }
    }
}

// ---------------------------------------------------------------------------
// C: chip inside the floating nav
// ---------------------------------------------------------------------------

/** "Ask AI" chip for the nav pill; on wide screens it types out a question next to the label. */
@Composable
fun AskNavChip() {
    if (!Backend.enabled || AskFloaterState.style != AskFab.NAV) return
    val hint = rememberTypewriter(askTeasers, paused = false)
    Button({
        classes("askfab-chip")
        attr("data-origin", "")
        attr("aria-label", "Ask AI about Ritwik")
        onClick { e -> openAsk(e.target as? Element) }
    }) {
        I({ classes("fa-solid", "fa-wand-magic-sparkles"); attr("aria-hidden", "true") })
        Span({ classes("t") }) { Text("Ask AI") }
        Span({ classes("q"); attr("aria-hidden", "true") }) { Text(hint) }
    }
}
