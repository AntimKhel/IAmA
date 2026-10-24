package dev.ritwik.iama.components.sections

import androidx.compose.runtime.*
import com.varabyte.kobweb.core.rememberPageContext
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import dev.ritwik.iama.data.Profile
import dev.ritwik.iama.util.UiState
import dev.ritwik.iama.util.css
import dev.ritwik.iama.util.goToSection
import dev.ritwik.iama.util.resumeHref
import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event

private val navSections = listOf("products" to "Products", "experience" to "Work", "roadmap" to "Upcoming")

/** Floating glass pill: logo with a scroll-progress ring, section links, blog, résumé and the "Hire me" fast lane. */
@Composable
fun NavHeader() {
    val router = rememberPageContext().router
    var colorMode by ColorMode.currentState

    Nav({
        classes("rr-nav")
        attr("aria-label", "Primary")
        ref { el ->
            val nav = el
            val logo = nav.querySelector(".rr-logo") as? HTMLElement
            var raf = 0
            val update: (Event) -> Unit = {
                if (raf == 0) {
                    raf = window.requestAnimationFrame {
                        raf = 0
                        val doc = document.documentElement
                        val max = ((doc?.scrollHeight ?: 1) - window.innerHeight).toDouble()
                        val p = if (max > 0) (window.scrollY / max).coerceIn(0.0, 1.0) else 0.0
                        logo?.style?.setProperty("--sp", p.css())
                        nav.classList.toggle("is-scrolled", window.scrollY > 40)
                    }
                }
            }
            val passive: dynamic = js("({passive: true})")
            window.addEventListener("scroll", update, passive)
            update(Event("scroll"))
            onDispose {
                window.removeEventListener("scroll", update, passive)
                if (raf != 0) window.cancelAnimationFrame(raf)
            }
        }
    }) {
        Button({
            classes("rr-logo")
            attr("aria-label", "Back to top")
            onClick {
                if (document.getElementById("top") != null) window.scrollTo(0.0, 0.0) else router.tryRoutingTo("/")
            }
        }) { Span { Text("R") } }

        Div({ classes("rr-nav-links") }) {
            navSections.forEach { (id, label) ->
                Button({ classes("rr-nav-link"); onClick { goToSection(id, router) } }) { Text(label) }
            }
            Button({ classes("rr-nav-link"); onClick { router.tryRoutingTo("/blog") } }) { Text("Blog") }
        }

        A(resumeHref, {
            classes("rr-nav-icon")
            attr("download", Profile.resumeFileName)
            attr("aria-label", "Download résumé")
            attr("title", "Download résumé")
        }) { I({ classes("fa-solid", "fa-file-arrow-down") }) }

        Button({
            classes("rr-nav-icon")
            attr("aria-label", "Toggle color mode")
            attr("title", "Toggle color mode")
            onClick { colorMode = colorMode.opposite }
        }) { I({ classes("fa-solid", if (colorMode.isDark) "fa-sun" else "fa-moon") }) }

        Button({ classes("rr-hire"); onClick { UiState.contactOpen = true } }) {
            I({ classes("fa-solid", "fa-bolt") })
            Text("Hire me")
        }
    }
}
