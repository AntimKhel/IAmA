package dev.ritwik.iama.components.ask

import androidx.compose.runtime.*
import com.varabyte.kobweb.core.rememberPageContext
import dev.ritwik.iama.components.phone.AppHost
import dev.ritwik.iama.components.phone.Apps
import dev.ritwik.iama.components.phone.PhoneState
import dev.ritwik.iama.data.Backend
import dev.ritwik.iama.util.UiState
import dev.ritwik.iama.util.jsObject
import dev.ritwik.iama.util.prefersReducedMotion
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event

/**
 * An always-there way into Ask Ritwik, for visitors who never tap the phone's Ask icon.
 *
 * Like a Dynamic Island: during the hero it's a chip in the nav that types out questions. Once the visitor
 * scrolls past the hero, the chip contracts, drips off the nav as a liquid blob and lands bottom-right as
 * an orb (with a teaser bubble); scrolling back up reverses it. The liquid is a short-lived, filtered blob
 * layer, so the chip and the orb themselves stay crisp.
 */
private enum class Phase { DOCKED, LEAVING, DRIPPING, FLOATING, RETURNING }

private object Island {
    var phase by mutableStateOf(Phase.DOCKED)
}

/** Questions the chip types out and the orb teases with. Each one is answerable from the site. */
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

// ---------------------------------------------------------------------------
// The nav chip (docked state)
// ---------------------------------------------------------------------------

/** "Ask AI" chip for the nav pill; on wide screens it types out a question next to the label. */
@Composable
fun AskNavChip() {
    if (!Backend.enabled) return
    val phase = Island.phase
    val hint = rememberTypewriter(askTeasers, paused = phase != Phase.DOCKED)
    Button({
        classes("askfab-chip")
        when (phase) {
            Phase.DOCKED -> {}
            Phase.LEAVING -> classes("is-dot") // contracts into a dot
            Phase.DRIPPING, Phase.RETURNING -> classes("is-dot", "is-hidden") // the liquid stands in for it
            Phase.FLOATING -> classes("is-gone")
        }
        attr("data-origin", "")
        attr("aria-label", "Ask AI about Ritwik")
        if (phase != Phase.DOCKED) attr("tabindex", "-1")
        onClick { e -> openAsk(e.target as? Element) }
    }) {
        I({ classes("fa-solid", "fa-wand-magic-sparkles"); attr("aria-hidden", "true") })
        Span({ classes("t") }) { Text("Ask AI") }
        Span({ classes("q"); attr("aria-hidden", "true") }) { Text(hint) }
    }
}

// ---------------------------------------------------------------------------
// Orb + liquid transition (placed once, in PageLayout)
// ---------------------------------------------------------------------------

@Composable
fun AskFloater() {
    if (!Backend.enabled) return
    val path = rememberPageContext().route.path
    if (path.endsWith("/admin")) return

    var ready by remember { mutableStateOf(false) }
    var docked by remember { mutableStateOf(true) }
    var liquid by remember { mutableStateOf<LiquidRun?>(null) }
    // While the liquid delivers or collects the orb, it appears/disappears in place instead of popping.
    var instant by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (document.documentElement?.classList?.contains("rr-booting") == true) delay(200)
        delay(600)
        ready = true
    }

    // Docked while the hero (landing + phone scene) is on screen; elsewhere, near the top of the page.
    // The two thresholds keep it from flickering back and forth at the boundary.
    DisposableEffect(path) {
        val update: (Event) -> Unit = {
            val scene = document.querySelector(".rr-scene")
            val leave: Boolean
            val back: Boolean
            if (scene != null) {
                val bottom = scene.getBoundingClientRect().bottom
                leave = bottom < window.innerHeight * 0.45
                back = bottom > window.innerHeight * 0.65
            } else {
                leave = window.scrollY > 320
                back = window.scrollY < 200
            }
            if (docked && leave) docked = false else if (!docked && back) docked = true
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

    // Drive the island between its two homes, with the liquid hand-off in between.
    LaunchedEffect(docked, ready) {
        liquid = null // a reversal mid-way cancels the previous hand-off
        if (!ready) return@LaunchedEffect
        val target = if (docked) Phase.DOCKED else Phase.FLOATING
        val now = Island.phase
        if (now != Phase.DOCKED && now != Phase.FLOATING) {
            Island.phase = target // reversed mid-way: settle without another animation
            return@LaunchedEffect
        }
        if (now == target) return@LaunchedEffect
        val canAnimate = !prefersReducedMotion && PhoneState.app == null
        if (!docked) {
            Island.phase = Phase.LEAVING
            if (canAnimate) {
                delay(260)
                val from = chipCenter()
                val to = orbCenter()
                if (from != null && to != null) {
                    instant = true
                    liquid = LiquidRun(from, to, leaving = true)
                    Island.phase = Phase.DRIPPING
                    delay((LIQUID_MS * 0.45).toLong()) // the drop has pinched off: let the nav close up behind it
                    Island.phase = Phase.FLOATING
                    delay((LIQUID_MS * 0.55).toLong())
                    liquid = null
                    delay(300)
                    instant = false
                }
            }
            Island.phase = Phase.FLOATING
        } else {
            Island.phase = Phase.RETURNING // the chip takes its dot-sized place again, invisibly, so we can aim at it
            if (canAnimate) {
                delay(40)
                val from = orbCenter()
                val to = chipCenter()
                if (from != null && to != null) {
                    instant = true
                    liquid = LiquidRun(from, to, leaving = false)
                    delay(LIQUID_MS.toLong())
                    liquid = null
                    instant = false
                }
            }
            Island.phase = Phase.DOCKED
        }
    }

    val orbVisible = ready && Island.phase == Phase.FLOATING && liquid == null && PhoneState.app == null && !UiState.contactOpen
    AskOrb(orbVisible, instant)
    liquid?.let { key(it) { Liquid(it) } }
}

private const val LIQUID_MS = 900

private class Point(val x: Double, val y: Double)

private class LiquidRun(val from: Point, val to: Point, val leaving: Boolean)

private fun chipCenter(): Point? {
    val r = (document.querySelector(".askfab-chip") as? HTMLElement)?.getBoundingClientRect() ?: return null
    return Point(r.left + r.height / 2, r.top + r.height / 2)
}

/** Where the orb sits when shown (layout position; its hidden state is only a transform). */
private fun orbCenter(): Point? {
    val wrap = document.querySelector(".askfab-orb-wrap") as? HTMLElement ?: return null
    val orb = wrap.querySelector(".askfab-orb") as? HTMLElement ?: return null
    return Point(
        wrap.offsetLeft + orb.offsetLeft + orb.offsetWidth / 2.0,
        wrap.offsetTop + orb.offsetTop + orb.offsetHeight / 2.0,
    )
}

/**
 * The hand-off: two gooey blobs under an SVG "goo" filter (blur + alpha threshold), so they merge and
 * separate like liquid. One stays at the nav and shrinks away (or grows in); the other travels.
 */
@Composable
private fun Liquid(run: LiquidRun) {
    Div({
        classes("askfab-liquid")
        attr("aria-hidden", "true")
        ref { el ->
            el.innerHTML = """
                <svg width="0" height="0" style="position:absolute"><filter id="askfab-goo">
                  <feGaussianBlur in="SourceGraphic" stdDeviation="8" result="b"/>
                  <feColorMatrix in="b" mode="matrix" values="1 0 0 0 0  0 1 0 0 0  0 0 1 0 0  0 0 0 20 -9"/>
                </filter></svg>
                <div class="askfab-goo"><span class="blob anchor"></span><span class="blob drop"></span></div>
            """.trimIndent()
            val anchor = el.querySelector(".anchor") as HTMLElement
            val drop = el.querySelector(".drop") as HTMLElement
            animateLiquid(anchor, drop, run)
            onDispose { }
        }
    })
}

private const val BLOB = 38.0

private fun at(p: Point, sx: Double, sy: Double, dx: Double = 0.0, dy: Double = 0.0) =
    "translate(${p.x + dx - BLOB / 2}px, ${p.y + dy - BLOB / 2}px) scale($sx, $sy)"

private fun animateLiquid(anchor: HTMLElement, drop: HTMLElement, run: LiquidRun) {
    val nav = if (run.leaving) run.from else run.to
    val orb = if (run.leaving) run.to else run.from
    val orbScale = 64.0 / BLOB
    // A point a little below the nav, then a gentle arc out to the corner.
    val neck = Point(nav.x, nav.y + 44)
    val mid = Point(nav.x + (orb.x - nav.x) * 0.55, nav.y + (orb.y - nav.y) * 0.35)

    val dropFrames = arrayOf(
        jsObject("offset" to 0.0, "transform" to at(nav, 1.0, 1.0)),
        jsObject("offset" to 0.22, "transform" to at(neck, 0.8, 1.3)),
        jsObject("offset" to 0.36, "transform" to at(neck, 0.95, 1.1, dy = 40.0)),
        jsObject("offset" to 0.68, "transform" to at(mid, 1.35, 1.05)),
        jsObject("offset" to 0.88, "transform" to at(orb, orbScale * 1.18, orbScale * 0.86)),
        jsObject("offset" to 1.0, "transform" to at(orb, orbScale, orbScale)),
    )
    val anchorFrames = arrayOf(
        jsObject("offset" to 0.0, "transform" to at(nav, 1.0, 1.0)),
        jsObject("offset" to 0.2, "transform" to at(nav, 0.9, 1.05, dy = 4.0)),
        jsObject("offset" to 0.34, "transform" to at(nav, 0.45, 0.45)),
        jsObject("offset" to 0.45, "transform" to at(nav, 0.0, 0.0)),
        jsObject("offset" to 1.0, "transform" to at(nav, 0.0, 0.0)),
    )
    if (!run.leaving) {
        dropFrames.reverse(); dropFrames.forEach { it.offset = 1.0 - (it.offset as Double) }
        anchorFrames.reverse(); anchorFrames.forEach { it.offset = 1.0 - (it.offset as Double) }
    }
    val timing = jsObject("duration" to LIQUID_MS, "easing" to "cubic-bezier(0.45, 0, 0.2, 1)", "fill" to "forwards")
    drop.asDynamic().animate(dropFrames, timing)
    anchor.asDynamic().animate(anchorFrames, timing)
}

private const val ORB_DISMISSED = "rr-askorb-dismissed"

@Composable
private fun AskOrb(visible: Boolean, instant: Boolean) {
    var bubble by remember { mutableStateOf(false) }
    var teaser by remember { mutableStateOf(0) }

    LaunchedEffect(visible) {
        if (!visible) { bubble = false; return@LaunchedEffect }
        if (runCatching { window.sessionStorage.getItem(ORB_DISMISSED) }.getOrNull() != null) return@LaunchedEffect
        delay(1400)
        bubble = true
        while (true) {
            delay(5000)
            teaser = (teaser + 1) % askTeasers.size
        }
    }

    Div({
        classes("askfab-orb-wrap")
        if (visible) classes("on")
        if (instant) classes("instant")
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
