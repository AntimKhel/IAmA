package dev.ritwik.iama.components.phone

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.core.rememberPageContext
import dev.ritwik.iama.data.Profile
import dev.ritwik.iama.util.UiState
import dev.ritwik.iama.util.goToSection
import dev.ritwik.iama.util.resumeHref
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.I
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

sealed interface AppAction {
    data class Section(val id: String) : AppAction
    data class Route(val path: String) : AppAction
    data class External(val url: String) : AppAction
    data object Resume : AppAction
    data object Contact : AppAction
}

class LauncherApp(
    val label: String,
    val icon: String,     // font-awesome classes, e.g. "fa-solid fa-briefcase"
    val from: String,     // gradient start
    val to: String,       // gradient end
    val action: AppAction,
)

object Apps {
    val work = LauncherApp("Work", "fa-solid fa-briefcase", "#2f6bff", "#7c4dff", AppAction.Section("experience"))
    val products = LauncherApp("Products", "fa-solid fa-layer-group", "#ff8a3d", "#ff3d7f", AppAction.Section("showcase"))
    val blog = LauncherApp("Blog", "fa-solid fa-newspaper", "#00bfa5", "#2f6bff", AppAction.Route("/blog"))
    val impact = LauncherApp("Impact", "fa-solid fa-gauge-high", "#3ddc84", "#00bfa5", AppAction.Section("showcase"))
    val contact = LauncherApp("Contact", "fa-solid fa-paper-plane", "#ff3d7f", "#7c4dff", AppAction.Contact)
    val resume = LauncherApp("Résumé", "fa-solid fa-file-arrow-down", "#6cf0a6", "#00bfa5", AppAction.Resume)
    val linkedIn = LauncherApp("LinkedIn", "fa-brands fa-linkedin-in", "#0a66c2", "#2f6bff", AppAction.External(Profile.linkedIn))
    val github = LauncherApp("GitHub", "fa-brands fa-github", "#3b434f", "#151a21", AppAction.External(Profile.github))
    val skills = LauncherApp("Skills", "fa-solid fa-code", "#7c4dff", "#b06bff", AppAction.Section("skills"))
    val awards = LauncherApp("Awards", "fa-solid fa-award", "#ffb13d", "#ff8a3d", AppAction.Section("certifications"))

    val phone = LauncherApp("Phone", "fa-solid fa-phone", "#3ddc84", "#00bfa5", AppAction.Contact)
    val mail = LauncherApp("Mail", "fa-solid fa-envelope", "#2f6bff", "#00bfa5", AppAction.External("mailto:${Profile.email}"))
    val play = LauncherApp("Play", "fa-brands fa-google-play", "#00bfa5", "#7c4dff", AppAction.Section("showcase"))
    val browser = LauncherApp("Browser", "fa-solid fa-compass", "#ff8a3d", "#2f6bff", AppAction.Route("/blog"))

    val grid = listOf(work, products, blog, impact, contact, resume, linkedIn, github)
    val desk = listOf(work, products, blog, impact, skills, awards, contact, resume, linkedIn, github)
    val dock = listOf(phone, mail, play, browser)
}

/** One launcher icon. Links (résumé, external) render as real anchors so they work without JS routing. */
@Composable
fun AppIcon(app: LauncherApp, index: Int = 0) {
    val router = rememberPageContext().router
    val styleVars: org.jetbrains.compose.web.css.StyleScope.() -> Unit = {
        property("--a", app.from)
        property("--b", app.to)
        property("--i", index)
    }
    val content: @Composable () -> Unit = {
        Div({ classes("ico") }) { I({ classes(*app.icon.split(" ").toTypedArray()) }) }
        Span { Text(app.label) }
    }

    when (val action = app.action) {
        is AppAction.External -> A(action.url, {
            classes("rr-app")
            style(styleVars)
            if (!action.url.startsWith("mailto:")) {
                target(ATarget.Blank)
                attr("rel", "noopener")
            }
            attr("aria-label", app.label)
        }) { content() }

        AppAction.Resume -> A(resumeHref, {
            classes("rr-app")
            style(styleVars)
            attr("download", Profile.resumeFileName)
            attr("aria-label", "Download résumé")
        }) { content() }

        else -> Button({
            classes("rr-app")
            style(styleVars)
            attr("aria-label", app.label)
            onClick {
                when (action) {
                    is AppAction.Section -> goToSection(action.id, router)
                    is AppAction.Route -> router.tryRoutingTo(action.path)
                    AppAction.Contact -> UiState.contactOpen = true
                }
            }
        }) { content() }
    }
}

@Composable
fun Dock(withSearch: Boolean = true, index: Int = 0) {
    Div({
        classes("rr-dock")
        style { property("--i", index) }
    }) {
        Div({ classes("row") }) {
            Apps.dock.forEachIndexed { i, app -> AppIcon(app, index + i) }
        }
        if (withSearch) {
            Div({ classes("rr-search") }) {
                Span({ classes("g") }) { Text("R") }
                Text("Search Ritwik's work…")
                Span({ style { property("margin-left", "auto") } }) {
                    I({ classes("fa-solid", "fa-microphone") })
                }
            }
        }
    }
}
