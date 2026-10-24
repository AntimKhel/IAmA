package dev.ritwik.iama.components.layouts

import androidx.compose.runtime.*
import com.varabyte.kobweb.core.data.add
import com.varabyte.kobweb.core.init.InitRoute
import com.varabyte.kobweb.core.init.InitRouteContext
import com.varabyte.kobweb.core.layout.Layout
import com.varabyte.kobweb.core.rememberPageContext
import com.varabyte.kobweb.navigation.BasePath
import com.varabyte.kobweb.navigation.Router
import com.varabyte.kobwebx.markdown.markdown
import dev.ritwik.iama.blog.BlogEntry
import dev.ritwik.iama.blog.blogEntries
import dev.ritwik.iama.data.Profile
import dev.ritwik.iama.util.UiState
import dev.ritwik.iama.util.clamp01
import dev.ritwik.iama.util.css
import dev.ritwik.iama.util.resumeHref
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.HTMLElement
import org.w3c.dom.asList
import org.w3c.dom.events.Event

@InitRoute
fun initMarkdownLayout(ctx: InitRouteContext) {
    val markdown = ctx.markdown ?: error("MarkdownLayout used on a non-markdown page")
    val title = markdown.frontMatter["title"]?.singleOrNull()
    require(title != null) { "Markdown file must set \"title\" in frontmatter" }
    ctx.data.add(PageLayoutData(title))
}

private class Heading(val id: String, val text: String)

/** Layout for every blog post: hero, reading progress, sticky table of contents, author card, more posts. */
@Composable
@Layout(".components.layouts.PageLayout")
fun MarkdownLayout(content: @Composable () -> Unit) {
    val ctx = rememberPageContext()
    val router = ctx.router
    val fm = ctx.markdown?.frontMatter
    val title = fm?.get("title")?.firstOrNull() ?: "Untitled"
    val date = fm?.get("date")?.firstOrNull()
    val description = fm?.get("description")?.firstOrNull()
    val tags = (fm?.get("tags")?.firstOrNull() ?: "").split(",").map { it.trim() }.filter { it.isNotEmpty() }
    val route = ctx.route.path.trimEnd('/')
    // The page path may carry the site's base path (/IAmA/…), so match on the route suffix.
    val entry = blogEntries.firstOrNull { route.endsWith(it.route.trimEnd('/')) }
    val more = blogEntries.filter { it !== entry }.take(2)

    var headings by remember(route) { mutableStateOf(emptyList<Heading>()) }
    var active by remember(route) { mutableStateOf<String?>(null) }

    LaunchedEffect(route) {
        if (description != null) setMetaDescription(description)
        delay(60) // let the markdown body render
        headings = document.querySelectorAll(".post-body h2[id]").asList().map {
            val el = it as HTMLElement
            Heading(el.id, el.textContent ?: "")
        }
    }

    // Highlight the heading currently being read.
    DisposableEffect(headings) {
        if (headings.isEmpty()) return@DisposableEffect onDispose { }
        val passive: dynamic = js("({passive: true})")
        val onScroll: (Event) -> Unit = {
            val line = window.innerHeight * 0.3
            active = headings.lastOrNull { h ->
                (document.getElementById(h.id)?.getBoundingClientRect()?.top ?: 1e9) < line
            }?.id ?: headings.first().id
        }
        window.addEventListener("scroll", onScroll, passive)
        onScroll(Event("scroll"))
        onDispose { window.removeEventListener("scroll", onScroll, passive) }
    }

    ReadingProgress()

    Article({ classes("post") }) {
        Header({ classes("post-hero") }) {
            Div({ classes("post-hero-glow"); attr("aria-hidden", "true") })
            Div({ classes("post-wrap") }) {
                RouteLink("/blog", router, "post-back") { I({ classes("fa-solid", "fa-arrow-left") }); Text("All posts") }
                if (tags.isNotEmpty()) Div({ classes("post-tags") }) { tags.forEach { Span { Text(it) } } }
                H1({ classes("post-title") }) { Text(title) }
                if (description != null) P({ classes("post-desc") }) { Text(description) }
                Div({ classes("post-meta") }) {
                    Span({ classes("post-avatar") }) { Text("R") }
                    Div {
                        Div({ classes("post-author") }) { Text(Profile.name) }
                        Div({ classes("post-when") }) {
                            Text(listOfNotNull(date, entry?.let { "${it.readingMinutes} min read" }).joinToString(" · "))
                        }
                    }
                }
            }
        }

        Div({ classes("post-grid") }) {
            Div({ classes("post-body", "prose") }) { content() }
            Aside({ classes("post-toc") }) {
                if (headings.size >= 2) {
                    Div({ classes("toc-title") }) { Text("On this page") }
                    Nav {
                        headings.forEach { h ->
                            A("#${h.id}", {
                                if (active == h.id) classes("on")
                                onClick {
                                    it.preventDefault()
                                    document.getElementById(h.id)?.asDynamic()?.scrollIntoView(js("({behavior: 'smooth', block: 'start'})"))
                                }
                            }) { Text(h.text) }
                        }
                    }
                }
            }
        }

        Footer({ classes("post-end", "post-wrap") }) {
            Div({ classes("author-card") }) {
                Span({ classes("post-avatar", "big") }) { Text("R") }
                Div({ classes("author-text") }) {
                    Div({ classes("author-name") }) { Text("Written by ${Profile.name}") }
                    P { Text("${Profile.headline}. I lead engineering across MyJio and write about Android, AI-native engineering and shipping at scale.") }
                    Div({ classes("author-actions") }) {
                        A(resumeHref, { classes("rr-btn", "primary"); attr("download", Profile.resumeFileName) }) {
                            I({ classes("fa-solid", "fa-file-arrow-down") }); Text("Résumé")
                        }
                        A(Profile.linkedIn, { classes("rr-btn"); target(ATarget.Blank); attr("rel", "noopener") }) {
                            I({ classes("fa-brands", "fa-linkedin-in") }); Text("LinkedIn")
                        }
                        Button({ classes("rr-btn"); onClick { UiState.contactOpen = true } }) {
                            I({ classes("fa-solid", "fa-paper-plane") }); Text("Contact")
                        }
                    }
                }
            }
            if (more.isNotEmpty()) {
                Div({ classes("more-title") }) { Text("Keep reading") }
                Div({ classes("more-grid") }) { more.forEach { MoreCard(it, router) } }
            }
        }
    }
}

@Composable
private fun MoreCard(p: BlogEntry, router: Router) {
    RouteLink(p.route, router, "more-card") {
        Div({ classes("more-meta") }) { Text("${p.date} · ${p.readingMinutes} min") }
        Div({ classes("more-name") }) { Text(p.title) }
        Div({ classes("more-desc") }) { Text(p.description) }
    }
}

/** An anchor with a real (base-path-prefixed) href that navigates client-side. */
@Composable
internal fun RouteLink(path: String, router: Router, cls: String, content: @Composable () -> Unit) {
    A(BasePath.prependTo(path), {
        classes(*cls.split(" ").toTypedArray())
        onClick { e ->
            if (!e.metaKey && !e.ctrlKey) {
                e.preventDefault()
                router.tryRoutingTo(path)
            }
        }
    }) { content() }
}

@Composable
private fun ReadingProgress() {
    Div({
        classes("read-progress")
        ref { el ->
            val passive: dynamic = js("({passive: true})")
            val update: (Event) -> Unit = {
                val doc = document.documentElement
                val max = ((doc?.scrollHeight ?: 1) - window.innerHeight).toDouble()
                el.style.setProperty("--rp", clamp01(if (max > 0) window.scrollY / max else 0.0).css())
            }
            window.addEventListener("scroll", update, passive)
            update(Event("scroll"))
            onDispose { window.removeEventListener("scroll", update, passive) }
        }
    })
}

private fun setMetaDescription(text: String) {
    val head = document.head ?: return
    val meta = head.querySelector("meta[name=description]") ?: document.createElement("meta").also {
        it.setAttribute("name", "description")
        head.appendChild(it)
    }
    meta.setAttribute("content", text)
}
