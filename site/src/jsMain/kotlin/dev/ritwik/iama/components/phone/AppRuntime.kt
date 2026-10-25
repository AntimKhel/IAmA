package dev.ritwik.iama.components.phone

import androidx.compose.runtime.*
import dev.ritwik.iama.components.phone.apps.AboutApp
import dev.ritwik.iama.components.phone.apps.AwardsApp
import dev.ritwik.iama.components.phone.apps.BlogApp
import dev.ritwik.iama.components.phone.apps.ContactApp
import dev.ritwik.iama.components.phone.apps.GuestbookApp
import dev.ritwik.iama.components.phone.apps.AskApp
import dev.ritwik.iama.components.phone.apps.ResumeApp
import dev.ritwik.iama.components.phone.apps.SearchApp
import dev.ritwik.iama.components.phone.apps.SkillsApp
import dev.ritwik.iama.components.phone.apps.StoreApp
import dev.ritwik.iama.components.phone.apps.WorkApp
import kotlinx.browser.document
import kotlinx.browser.window
import dev.ritwik.iama.util.haptic
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent

/** Screens that open as in-phone apps. */
enum class AppScreen { WORK, STORE, BLOG, ABOUT, SKILLS, AWARDS, CONTACT, GUESTBOOK, SEARCH, RESUME, ASK }

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
        this.payload = payload
        origin = clicked?.let { originOf(it, host) }
        this.host = host
        closing = false
        this.app = app
    }

    fun close() {
        if (app != null) {
            haptic(6)
            closing = true
        }
    }

    internal fun finishClose() {
        app = null
        closing = false
    }

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

/** Renders the open app if it belongs to [host]. One host lives in the phone, one in the page layout. */
@Composable
fun AppWindowHost(host: AppHost) {
    val app = PhoneState.app ?: return
    if (PhoneState.host != host) return
    val screen = (app.action as? AppAction.Open)?.screen ?: return
    val closing = PhoneState.closing

    LaunchedEffect(closing) {
        if (closing) {
            delay(340)
            PhoneState.finishClose()
        }
    }

    DisposableEffect(Unit) {
        val onKey: (Event) -> Unit = { if ((it as KeyboardEvent).key == "Escape") PhoneState.close() }
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
                }
            }
            // Splash: the icon's colour fills the window, then fades as content appears.
            Div({ classes("rr-appwin-splash"); attr("aria-hidden", "true") }) {
                I({ classes(*app.icon.split(" ").toTypedArray()) })
            }
            Button({
                classes("rr-appwin-gesture")
                attr("aria-label", "Close ${app.label}")
                onClick { PhoneState.close() }
            })
        }
    }
}

/**
 * Top bar shared by every app. [onBack] defaults to closing the app; screens with
 * internal navigation (a post, a product page) pass their own.
 */
@Composable
fun AppBar(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null, actions: @Composable () -> Unit = {}) {
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
