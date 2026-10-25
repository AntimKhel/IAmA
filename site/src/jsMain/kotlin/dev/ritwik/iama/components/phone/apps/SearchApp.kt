package dev.ritwik.iama.components.phone.apps

import androidx.compose.runtime.*
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import dev.ritwik.iama.blog.blogEntries
import dev.ritwik.iama.components.devmode.DevMode
import dev.ritwik.iama.components.phone.AppAction
import dev.ritwik.iama.components.phone.AppBar
import dev.ritwik.iama.components.phone.Apps
import dev.ritwik.iama.components.phone.LauncherApp
import dev.ritwik.iama.components.phone.PhoneState
import dev.ritwik.iama.data.Profile
import dev.ritwik.iama.data.experiences
import dev.ritwik.iama.data.products
import dev.ritwik.iama.data.skillCategories
import dev.ritwik.iama.util.copyToClipboard
import dev.ritwik.iama.util.resumeHref
import kotlinx.browser.document
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.HTMLElement

/** One searchable thing: what it's called, how it's described, and what happens when you pick it. */
private class Hit(
    val title: String,
    val subtitle: String,
    val kind: String,
    val icon: String,
    val keywords: String = "",
    val href: String? = null,
    val download: Boolean = false,
    val run: (() -> Unit)? = null,
)

private fun openApp(app: LauncherApp, payload: Any? = null) = PhoneState.open(app, PhoneState.host, null, payload)

/** Everything on the site that search can reach, built from the same data the apps use. */
private fun index(toggleTheme: () -> Unit): List<Hit> = buildList {
    // Quick actions first: the things recruiters most often want.
    add(Hit("Download résumé", "PDF · ${Profile.resumeFileName}", "Action", "fa-solid fa-file-arrow-down", "cv resume pdf download hire", href = resumeHref, download = true))
    add(Hit("Copy email", Profile.email, "Action", "fa-regular fa-copy", "email mail contact address", run = { copyToClipboard(Profile.email, "Email") }))
    add(Hit("Email Ritwik", "Opens your mail app", "Action", "fa-solid fa-envelope", "email write message hire contact", href = "mailto:${Profile.email}"))
    add(Hit("Toggle dark / light mode", "Switch the site's theme", "Action", "fa-solid fa-circle-half-stroke", "theme dark light mode color", run = toggleTheme))
    add(Hit(if (DevMode.enabled) "Exit dev mode" else "Enter dev mode", "Re-skin the site as terminals", "Action", "fa-solid fa-terminal", "developer terminal easter egg", run = {
        PhoneState.close()
        DevMode.toggle()
    }))

    Apps.all.forEach { app ->
        val action = app.action
        when (action) {
            is AppAction.External -> add(Hit(app.label, "Opens ${action.url.removePrefix("https://").removePrefix("mailto:")}", "App", app.icon, href = action.url))
            else -> add(Hit(app.label, "Open the ${app.label} app", "App", app.icon, run = { openApp(app) }))
        }
    }
    products.forEach { p ->
        add(Hit(p.name, p.tagline, "Product", p.icon, "${p.myWork} ${p.tech.joinToString(" ")} ${p.role}", run = { openApp(Apps.products, p) }))
    }
    blogEntries.forEach { post ->
        add(Hit(post.title, "${post.readingMinutes} min read · ${post.date}", "Post", "fa-solid fa-newspaper", "${post.description} ${post.tags.joinToString(" ")}", run = { openApp(Apps.blog, post) }))
    }
    experiences.forEach { e ->
        add(Hit(e.company, "${e.role} · ${e.period}", "Work", "fa-solid fa-briefcase", "${e.highlights.joinToString(" ")} ${e.techStack.joinToString(" ")}", run = { openApp(Apps.work) }))
    }
    skillCategories.forEach { c ->
        c.skills.forEach { s -> add(Hit(s, c.name, "Skill", "fa-solid fa-code", c.name, run = { openApp(Apps.skills) })) }
    }
}

private val combiningMarks = Regex("[\\u0300-\\u036f]")

/** Lower-case and accent-free, so "resume" finds "Résumé". */
private fun fold(s: String): String = (s.lowercase().asDynamic().normalize("NFD") as String).replace(combiningMarks, "")

/** Simple, predictable ranking: prefix of the title beats a word prefix, beats a substring, beats keywords. */
private fun rank(hits: List<Hit>, query: String): List<Hit> {
    val terms = fold(query).split(Regex("\\s+")).filter { it.isNotBlank() }
    if (terms.isEmpty()) return emptyList()
    return hits.mapNotNull { h ->
        val title = fold(h.title)
        val words = title.split(Regex("[^\\p{L}\\p{N}]+"))
        val rest = fold("${h.subtitle} ${h.keywords} ${h.kind}")
        var score = 0.0
        for (t in terms) {
            score += when {
                title.startsWith(t) -> 6.0
                words.any { it.startsWith(t) } -> 4.0
                title.contains(t) -> 2.5
                rest.contains(t) -> 1.0
                else -> return@mapNotNull null // every term must match somewhere
            }
        }
        h to score
    }.sortedByDescending { it.second }.map { it.first }.take(14)
}

@Composable
fun SearchApp() {
    var colorMode by ColorMode.currentState
    val all = remember { index(toggleTheme = { colorMode = colorMode.opposite }) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(0) }
    val results = remember(query) { rank(all, query) }
    val suggestions = remember {
        all.filter { it.kind == "Action" }.take(3) + all.filter { it.kind == "Product" }.take(2) + all.filter { it.kind == "Post" }.take(2)
    }
    val shown = if (query.isBlank()) suggestions else results
    LaunchedEffect(query) { selected = 0 }
    // Keep the highlighted result visible while arrowing through the list.
    LaunchedEffect(selected) {
        (document.querySelector(".rr-search-results .rr-a-row.on") as? HTMLElement)?.asDynamic()?.scrollIntoView(js("({block: 'nearest'})"))
    }

    AppBar("Search", "Apps, products, posts, skills")
    Div({ classes("rr-a-scroll") }) {
        Div({ classes("rr-search-field") }) {
            I({ classes("fa-solid", "fa-magnifying-glass") })
            Input(InputType.Search) {
                value(query)
                placeholder("Try “Compose”, “OTA”, “résumé”…")
                attr("aria-label", "Search Ritwik's work")
                attr("autocomplete", "off")
                attr("data-autofocus", "")
                onInput { query = it.value }
                onKeyDown { e ->
                    when (e.key) {
                        "ArrowDown" -> { e.preventDefault(); selected = (selected + 1).coerceAtMost(shown.lastIndex) }
                        "ArrowUp" -> { e.preventDefault(); selected = (selected - 1).coerceAtLeast(0) }
                        "Enter" -> {
                            e.preventDefault()
                            val el = document.querySelector(".rr-search-results .rr-a-row.on") as? HTMLElement
                            el?.click()
                        }
                    }
                }
            }
        }
        Heading(if (query.isBlank()) "Suggestions" else "${results.size} result${if (results.size == 1) "" else "s"}")
        if (query.isNotBlank() && results.isEmpty()) {
            P({ classes("rr-a-p") }) { Text("Nothing matches “$query”. Try a technology, a product or a company.") }
        }
        Div({ classes("rr-search-results"); attr("role", "listbox") }) {
            shown.forEachIndexed { i, hit -> HitRow(hit, i == selected) }
        }
    }
}

@Composable
private fun HitRow(hit: Hit, on: Boolean) {
    val body: @Composable () -> Unit = {
        Div({ classes("rr-a-picon", "small") }) { I({ classes(*hit.icon.split(" ").toTypedArray()) }) }
        Div({ classes("rr-a-row-text") }) {
            Div({ classes("rr-a-title") }) { Text(hit.title) }
            Div({ classes("rr-a-sub") }) { Text(hit.subtitle) }
        }
        Span({ classes("rr-search-kind") }) { Text(hit.kind) }
    }
    val attrsCommon: org.jetbrains.compose.web.attributes.AttrsScope<*>.() -> Unit = {
        classes("rr-a-row")
        if (on) classes("on")
        attr("role", "option")
        attr("aria-selected", on.toString())
    }
    if (hit.href != null) {
        A(hit.href, {
            attrsCommon()
            if (hit.download) attr("download", Profile.resumeFileName)
            if (hit.href.startsWith("http")) { target(ATarget.Blank); attr("rel", "noopener") }
        }) { body() }
    } else {
        Button({
            attrsCommon()
            onClick { hit.run?.invoke() }
        }) { body() }
    }
}
