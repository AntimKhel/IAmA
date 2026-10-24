package dev.ritwik.iama.components.hero

import androidx.compose.runtime.*
import com.varabyte.kobweb.core.AppGlobals
import com.varabyte.kobweb.core.isExporting
import dev.ritwik.iama.components.phone.AppHost
import dev.ritwik.iama.components.phone.AppIcon
import dev.ritwik.iama.components.phone.Apps
import dev.ritwik.iama.components.phone.Dock
import dev.ritwik.iama.components.phone.PhoneFrame
import dev.ritwik.iama.components.phone.PhoneState
import dev.ritwik.iama.components.phone.rememberClock
import dev.ritwik.iama.data.Profile
import dev.ritwik.iama.data.floaterStats
import dev.ritwik.iama.data.heroStats
import dev.ritwik.iama.util.HtmlState
import dev.ritwik.iama.util.UiState
import dev.ritwik.iama.util.resumeHref
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event

private const val BOOT_KEY = "rr-booted"

@Composable
fun HeroScene() {
    // The inline <head> script decides whether this visit boots (first home visit of the session).
    var booting by remember { mutableStateOf(!AppGlobals.isExporting && HtmlState.isBooting) }

    LaunchedEffect(Unit) {
        if (AppGlobals.isExporting) return@LaunchedEffect
        HtmlState.restoreTapped()
        if (booting) {
            runCatching { window.sessionStorage.setItem(BOOT_KEY, "1") }
        } else {
            HtmlState.ready()
        }
    }

    if (booting) BootOverlay(onFinished = { booting = false })
    PhoneFocusEffect()

    Section({
        id("top")
        classes("rr-scene")
        ref { el ->
            val engine = SceneEngine(el)
            onDispose { engine.dispose() }
        }
    }) {
        Div({ classes("rr-sticky") }) {
            Div({ classes("rr-mesh"); attr("aria-hidden", "true") }) {
                listOf("b1", "b2", "b3", "b4").forEach { Div({ classes("rr-blob", it) }) }
            }
            Div({ classes("rr-floor"); attr("aria-hidden", "true") })
            Canvas({ classes("rr-particles"); attr("aria-hidden", "true") })
            Div({ classes("rr-grain"); attr("aria-hidden", "true") })
            Div({ classes("rr-vignette"); attr("aria-hidden", "true") })

            HeroCopy()

            Div({ classes("rr-stage"); attr("aria-hidden", "true") }) {
                Div({ classes("rr-phone-zoom") }) {
                    Div({ classes("rr-phone-tilt") }) {
                        Div({ classes("rr-phone-arrive") }) { PhoneFrame() }
                    }
                }
            }

            Floaters()
            TapCallout()
            Desk()
            ScrollHint()
            FocusBar()
        }
    }
}

// ---------------------------------------------------------------------------
// Boot overlay
// ---------------------------------------------------------------------------

private const val LOGO_SVG = """
<svg class="rr-boot-logo" viewBox="0 0 100 100" fill="none" aria-hidden="true">
  <defs>
    <linearGradient id="rrg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="#3ddc84"/><stop offset=".45" stop-color="#00bfa5"/>
      <stop offset=".75" stop-color="#2f6bff"/><stop offset="1" stop-color="#7c4dff"/>
    </linearGradient>
  </defs>
  <path d="M30 80 V20 H56 a17 17 0 0 1 0 34 H30 M54 54 L74 80" stroke="url(#rrg)" stroke-width="9"
        stroke-linecap="round" stroke-linejoin="round"/>
</svg>
"""

@Composable
private fun BootOverlay(onFinished: () -> Unit) {
    var out by remember { mutableStateOf(false) }
    val reveal = {
        if (!out) {
            out = true
            HtmlState.ready()
        }
    }

    LaunchedEffect(Unit) {
        delay(2000)
        reveal()
    }
    LaunchedEffect(out) {
        if (out) {
            delay(950)
            onFinished()
        }
    }
    DisposableEffect(Unit) {
        val onKey: (Event) -> Unit = { reveal() }
        window.addEventListener("keydown", onKey)
        onDispose { window.removeEventListener("keydown", onKey) }
    }

    Div({
        classes("rr-boot")
        if (out) classes("is-out")
        attr("role", "presentation")
        onClick { reveal() }
    }) {
        Div({ classes("rr-boot-ring") })
        Div({ classes("rr-boot-center") }) {
            Div({ classes("rr-boot-dots") }) {
                listOf("#3ddc84", "#00bfa5", "#7c4dff", "#2f6bff").forEachIndexed { i, c ->
                    Div({
                        classes("rr-boot-dot")
                        style { property("--i", i); property("--c", c) }
                    })
                }
            }
            Div({ ref { it.innerHTML = LOGO_SVG; onDispose { } } })
        }
        Div({ classes("rr-boot-caption") }) {
            Text("ritwik.os · booting")
            Div({ classes("rr-boot-bar") })
        }
        Div({ classes("rr-boot-skip") }) {
            Button({ onClick { it.stopPropagation(); reveal() } }) { Text("Skip intro") }
            A(resumeHref, {
                attr("download", Profile.resumeFileName)
                onClick { it.stopPropagation() }
            }) { Text("Résumé ↓") }
        }
    }
}

// ---------------------------------------------------------------------------
// Copy column
// ---------------------------------------------------------------------------

@Composable
private fun HeroCopy() {
    Div({ classes("rr-copy") }) {
        Div({ classes("rr-anim"); style { property("--d", "0s") } }) {
            Span({ classes("rr-chip") }) {
                Span({ classes("rr-pulse") })
                Text(Profile.availability)
            }
        }

        H1({ classes("rr-name"); attr("aria-label", Profile.name) }) {
            var i = 0
            listOf(Profile.shortName, Profile.lastName).forEach { line ->
                Span({ classes("rr-line"); attr("aria-hidden", "true") }) {
                    line.forEach { ch ->
                        val idx = i++
                        Span({
                            classes("rr-ch")
                            if (ch == ' ') classes("sp")
                            style { property("--i", idx) }
                        }) { Text(if (ch == ' ') " " else ch.toString()) }
                    }
                }
            }
        }

        Div({ classes("rr-roles", "rr-anim"); style { property("--d", "0.45s") } }) {
            Span({ classes("lead") }) { Text("I'm your next") }
            Span({ classes("rr-rotator"); attr("aria-label", Profile.roles.joinToString(", ")) }) {
                Profile.roles.forEachIndexed { i, role ->
                    Span({ style { property("--i", i); property("--n", Profile.roles.size) }; attr("aria-hidden", "true") }) { Text(role) }
                }
            }
        }

        P({ classes("rr-summary", "rr-anim"); style { property("--d", "0.55s") } }) {
            Text(Profile.summary)
        }

        Div({ classes("rr-cta", "rr-anim"); style { property("--d", "0.65s") } }) {
            A(resumeHref, {
                classes("rr-btn", "primary")
                attr("download", Profile.resumeFileName)
                magnetic()
            }) {
                I({ classes("fa-solid", "fa-file-arrow-down") })
                Text("Download résumé")
            }
            A(Profile.linkedIn, {
                classes("rr-btn")
                target(ATarget.Blank)
                attr("rel", "noopener")
                magnetic()
            }) {
                I({ classes("fa-brands", "fa-linkedin-in") })
                Text("LinkedIn")
            }
            Button({
                classes("rr-btn")
                onClick { UiState.contactOpen = true }
                magnetic()
            }) {
                I({ classes("fa-solid", "fa-paper-plane") })
                Text("Contact")
            }
        }

        Div({ classes("rr-meta", "rr-anim"); style { property("--d", "0.8s") } }) {
            Span { I({ classes("fa-solid", "fa-location-dot") }); Text(Profile.location) }
            Span { I({ classes("fa-brands", "fa-android") }); Text("${Profile.yearsOfExperience} years") }
            Span { I({ classes("fa-solid", "fa-wand-magic-sparkles") }); Text("Android · AI · React Native") }
        }

        MobileHome()
    }
}

/** Buttons that lean toward the cursor. */
private fun <T : Element> AttrsScope<T>.magnetic() {
    ref { el ->
        val e = el as HTMLElement
        val move: (Event) -> Unit = { ev ->
            val m = ev.asDynamic()
            val r = e.getBoundingClientRect()
            val dx = (m.clientX as Double) - (r.left + r.width / 2)
            val dy = (m.clientY as Double) - (r.top + r.height / 2)
            e.style.setProperty("--bx", "${dx * 0.18}px")
            e.style.setProperty("--by", "${dy * 0.28}px")
        }
        val leave: (Event) -> Unit = {
            e.style.setProperty("--bx", "0px")
            e.style.setProperty("--by", "0px")
        }
        e.addEventListener("pointermove", move)
        e.addEventListener("pointerleave", leave)
        onDispose {
            e.removeEventListener("pointermove", move)
            e.removeEventListener("pointerleave", leave)
        }
    }
}

// ---------------------------------------------------------------------------
// Mobile home screen (visible < 900px instead of the phone mockup)
// ---------------------------------------------------------------------------

@Composable
private fun MobileHome() {
    val clock = rememberClock()
    Div({ classes("rr-mhome") }) {
        Div({ classes("rr-wall") })
        Div({ classes("rr-glance") }) {
            Div({ classes("d") }) { Text(clock.shortDate) }
            Div({ classes("t") }) { Text("Your phone is now my portfolio.") }
        }
        Div({ classes("rr-grid") }) {
            Apps.grid.forEachIndexed { i, app -> AppIcon(app, i) }
        }
        Dock(index = Apps.grid.size)
    }
}

// ---------------------------------------------------------------------------
// Floating stat widgets
// ---------------------------------------------------------------------------

private class FloaterSpec(val fx: String, val fy: String, val wx: Double, val wy: Double, val depth: Double, val a: String, val b: String)

private val floaterSpecs = listOf(
    FloaterSpec("-300px", "-190px", -1.0, -0.8, 1.6, "#00bfa5", "#7c4dff"),
    FloaterSpec("290px", "-40px", 1.0, -0.2, 2.4, "#3ddc84", "#00bfa5"),
    FloaterSpec("-290px", "230px", -0.9, 0.9, 1.2, "#ff8a3d", "#ff3d7f"),
)

@Composable
private fun Floaters() {
    Div({ classes("rr-floaters"); attr("aria-hidden", "true") }) {
        floaterStats.zip(floaterSpecs).forEachIndexed { i, (stat, spec) ->
            Div({
                classes("rr-floater")
                style {
                    property("--fx", spec.fx)
                    property("--fy", spec.fy)
                    property("--wx", spec.wx)
                    property("--wy", spec.wy)
                    property("--depth", spec.depth)
                    property("--fi", i)
                    property("--fd", "${-i * 1.7}s")
                }
            }) {
                Div({ classes("rr-floater-inner") }) {
                    Div({ classes("ic"); style { property("--a", spec.a); property("--b", spec.b) } }) {
                        I({ classes(*stat.icon.split(" ").toTypedArray()) })
                    }
                    Div {
                        Div({ classes("v") }) { Text(stat.value) }
                        Div({ classes("l") }) { Text(stat.label) }
                    }
                }
            }
        }
    }
}

/**
 * When an app opens inside the hero phone, the scene enters focus mode (phone centered at its fit
 * scale, copy slid out — see rr.css). The transition class is only present while animating, so
 * normal scroll-driven movement never lags behind the scroll position.
 */
@Composable
private fun PhoneFocusEffect() {
    val focused = PhoneState.app != null && PhoneState.host == AppHost.PHONE && !PhoneState.closing
    LaunchedEffect(focused) {
        val scene = document.getElementById("top") ?: return@LaunchedEffect
        if (focused == scene.classList.contains("is-focus")) return@LaunchedEffect
        scene.classList.add("is-focus-anim")
        scene.classList.toggle("is-focus", focused)
        delay(800)
        scene.classList.remove("is-focus-anim")
    }
}

@Composable
private fun FocusBar() {
    Div({ classes("rr-focus-bar") }) {
        Button({ onClick { PhoneState.close() } }) {
            I({ classes("fa-solid", "fa-house") })
            Text("Home")
        }
        TagElement<org.w3c.dom.HTMLElement>("kbd", {}) { Text("Esc") }
    }
}

/** Speech bubble beside the phone telling visitors the apps are real. */
@Composable
private fun TapCallout() {
    Div({ classes("rr-callout"); attr("aria-hidden", "true") }) {
        Div({ classes("rr-callout-inner") }) {
            Span({ classes("hand") }) { Text("👆") }
            Div {
                Div({ classes("t") }) { Text("Psst — these apps work.") }
                Div({ classes("s") }) { Text("Tap one to look around.") }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Full-screen launcher revealed at the end of the dive
// ---------------------------------------------------------------------------

@Composable
private fun Desk() {
    val clock = rememberClock()
    Div({ classes("rr-desk") }) {
        Div({ classes("rr-wall") })
        Div({ classes("rr-desk-top") }) {
            Div({ classes("rr-desk-hello") }) {
                Div({ classes("d") }) { Text("${clock.date} · ${clock.time}") }
                H2 {
                    Text("You're inside ")
                    Em { Text("Ritwik's phone.") }
                }
            }
            Div({ classes("rr-desk-widgets") }) {
                heroStats.forEach { stat ->
                    Div({ classes("rr-widget-pill") }) {
                        Div {
                            Div({ classes("big") }) { Text(stat.value) }
                            Text(stat.label)
                        }
                    }
                }
            }
        }
        Div({ classes("rr-desk-grid") }) {
            Apps.desk.forEachIndexed { i, app -> AppIcon(app, i) }
        }
        Div({ classes("rr-desk-bottom") }) {
            Div({ classes("rr-search") }) {
                Span({ classes("g") }) { Text("R") }
                Text("Tap an app — or keep scrolling for the full story")
                Span({ style { property("margin-left", "auto") } }) { I({ classes("fa-solid", "fa-microphone") }) }
            }
        }
        Div({ classes("rr-desk-hint") }) {
            I({ classes("fa-solid", "fa-arrow-down") })
            Text("Scroll")
        }
    }
}

@Composable
private fun ScrollHint() {
    Div({ classes("rr-hint"); attr("aria-hidden", "true") }) {
        Div({ classes("rr-mouse") })
        Text("Scroll to enter")
    }
}
