package dev.ritwik.iama.components.ask

import androidx.compose.runtime.*
import com.varabyte.kobweb.core.rememberPageContext
import dev.ritwik.iama.components.phone.AppHost
import dev.ritwik.iama.components.phone.Apps
import dev.ritwik.iama.components.phone.PhoneState
import dev.ritwik.iama.data.Backend
import dev.ritwik.iama.util.prefersReducedMotion
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.Element
import org.w3c.dom.events.Event

/** Questions the nav chip types out once the visitor is past the hero. Each one is answerable from the site. */
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

/** Types [phrases] out one after another, looping; frozen on the current text while [paused]. */
@Composable
fun rememberTypewriter(phrases: List<String>, paused: Boolean): String {
    var text by remember { mutableStateOf(if (prefersReducedMotion) phrases.first() else "") }
    LaunchedEffect(paused) {
        if (paused) return@LaunchedEffect
        var i = phrases.indexOfFirst { text.isNotEmpty() && it.startsWith(text) }.coerceAtLeast(0)
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
 * True once the visitor has scrolled past the hero (landing + phone scene); on other pages, past the top.
 * Separate leave/return thresholds stop it flickering at the boundary.
 */
@Composable
private fun rememberPastHero(): Boolean {
    val path = rememberPageContext().route.path
    var past by remember { mutableStateOf(false) }
    DisposableEffect(path) {
        val update: (Event) -> Unit = {
            val scene = document.querySelector(".rr-scene")
            if (scene != null) {
                val bottom = scene.getBoundingClientRect().bottom
                if (!past && bottom < window.innerHeight * 0.45) past = true
                else if (past && bottom > window.innerHeight * 0.65) past = false
            } else {
                if (!past && window.scrollY > 320) past = true
                else if (past && window.scrollY < 200) past = false
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
    return past
}

/**
 * "Ask AI" chip in the nav. During the hero it's just the label; past the hero it widens and types out
 * questions. Clicking a question asks it; clicking the label opens Ask.
 */
@Composable
fun AskNavChip() {
    if (!Backend.enabled) return
    val expanded = rememberPastHero()
    val hint = rememberTypewriter(askTeasers, paused = !expanded)
    Button({
        classes("askfab-chip")
        if (expanded) classes("is-expanded")
        attr("data-origin", "")
        attr("aria-label", "Ask AI about Ritwik")
        onClick { e ->
            val onPrompt = (e.target as? Element)?.closest(".q") != null
            val question = if (expanded && onPrompt) askTeasers.firstOrNull { hint.isNotEmpty() && it.startsWith(hint) } else null
            openAsk(e.target as? Element, question)
        }
    }) {
        I({ classes("fa-solid", "fa-wand-magic-sparkles"); attr("aria-hidden", "true") })
        Span({ classes("t") }) { Text("Ask AI") }
        Span({ classes("q"); attr("aria-hidden", "true") }) { Text(hint) }
    }
}
