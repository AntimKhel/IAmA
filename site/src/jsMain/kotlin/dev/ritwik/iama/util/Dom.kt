package dev.ritwik.iama.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.varabyte.kobweb.navigation.BasePath
import com.varabyte.kobweb.navigation.Router
import dev.ritwik.iama.data.Profile
import kotlinx.browser.document
import kotlinx.browser.window
import kotlin.math.round

/** App-wide UI state shared between the nav, hero, phone apps and contact sheet. */
object UiState {
    var contactOpen by mutableStateOf(false)
    var toast by mutableStateOf<String?>(null)
    private var toastSeq = 0

    fun showToast(message: String) {
        val id = ++toastSeq
        toast = message
        window.setTimeout({ if (toastSeq == id) toast = null }, 2200)
    }
}

/** Classes on <html> that gate the hero choreography (see rr.css). */
object HtmlState {
    private val root get() = document.documentElement

    val isBooting: Boolean get() = root?.classList?.contains("rr-booting") == true

    fun ready() {
        root?.classList?.remove("rr-booting")
        root?.classList?.add("rr-ready")
    }
}

val prefersReducedMotion: Boolean
    get() = window.matchMedia("(prefers-reduced-motion: reduce)").matches

val resumeHref: String get() = BasePath.prependTo(Profile.resumePath)

val phoneNumber: String get() = Profile.phoneParts.joinToString(" ")

fun copyToClipboard(text: String, label: String) {
    val clipboard = window.navigator.asDynamic().clipboard
    if (clipboard != null) {
        clipboard.writeText(text).then({ UiState.showToast("$label copied") }, { UiState.showToast(text) })
    } else {
        UiState.showToast(text)
    }
}

/** Smooth-scrolls to an element on the current page, or routes home and jumps to it. */
fun goToSection(id: String, router: Router) {
    val el = document.getElementById(id)
    if (el != null) {
        el.asDynamic().scrollIntoView(js("({behavior: 'smooth', block: 'start'})"))
    } else {
        router.tryRoutingTo("/#$id")
    }
}

/** Rounds to 4 decimals so CSS custom properties stay short and stable. */
fun Double.css(): String = (round(this * 10000) / 10000).toString()

fun clamp01(v: Double) = if (v < 0.0) 0.0 else if (v > 1.0) 1.0 else v
