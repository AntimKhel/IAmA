package dev.ritwik.iama.components.phone

import androidx.compose.runtime.*
import dev.ritwik.iama.components.devtools.DevOptions
import dev.ritwik.iama.components.phone.apps.AboutApp
import dev.ritwik.iama.components.phone.apps.AskApp
import dev.ritwik.iama.components.phone.apps.AwardsApp
import dev.ritwik.iama.components.phone.apps.BlogApp
import dev.ritwik.iama.components.phone.apps.ContactApp
import dev.ritwik.iama.components.phone.apps.GuestbookApp
import dev.ritwik.iama.components.phone.apps.LogcatApp
import dev.ritwik.iama.components.phone.apps.ResumeApp
import dev.ritwik.iama.components.phone.apps.SearchApp
import dev.ritwik.iama.components.phone.apps.SettingsApp
import dev.ritwik.iama.components.phone.apps.SkillsApp
import dev.ritwik.iama.components.phone.apps.StatsApp
import dev.ritwik.iama.components.phone.apps.StoreApp
import dev.ritwik.iama.components.phone.apps.WorkApp
import dev.ritwik.iama.util.Logcat
import dev.ritwik.iama.util.haptic
import dev.ritwik.iama.util.jsObject
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import kotlin.math.abs

/** Screens that open as in-phone apps. */
enum class AppScreen { WORK, STORE, BLOG, ABOUT, SKILLS, AWARDS, CONTACT, GUESTBOOK, SEARCH, RESUME, ASK, SETTINGS, STATS, LOGCAT }

/** Where an app window is drawn: inside the hero's phone mockup, or over the whole viewport. */
enum class AppHost { PHONE, FULL }

val LocalAppHost = compositionLocalOf { AppHost.FULL }

/** Insets (px) of the launching icon inside its host, so the window can grow out of it. */
class Origin(val top: Double, val right: Double, val bottom: Double, val left: Double)

object PhoneState {
    var app by mutableStateOf<LauncherApp?>(null)
        private set
    var host by mutableStateOf(AppHost.FULL)
        private set
    var origin by mutableStateOf<Origin?>(null)
        private set
    var closing by mutableStateOf(false)
        private set
    /** Optional deep link into the app, e.g. the product to show first in Products. */
    var payload by mutableStateOf<Any?>(null)
        private set

    fun open(app: LauncherApp, host: AppHost, clicked: Element?, payload: Any? = null) {
        haptic()
        val wasOpen = this.app != null
        this.payload = payload
        origin = clicked?.let { originOf(it, host) }
        this.host = host
        closing = false
        this.app = app
        Logcat.i("ActivityManager", "START u0 {cmp=${Logcat.PACKAGE}/.apps.${activityName(app)}${if (payload != null) " (has extras)" else ""}} from ${if (host == AppHost.PHONE) "hero phone" else "launcher"}")
        if (!wasOpen) BackDispatcher.onAppOpened()
    }

    fun close() {
        if (app != null && !closing) {
            haptic(6)
            closing = true
        }
    }

    internal fun finishClose() {
        app?.let { Logcat.i("ActivityManager", "Activity finished: ${Logcat.PACKAGE}/.apps.${activityName(it)}") }
        app = null
        closing = false
        BackDispatcher.onAppClosed()
    }

    private fun activityName(app: LauncherApp) = app.label.filter { it.isLetterOrDigit() } + "Activity"

    private fun originOf(clicked: Element, host: AppHost): Origin? {
        val icon = (clicked.closest(".rr-app")?.querySelector(".ico") ?: clicked.closest("[data-origin]") ?: return null) as HTMLElement
        val r = icon.getBoundingClientRect()
        return when (host) {
            AppHost.FULL -> Origin(r.top, window.innerWidth - r.right, window.innerHeight - r.bottom, r.left)
            AppHost.PHONE -> {
                val screen = icon.closest(".rr-screen") as? HTMLElement ?: return null
                val c = screen.getBoundingClientRect()
                // The phone is scaled/tilted; convert from screen pixels back to the screen's own layout pixels.
                val k = if (c.width > 0) screen.offsetWidth / c.width else 1.0
                Origin((r.top - c.top) * k, (c.right - r.right) * k, (c.bottom - r.bottom) * k, (r.left - c.left) * k)
            }
        }
    }
}

/**
 * Back, the Android way: an app's own screen stack first (a post, a product page), then closing the app.
 * Escape, the predictive back gesture and the browser's / phone's real back button all go through here:
 * opening an app adds a history entry, so system back closes the app instead of leaving the site.
 */
object BackDispatcher {
    /** Registered by an [AppBar] that has its own back (see below). */
    internal var inAppBack: (() -> Unit)? = null
    private var armed = false
    private var armedPath = ""
    private var installed = false

    val hasInAppBack: Boolean get() = inAppBack != null

    fun back() {
        val inApp = inAppBack
        if (inApp != null) inApp() else PhoneState.close()
    }

    internal fun onAppOpened() {
        if (armed) return
        runCatching {
            window.history.pushState(jsObject("rrApp" to true), "", window.location.href)
            armed = true
            armedPath = window.location.pathname
        }
    }

    internal fun onAppClosed() {
        if (!armed) return
        armed = false
        // Drop our entry again, unless the app navigated somewhere (then that page owns the history).
        if (window.location.pathname == armedPath) runCatching { window.history.back() }
    }

    fun install() {
        if (installed) return
        installed = true
        window.addEventListener("popstate", {
            if (armed && PhoneState.app != null) {
                armed = false
                Logcat.d("OnBackPressedDispatcher", "system back")
                val inApp = inAppBack
                if (inApp != null) {
                    inApp()
                    onAppOpened() // still in the app: keep catching back
                } else {
                    PhoneState.close()
                }
            }
        })
    }
}

/** Renders the open app if it belongs to [host]. One host lives in the phone, one in the page layout. */
@Composable
fun AppWindowHost(host: AppHost) {
    val app = PhoneState.app ?: return
    if (PhoneState.host != host) return
    val screen = (app.action as? AppAction.Open)?.screen ?: return
    val closing = PhoneState.closing

    LaunchedEffect(closing) {
        if (closing) {
            delay((340 * DevOptions.effectiveWindowScale).toLong().coerceAtLeast(16))
            PhoneState.finishClose()
        }
    }

    DisposableEffect(Unit) {
        val onKey: (Event) -> Unit = { if ((it as KeyboardEvent).key == "Escape") BackDispatcher.back() }
        window.addEventListener("keydown", onKey)
        // Keyboard: move focus into the app (its search box, else its back button) and restore it on close.
        val opener = document.activeElement as? HTMLElement
        window.setTimeout({
            val win = document.querySelector(".rr-appwin:not(.is-closing)")
            ((win?.querySelector("[data-autofocus]") ?: win?.querySelector(".rr-appbar-back")) as? HTMLElement)?.focus()
        }, 60)

        // An open app is modal: the page underneath stops scrolling until it closes.
        val root = document.documentElement as? HTMLElement
        val previousOverflow = root?.style?.getPropertyValue("overflow") ?: ""
        root?.style?.setProperty("overflow", "hidden")
        onDispose {
            window.removeEventListener("keydown", onKey)
            root?.style?.setProperty("overflow", previousOverflow)
            if (opener != null && document.body?.contains(opener) == true) opener.focus()
        }
    }

    val o = PhoneState.origin
    key(app) {
        Div({
            classes("rr-appwin", if (host == AppHost.PHONE) "in-phone" else "full")
            if (closing) classes("is-closing")
            if (o == null) classes("no-origin")
            attr("role", "dialog")
            attr("aria-label", app.label)
            style {
                property("--a", app.from)
                property("--b", app.to)
                if (o != null) {
                    property("--ot", "${o.top}px")
                    property("--or", "${o.right}px")
                    property("--ob", "${o.bottom}px")
                    property("--ol", "${o.left}px")
                }
            }
            ref { win -> val dispose = predictiveBack(win); onDispose { dispose() } }
        }) {
            Div({ classes("rr-appwin-body") }) {
                when (screen) {
                    AppScreen.WORK -> WorkApp()
                    AppScreen.STORE -> StoreApp()
                    AppScreen.BLOG -> BlogApp()
                    AppScreen.ABOUT -> AboutApp()
                    AppScreen.SKILLS -> SkillsApp()
                    AppScreen.AWARDS -> AwardsApp()
                    AppScreen.CONTACT -> ContactApp()
                    AppScreen.GUESTBOOK -> GuestbookApp()
                    AppScreen.SEARCH -> SearchApp()
                    AppScreen.RESUME -> ResumeApp()
                    AppScreen.ASK -> AskApp()
                    AppScreen.SETTINGS -> SettingsApp()
                    AppScreen.STATS -> StatsApp()
                    AppScreen.LOGCAT -> LogcatApp()
                }
            }
            // Splash: the icon's colour fills the window, then fades as content appears.
            Div({ classes("rr-appwin-splash"); attr("aria-hidden", "true") }) {
                I({ classes(*app.icon.split(" ").toTypedArray()) })
            }
            Div({ classes("rr-back-arrow"); attr("aria-hidden", "true") }) { I({ classes("fa-solid", "fa-chevron-left") }) }
            Button({
                classes("rr-appwin-gesture")
                attr("aria-label", "Close ${app.label}")
                onClick { PhoneState.close() }
            })
        }
    }
}

/**
 * Android's predictive back: drag in from either edge of the window and it shrinks, rounding its corners,
 * with the back arrow growing at the edge, so you can see where back will take you. Let go past a third of
 * the way to go back (to the previous screen in the app, or out to the home screen); otherwise it springs
 * back. Returns a function that removes the listeners.
 */
private fun predictiveBack(win: HTMLElement): () -> Unit {
    val zone = 26.0
    var tracking = false
    var dragging = false
    var edge = 0 // -1 dragging from the left edge, 1 from the right
    var startX = 0.0
    var startY = 0.0
    var progress = 0.0
    var width = 1.0
    var pointerId = -1
    val arrow = win.querySelector(".rr-back-arrow") as? HTMLElement
    val body = win.querySelector(".rr-appwin-body") as? HTMLElement

    fun target(): HTMLElement = if (BackDispatcher.hasInAppBack) (body ?: win) else win
    fun scaled(ms: Int) = (ms * DevOptions.effectiveWindowScale).coerceAtLeast(1.0)
    // A full-screen app centres its body with a transform; keep it while sliding the body.
    fun bodyBase() = if (win.classList.contains("full")) "translateX(-50%) " else ""

    fun paint(dy: Double) {
        val p = progress
        val el = target()
        if (el === win) {
            el.style.transform = "translate(${-edge * p * 0.06 * width}px, ${dy * 0.08}px) scale(${1 - 0.12 * p})"
            el.style.borderRadius = "${22 + 18 * p}px"
        } else {
            el.style.transform = "${bodyBase()}translateX(${-edge * p * 0.12 * width}px)"
            el.style.opacity = "${1 - 0.35 * p}"
        }
        arrow?.let {
            it.style.top = "${startY - win.getBoundingClientRect().top + dy}px"
            it.style.setProperty("--p", "$p")
            it.classList.toggle("left", edge == -1)
            it.classList.toggle("armed", p >= 0.35)
        }
    }

    fun reset(el: HTMLElement) {
        el.style.removeProperty("transform")
        el.style.removeProperty("border-radius")
        el.style.removeProperty("opacity")
        win.classList.remove("back-dragging")
    }

    val down: (Event) -> Unit = down@{ ev ->
        val e = ev.asDynamic()
        if (!DevOptions.backAnimations || PhoneState.closing || (e.button as? Int ?: 0) != 0) return@down
        val r = win.getBoundingClientRect()
        val x = e.clientX as Double
        edge = when {
            x - r.left < zone -> -1
            r.right - x < zone -> 1
            else -> 0
        }
        if (edge == 0) return@down
        tracking = true
        dragging = false
        startX = x
        startY = e.clientY as Double
        width = r.width
        progress = 0.0
        pointerId = e.pointerId as Int
    }
    val move: (Event) -> Unit = move@{ ev ->
        val e = ev.asDynamic()
        if (!tracking || e.pointerId != pointerId) return@move
        val dx = ((e.clientX as Double) - startX) * -edge
        val dy = (e.clientY as Double) - startY
        if (!dragging) {
            if (abs(dy) > 24 && abs(dy) > dx) { tracking = false; return@move } // a vertical scroll, not back
            if (dx < 8) return@move
            dragging = true
            win.classList.add("settled", "back-dragging")
            runCatching { win.setPointerCapture(pointerId) }
            Logcat.d("OnBackPressedDispatcher", "back gesture started (${if (edge == -1) "left" else "right"} edge)")
        }
        e.preventDefault()
        progress = (dx / (width * 0.42)).coerceIn(0.0, 1.0)
        paint(dy)
    }
    val up: (Event) -> Unit = up@{ ev ->
        val e = ev.asDynamic()
        if (!tracking || e.pointerId != pointerId) return@up
        tracking = false
        if (!dragging) return@up
        dragging = false
        val el = target()
        val current = el.style.transform
        if (progress >= 0.35) {
            haptic(10)
            Logcat.d("OnBackPressedDispatcher", "back gesture committed (progress ${(progress * 100).toInt()}%)")
            if (el === win) {
                val anim = win.asDynamic().animate(
                    arrayOf(jsObject("transform" to current, "opacity" to 1), jsObject("transform" to "$current scale(0.86)", "opacity" to 0)),
                    jsObject("duration" to scaled(200), "easing" to "cubic-bezier(0.4, 0, 1, 1)", "fill" to "forwards"),
                )
                anim.onfinish = { PhoneState.finishClose() }
            } else {
                val anim = el.asDynamic().animate(
                    arrayOf(jsObject("transform" to current, "opacity" to el.style.opacity), jsObject("transform" to "${bodyBase()}translateX(${-edge * 0.3 * width}px)", "opacity" to 0)),
                    jsObject("duration" to scaled(160), "easing" to "ease-in"),
                )
                anim.onfinish = {
                    BackDispatcher.back()
                    reset(el)
                    el.asDynamic().animate(
                        arrayOf(jsObject("opacity" to 0, "transform" to "${bodyBase()}translateX(${edge * 24}px)"), jsObject("opacity" to 1, "transform" to bodyBase().trim().ifEmpty { "none" })),
                        jsObject("duration" to scaled(220), "easing" to "cubic-bezier(0.2, 0.9, 0.1, 1)"),
                    )
                }
            }
        } else {
            val rest = if (el === win) "none" else bodyBase().trim().ifEmpty { "none" }
            val anim = el.asDynamic().animate(
                arrayOf(jsObject("transform" to current), jsObject("transform" to rest)),
                jsObject("duration" to scaled(260), "easing" to "cubic-bezier(0.34, 1.56, 0.64, 1)"),
            )
            anim.onfinish = { reset(el) }
            arrow?.style?.setProperty("--p", "0")
        }
    }
    win.addEventListener("pointerdown", down)
    win.addEventListener("pointermove", move)
    win.addEventListener("pointerup", up)
    win.addEventListener("pointercancel", up)
    return {
        win.removeEventListener("pointerdown", down)
        win.removeEventListener("pointermove", move)
        win.removeEventListener("pointerup", up)
        win.removeEventListener("pointercancel", up)
    }
}

/**
 * Top bar shared by every app. [onBack] defaults to closing the app; screens with internal navigation (a
 * post, a product page) pass their own, which also becomes what Escape, system back and the predictive
 * back gesture do.
 */
@Composable
fun AppBar(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null, actions: @Composable () -> Unit = {}) {
    if (onBack != null) {
        val latest by rememberUpdatedState(onBack)
        DisposableEffect(Unit) {
            val handler: () -> Unit = { latest() }
            BackDispatcher.inAppBack = handler
            onDispose { if (BackDispatcher.inAppBack === handler) BackDispatcher.inAppBack = null }
        }
    }
    Div({ classes("rr-appbar") }) {
        Button({
            classes("rr-appbar-back")
            attr("aria-label", if (onBack != null) "Back" else "Close")
            onClick { (onBack ?: { PhoneState.close() })() }
        }) { I({ classes("fa-solid", "fa-arrow-left") }) }
        Div({ classes("rr-appbar-title") }) {
            Div({ classes("t") }) { Text(title) }
            if (subtitle != null) Div({ classes("s") }) { Text(subtitle) }
        }
        Div({ classes("rr-appbar-actions") }) { actions() }
    }
}
