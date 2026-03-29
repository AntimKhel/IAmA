package dev.ritwik.iama.pages.blog

import androidx.compose.runtime.Composable
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
import dev.ritwik.iama.blog.BlogEntry
import dev.ritwik.iama.blog.blogEntries
import dev.ritwik.iama.components.layouts.PageLayoutData
import dev.ritwik.iama.components.widgets.ScrollReveal
import dev.ritwik.iama.components.widgets.SectionHeader

@InitRoute
fun initBlogPage(ctx: InitRouteContext) {
    ctx.data.add(PageLayoutData("Blog"))
}

@Page
@Layout(".components.layouts.PageLayout")
@Composable
fun BlogListPage() {
    Column(SectionStyle.toModifier().padding(top = 6.cssRem)) {
        SectionHeader("Blog")

        SpanText(
            "Thoughts on Android development, architecture, and building apps at scale.",
            SubheadlineTextStyle.toModifier().margin(top = (-2).cssRem, bottom = 2.cssRem)
        )

        Column(Modifier.fillMaxWidth().gap(1.5.cssRem)) {
            blogEntries.forEachIndexed { index, post ->
                ScrollReveal(keyframes = FadeInUp, delay = (index * 100).ms) {
                    BlogPostCard(post)
                }
            }
        }
    }
}

@Composable
private fun BlogPostCard(post: BlogEntry) {
    val sitePalette = ColorMode.current.toSitePalette()

    Column(CardStyle.toModifier().fillMaxWidth().gap(0.6.cssRem)) {
        Link(
            post.route,
            Modifier.setVariable(ColorVar, sitePalette.brand.primary),
            variant = UncoloredLinkVariant,
        ) {
            SpanText(post.title, HeadlineCardStyle.toModifier())
        }

        SpanText(post.date, MonoLabelStyle.toModifier())

        SpanText(
            post.description,
            SubheadlineTextStyle.toModifier()
        )

        Row(Modifier.flexWrap(FlexWrap.Wrap).gap(0.5.cssRem).margin(top = 0.25.cssRem)) {
            post.tags.forEach { tag -> SpanText(tag, ChipStyle.toModifier()) }
        }
    }
}
