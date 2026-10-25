package dev.ritwik.iama.components.phone

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.core.rememberPageContext
import dev.ritwik.iama.data.Backend
import dev.ritwik.iama.data.Profile
import dev.ritwik.iama.util.HtmlState
import dev.ritwik.iama.util.UiState
import dev.ritwik.iama.util.haptic
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
import org.w3c.dom.Element

sealed interface AppAction {
    data class Section(val id: String) : AppAction
    data class Route(val path: String) : AppAction
    data class External(val url: String) : AppAction
    data object Resume : AppAction
    data object Contact : AppAction
    data class Open(val screen: AppScreen) : AppAction
}

class LauncherApp(
    val label: String,
    val icon: String,     // font-awesome classes, e.g. "fa-solid fa-briefcase"
    val from: String,     // gradient start
    val to: String,       // gradient end
    val action: AppAction,
)

object Apps {
    val work = LauncherApp("Work", "fa-solid fa-briefcase", "#2f6bff", "#7c4dff", AppAction.Open(AppScreen.WORK))
    val products = LauncherApp("Products", "fa-solid fa-layer-group", "#ff8a3d", "#ff3d7f", AppAction.Open(AppScreen.STORE))
    val blog = LauncherApp("Blog", "fa-solid fa-newspaper", "#00bfa5", "#2f6bff", AppAction.Open(AppScreen.BLOG))
    val about = LauncherApp("About", "fa-solid fa-user-astronaut", "#3ddc84", "#00bfa5", AppAction.Open(AppScreen.ABOUT))
    val contact = LauncherApp("Contact", "fa-solid fa-address-card", "#ff3d7f", "#7c4dff", AppAction.Open(AppScreen.CONTACT))
    val resume = LauncherApp("Résumé", "fa-solid fa-file-lines", "#6cf0a6", "#00bfa5", AppAction.Open(AppScreen.RESUME))
    val search = LauncherApp("Search", "fa-solid fa-magnifying-glass", "#2f6bff", "#7c4dff", AppAction.Open(AppScreen.SEARCH))
    val linkedIn = LauncherApp("LinkedIn", "fa-brands fa-linkedin-in", "#0a66c2", "#2f6bff", AppAction.External(Profile.linkedIn))
    val github = LauncherApp("GitHub", "fa-brands fa-github", "#3b434f", "#151a21", AppAction.External(Profile.github))
    val skills = LauncherApp("Skills", "fa-solid fa-code", "#7c4dff", "#b06bff", AppAction.Open(AppScreen.SKILLS))
    val awards = LauncherApp("Awards", "fa-solid fa-trophy", "#ffb13d", "#ff8a3d", AppAction.Open(AppScreen.AWARDS))
    val guestbook = LauncherApp("Guestbook", "fa-solid fa-feather-pointed", "#ff6fae", "#ffb13d", AppAction.Open(AppScreen.GUESTBOOK))
    val ask = LauncherApp("Ask", "fa-solid fa-wand-magic-sparkles", "#7c4dff", "#00bfa5", AppAction.Open(AppScreen.ASK))

    val phone = LauncherApp("Phone", "fa-solid fa-phone", "#3ddc84", "#00bfa5", AppAction.Open(AppScreen.CONTACT))
    val mail = LauncherApp("Mail", "fa-solid fa-envelope", "#2f6bff", "#00bfa5", AppAction.External("mailto:${Profile.email}"))
    val play = LauncherApp("Play", "fa-brands fa-google-play", "#00bfa5", "#7c4dff", AppAction.Open(AppScreen.STORE))
    val browser = LauncherApp("Browser", "fa-solid fa-compass", "#ff8a3d", "#2f6bff", AppAction.Open(AppScreen.BLOG))

    /** The Guestbook takes GitHub's slot once the backend is configured (GitHub stays in footer + contact). */
    private val lastSlot get() = if (Backend.enabled) guestbook else github

    val grid get() = listOf(work, products, blog, about, contact, resume, linkedIn, lastSlot)

    /** Every app, for launcher search. */
    val all get() = listOfNotNull(
        work, products, blog, about, skills, awards, contact, resume,
        ask.takeIf { Backend.enabled }, guestbook.takeIf { Backend.enabled }, linkedIn, github,
    )

    // Ask needs the backend too; it takes the slots of apps that are reachable elsewhere (Browser opened
    // the Blog; Awards is in Search and About).
    val desk get() = listOf(work, products, blog, about, skills, if (Backend.enabled) ask else awards, contact, resume, linkedIn, lastSlot)
    val dock get() = listOf(phone, mail, play, if (Backend.enabled) ask else browser)
}

/** One launcher icon. Links (résumé, external) render as real anchors so they work without JS routing. */
@Composable
fun AppIcon(app: LauncherApp, index: Int = 0, hint: Boolean = false) {
    val router = rememberPageContext().router
    val host = LocalAppHost.current
    val styleVars: org.jetbrains.compose.web.css.StyleScope.() -> Unit = {
        property("--a", app.from)
        property("--b", app.to)
        property("--i", index)
    }
    val content: @Composable () -> Unit = {
        Div({ classes("ico") }) {
            I({ classes(*app.icon.split(" ").toTypedArray()) })
            if (hint) Span({ classes("rr-finger"); attr("aria-hidden", "true") })
        }
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
            onClick { HtmlState.markTapped(); haptic() }
        }) { content() }

        AppAction.Resume -> A(resumeHref, {
            classes("rr-app")
            style(styleVars)
            attr("download", Profile.resumeFileName)
            attr("aria-label", "Download résumé")
            onClick { HtmlState.markTapped() }
        }) { content() }

        else -> Button({
            classes("rr-app")
            style(styleVars)
            attr("aria-label", app.label)
            onClick { e ->
                HtmlState.markTapped()
                when (action) {
                    is AppAction.Open -> PhoneState.open(app, host, e.target as? Element)
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
        if (withSearch) SearchBar("Search Ritwik's work…")
    }
}

/** The launcher's search pill: opens the Search app, growing out of the bar itself. */
@Composable
fun SearchBar(label: String) {
    val host = LocalAppHost.current
    Button({
        classes("rr-search")
        attr("data-origin", "")
        attr("aria-keyshortcuts", "Meta+K Control+K")
        onClick { e -> PhoneState.open(Apps.search, host, e.target as? Element) }
    }) {
        Span({ classes("g"); attr("aria-hidden", "true") }) { Text("R") }
        Span({ classes("rr-search-label") }) { Text(label) }
        Span({ classes("rr-search-kbd"); attr("aria-hidden", "true") }) { Text("⌘K") }
    }
}
