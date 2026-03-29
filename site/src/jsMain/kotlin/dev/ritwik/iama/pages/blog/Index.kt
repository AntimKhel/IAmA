package dev.ritwik.iama.pages.blog

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.data.add
import com.varabyte.kobweb.core.init.InitRoute
import com.varabyte.kobweb.core.init.InitRouteContext
import com.varabyte.kobweb.core.layout.Layout
import com.varabyte.kobweb.silk.components.navigation.Link
import com.varabyte.kobweb.silk.components.navigation.UncoloredLinkVariant
import com.varabyte.kobweb.silk.components.text.SpanText
import com.varabyte.kobweb.silk.style.toModifier
import com.varabyte.kobweb.silk.style.vars.color.ColorVar
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import org.jetbrains.compose.web.css.*
import dev.ritwik.iama.*
import dev.ritwik.iama.components.layouts.PageLayoutData
import dev.ritwik.iama.components.widgets.ScrollReveal
import dev.ritwik.iama.components.widgets.SectionHeader

data class BlogPost(
    val title: String,
    val description: String,
    val date: String,
    val path: String,
    val tags: List<String>,
)

val blogPosts = listOf(
    BlogPost(
        title = "Hello World - Why I Built This Site with Kotlin",
        description = "An Android developer's journey into web development using Compose for Web and Kobweb.",
        date = "March 29, 2026",
        path = "/blog/hello-world",
        tags = listOf("Kotlin", "Kobweb", "Web"),
    ),
    BlogPost(
        title = "Building a Design System with Jetpack Compose",
        description = "How we built the Jio Design System from scratch using Jetpack Compose, and lessons learned scaling it across multiple apps.",
        date = "March 25, 2026",
        path = "/blog/compose-design-system",
        tags = listOf("Jetpack Compose", "Design System", "Android"),
    ),
    BlogPost(
        title = "OTA Updates for React Native Without CodePush",
        description = "We built our own over-the-air update system for React Native at Jio. Here's the architecture and why we chose this over CodePush.",
        date = "March 20, 2026",
        path = "/blog/ota-updates-react-native",
        tags = listOf("React Native", "OTA", "Architecture"),
    ),
)

@InitRoute
fun initBlogPage(ctx: InitRouteContext) {
    ctx.data.add(PageLayoutData("Blog"))
}

@Page
@Layout(".components.layouts.PageLayout")
@Composable
fun BlogListPage() {
    Column(
        SectionStyle.toModifier()
            .padding(top = 6.cssRem)
    ) {
        SectionHeader("Blog")

        SpanText(
            "Thoughts on Android development, architecture, and building apps at scale.",
            Modifier
                .fontSize(1.1.cssRem)
                .color(ColorMode.current.toSitePalette().textSecondary)
                .margin(top = (-2).cssRem, bottom = 2.cssRem)
        )

        Column(Modifier.fillMaxWidth().gap(1.5.cssRem)) {
            blogPosts.forEachIndexed { index, post ->
                ScrollReveal(
                    keyframes = FadeInUp,
                    delay = (index * 100).ms,
                ) {
                    BlogPostCard(post)
                }
            }
        }
    }
}

@Composable
private fun BlogPostCard(post: BlogPost) {
    val sitePalette = ColorMode.current.toSitePalette()

    Column(
        CardStyle.toModifier()
            .fillMaxWidth()
            .gap(0.6.cssRem)
    ) {
        Link(
            post.path,
            Modifier.setVariable(ColorVar, sitePalette.brand.primary),
            variant = UncoloredLinkVariant,
        ) {
            SpanText(
                post.title,
                Modifier.fontSize(1.3.cssRem).fontWeight(FontWeight.SemiBold)
            )
        }

        SpanText(
            post.date,
            Modifier
                .fontSize(0.8.cssRem)
                .fontFamily("JetBrains Mono", "monospace")
                .color(sitePalette.textSecondary)
        )

        SpanText(
            post.description,
            Modifier.lineHeight(1.6).color(sitePalette.textSecondary)
        )

        Row(Modifier.flexWrap(FlexWrap.Wrap).gap(0.5.cssRem).margin(top = 0.25.cssRem)) {
            post.tags.forEach { tag ->
                SpanText(tag, ChipStyle.toModifier())
            }
        }
    }
}
