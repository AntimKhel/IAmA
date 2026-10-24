package dev.ritwik.iama.pages.blog

import androidx.compose.runtime.*
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.data.add
import com.varabyte.kobweb.core.init.InitRoute
import com.varabyte.kobweb.core.init.InitRouteContext
import com.varabyte.kobweb.core.layout.Layout
import com.varabyte.kobweb.core.rememberPageContext
import dev.ritwik.iama.blog.BlogEntry
import dev.ritwik.iama.blog.blogEntries
import dev.ritwik.iama.components.layouts.PageLayoutData
import dev.ritwik.iama.components.layouts.RouteLink
import org.jetbrains.compose.web.dom.*

@InitRoute
fun initBlogPage(ctx: InitRouteContext) {
    ctx.data.add(PageLayoutData("Blog"))
}

private fun hue(p: BlogEntry) = (p.title.sumOf { it.code } * 37) % 360

@Page
@Layout(".components.layouts.PageLayout")
@Composable
fun BlogListPage() {
    val router = rememberPageContext().router
    var tag by remember { mutableStateOf<String?>(null) }
    val allTags = remember { blogEntries.flatMap { it.tags }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key } }
    val selected = tag
    val posts = blogEntries.filter { selected == null || selected in it.tags }

    Section({ classes("blog-page") }) {
        Div({ classes("blog-hero-glow"); attr("aria-hidden", "true") })
        Div({ classes("post-wrap", "wide") }) {
            Div({ classes("blog-head") }) {
                Span({ classes("blog-eyebrow") }) { Span({ classes("dot") }); Text("Blog · ${blogEntries.size} posts") }
                H1({ classes("blog-title") }) { Text("Notes from "); Span({ classes("grad") }) { Text("the field") } }
                P({ classes("blog-sub") }) {
                    Text("Long-form writing on Android, React Native, build performance and AI-native engineering — the problems I hit shipping apps used by hundreds of millions.")
                }
                Div({ classes("blog-tags") }) {
                    Button({ if (tag == null) classes("on"); onClick { tag = null } }) { Text("All") }
                    allTags.forEach { t ->
                        Button({ if (tag == t) classes("on"); onClick { tag = if (tag == t) null else t } }) { Text(t) }
                    }
                }
            }

            key(tag) {
                Div({ classes("blog-list") }) {
                    posts.forEachIndexed { i, p ->
                        RouteLink(p.route, router, if (i == 0) "blog-card featured" else "blog-card") {
                            Div({ classes("blog-cover"); style { property("--h", hue(p)) } }) {
                                Span({ classes("blog-glyph") }) { Text(p.title.first().toString()) }
                                if (i == 0 && tag == null) Span({ classes("blog-new") }) { Text("Latest") }
                            }
                            Div({ classes("blog-body") }) {
                                Div({ classes("blog-meta") }) { Text("${p.date} · ${p.readingMinutes} min read") }
                                H2({ classes("blog-name") }) { Text(p.title) }
                                P({ classes("blog-desc") }) { Text(p.description) }
                                Div({ classes("blog-chips") }) { p.tags.forEach { Span { Text(it) } } }
                            }
                        }
                    }
                }
            }
        }
    }
}
