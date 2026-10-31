package dev.ritwik.iama.components.phone.apps

import org.w3c.dom.HTMLElement
import kotlinx.browser.document
import dev.ritwik.iama.components.blog.slugOf
import dev.ritwik.iama.components.blog.ReadTracker
import dev.ritwik.iama.components.blog.PostCounts
import dev.ritwik.iama.components.blog.ClapBar
import androidx.compose.runtime.*
import com.varabyte.kobweb.core.rememberPageContext
import dev.ritwik.iama.blog.BlogEntry
import dev.ritwik.iama.blog.blogEntries
import dev.ritwik.iama.components.phone.AppBar
import dev.ritwik.iama.components.phone.PhoneState
import org.jetbrains.compose.web.dom.*

/** Reader app: post list, and the real markdown post rendered in-app. */
@Composable
fun BlogApp() {
    // Search can deep-link straight into a post.
    var reading by remember { mutableStateOf(PhoneState.payload as? BlogEntry) }
    LaunchedEffect(Unit) { dev.ritwik.iama.components.blog.PostStats.loadAll() }
    val router = rememberPageContext().router
    val post = reading

    if (post == null) {
        AppBar("Blog", "${blogEntries.size} posts")
        Div({ classes("rr-a-scroll") }) {
            Div({ classes("rr-a-hero") }) {
                Div({ classes("k") }) { Text("Notes from the field") }
                Div({ classes("v") }) { Text("Android, AI-native engineering and building at scale.") }
            }
            blogEntries.forEachIndexed { i, entry ->
                Button({
                    classes("rr-a-post", "rr-a-stagger")
                    style { property("--i", i) }
                    onClick { reading = entry }
                }) {
                    Div({ classes("rr-a-meta") }) { Text("${entry.date} · ${entry.readingMinutes} min read"); PostCounts(slugOf(entry.route)) }
                    Div({ classes("rr-a-title") }) { Text(entry.title) }
                    Div({ classes("rr-a-sub") }) { Text(entry.description) }
                    Chips(entry.tags)
                }
            }
        }
    } else {
        AppBar(post.title, "${post.readingMinutes} min read", onBack = { reading = null }) {
            Button({
                classes("rr-appbar-back")
                attr("aria-label", "Open full article")
                attr("title", "Open full article")
                onClick {
                    PhoneState.close()
                    router.tryRoutingTo(post.route)
                }
            }) { I({ classes("fa-solid", "fa-up-right-from-square") }) }
        }
        Div({ classes("rr-a-scroll") }) {
            Div({ classes("rr-a-article") }) {
                Div({ classes("rr-a-meta") }) { Text(post.date) }
                H1({ classes("rr-a-article-title") }) { Text(post.title) }
                Chips(post.tags)
                Div({ classes("rr-a-article-body") }) { post.content() }
                val slug = slugOf(post.route)
                ReadTracker(slug, scroller = { document.querySelector(".rr-appwin .rr-a-scroll") as? HTMLElement }) {
                    document.querySelector(".rr-appwin .rr-a-article-body") as? HTMLElement
                }
                ClapBar(slug)
                Button({
                    classes("rr-a-install")
                    onClick {
                        PhoneState.close()
                        router.tryRoutingTo(post.route)
                    }
                }) { Text("Open the full article"); I({ classes("fa-solid", "fa-arrow-right") }) }
            }
        }
    }
}
