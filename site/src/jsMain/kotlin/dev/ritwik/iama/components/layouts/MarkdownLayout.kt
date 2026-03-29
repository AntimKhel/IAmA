package dev.ritwik.iama.components.layouts

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.css.*
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.core.data.add
import com.varabyte.kobweb.core.init.InitRoute
import com.varabyte.kobweb.core.init.InitRouteContext
import com.varabyte.kobweb.core.layout.Layout
import com.varabyte.kobweb.core.rememberPageContext
import com.varabyte.kobweb.silk.components.navigation.Link
import com.varabyte.kobweb.silk.components.navigation.UncoloredLinkVariant
import com.varabyte.kobweb.silk.components.text.SpanText
import com.varabyte.kobweb.silk.style.CssStyle
import com.varabyte.kobweb.silk.style.base
import com.varabyte.kobweb.silk.style.toAttrs
import com.varabyte.kobweb.silk.style.toModifier
import com.varabyte.kobweb.silk.style.vars.color.ColorVar
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import com.varabyte.kobweb.silk.theme.colors.palette.color
import com.varabyte.kobweb.silk.theme.colors.palette.toPalette
import com.varabyte.kobwebx.markdown.markdown
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import dev.ritwik.iama.ChipStyle
import dev.ritwik.iama.toSitePalette

// Back button style
val BackButtonStyle = CssStyle {
    base {
        Modifier
            .padding(topBottom = 0.5.cssRem, leftRight = 1.cssRem)
            .borderRadius(0.5.cssRem)
            .fontSize(0.9.cssRem)
            .fontWeight(FontWeight.Medium)
            .border(1.px, LineStyle.Solid, colorMode.toSitePalette().border)
            .backgroundColor(colorMode.toSitePalette().nearBackground)
            .color(colorMode.toSitePalette().brand.primary)
            .transition(
                Transition.of("background-color", 200.ms, TransitionTimingFunction.EaseOut),
                Transition.of("border-color", 200.ms, TransitionTimingFunction.EaseOut),
            )
    }
    cssRule(":hover") {
        Modifier
            .backgroundColor(colorMode.toSitePalette().brand.primary.toRgb().copyf(alpha = 0.1f))
            .styleModifier {
                property("border-color", colorMode.toSitePalette().brand.primary.toString())
            }
    }
}

// Blog post article container
val BlogArticleStyle = CssStyle {
    base {
        Modifier
            .fillMaxWidth()
            .maxWidth(48.cssRem)
            .margin(leftRight = autoLength)
            .padding(top = 6.cssRem, bottom = 4.cssRem, leftRight = 2.cssRem)
    }
}

// Markdown content styling
val MarkdownContentStyle = CssStyle {
    base {
        Modifier
            .fillMaxWidth()
            .lineHeight(1.8)
            .fontSize(1.05.cssRem)
    }

    // Headings - hide the auto-generated H1 (we show our own)
    cssRule(" > h1:first-child") {
        Modifier.display(DisplayStyle.None)
    }

    cssRule("h2") {
        Modifier
            .fontSize(1.8.cssRem)
            .fontWeight(FontWeight.Bold)
            .margin(top = 3.cssRem, bottom = 1.cssRem)
            .lineHeight(1.3)
            .color(colorMode.toPalette().color)
    }

    cssRule("h3") {
        Modifier
            .fontSize(1.4.cssRem)
            .fontWeight(FontWeight.SemiBold)
            .margin(top = 2.cssRem, bottom = 0.75.cssRem)
            .lineHeight(1.3)
            .color(colorMode.toPalette().color)
    }

    cssRule("h4") {
        Modifier
            .fontSize(1.1.cssRem)
            .fontWeight(FontWeight.SemiBold)
            .margin(top = 1.5.cssRem, bottom = 0.5.cssRem)
    }

    cssRule("p") {
        Modifier
            .margin(bottom = 1.25.cssRem)
            .color(colorMode.toPalette().color.toRgb().copyf(alpha = 0.9f))
    }

    cssRule("ul, ol") {
        Modifier
            .fillMaxWidth()
            .overflowWrap(OverflowWrap.BreakWord)
            .padding(left = 1.5.cssRem)
            .margin(bottom = 1.25.cssRem)
    }

    cssRule("li") {
        Modifier
            .margin(bottom = 0.5.cssRem)
            .lineHeight(1.7)
    }

    cssRule("strong") {
        Modifier
            .fontWeight(FontWeight.SemiBold)
            .color(colorMode.toPalette().color)
    }

    // Inline code
    cssRule("code") {
        Modifier
            .fontFamily("JetBrains Mono", "monospace")
            .fontSize(0.9.cssRem)
            .padding(topBottom = 0.15.cssRem, leftRight = 0.4.cssRem)
            .borderRadius(0.25.cssRem)
            .backgroundColor(colorMode.toSitePalette().nearBackground)
            .color(colorMode.toSitePalette().brand.primary)
            .fontWeight(FontWeight.Medium)
    }

    // Code blocks
    cssRule("pre") {
        Modifier
            .margin(top = 0.5.cssRem, bottom = 1.5.cssRem)
            .fillMaxWidth()
            .borderRadius(0.75.cssRem)
            .border(1.px, LineStyle.Solid, colorMode.toSitePalette().border)
            .styleModifier { property("overflow", "hidden") }
    }

    cssRule("pre > code") {
        Modifier
            .display(DisplayStyle.Block)
            .fillMaxWidth()
            .backgroundColor(colorMode.toSitePalette().nearBackground)
            .padding(1.25.cssRem)
            .fontSize(0.85.cssRem)
            .lineHeight(1.6)
            .borderRadius(0.px)
            .styleModifier { property("overflow-x", "auto") }
    }

    // Blockquotes
    cssRule("blockquote") {
        Modifier
            .margin(topBottom = 1.5.cssRem, leftRight = 0.px)
            .padding(topBottom = 0.5.cssRem, leftRight = 1.25.cssRem)
            .styleModifier {
                property("border-left", "4px solid ${colorMode.toSitePalette().brand.primary}")
            }
            .backgroundColor(colorMode.toSitePalette().brand.primary.toRgb().copyf(alpha = 0.05f))
            .borderRadius(0.px, 0.5.cssRem, 0.5.cssRem, 0.px)
    }

    // Tables
    cssRule("table") {
        Modifier
            .fillMaxWidth()
            .margin(topBottom = 1.5.cssRem)
            .borderRadius(0.5.cssRem)
            .styleModifier {
                property("border-collapse", "collapse")
                property("overflow", "hidden")
            }
    }

    cssRule("th") {
        Modifier
            .padding(0.75.cssRem)
            .textAlign(TextAlign.Start)
            .fontWeight(FontWeight.SemiBold)
            .backgroundColor(colorMode.toSitePalette().nearBackground)
            .fontSize(0.9.cssRem)
            .border(1.px, LineStyle.Solid, colorMode.toSitePalette().border)
    }

    cssRule("td") {
        Modifier
            .padding(0.75.cssRem)
            .fontSize(0.95.cssRem)
            .border(1.px, LineStyle.Solid, colorMode.toSitePalette().border)
    }

    // Links
    cssRule("a") {
        Modifier
            .color(colorMode.toSitePalette().brand.primary)
            .fontWeight(FontWeight.Medium)
            .textDecorationLine(TextDecorationLine.None)
            .transition(Transition.of("color", 200.ms, TransitionTimingFunction.EaseOut))
    }

    // Horizontal rules
    cssRule("hr") {
        Modifier
            .margin(topBottom = 2.cssRem)
            .border(0.px)
            .styleModifier {
                property("border-top", "1px solid ${colorMode.toSitePalette().border}")
            }
    }
}

// Blog post header style
val BlogHeaderStyle = CssStyle.base {
    Modifier
        .fillMaxWidth()
        .margin(bottom = 2.5.cssRem)
        .padding(bottom = 2.cssRem)
        .styleModifier {
            property("border-bottom", "1px solid ${colorMode.toSitePalette().border}")
        }
}

@InitRoute
fun initMarkdownLayout(ctx: InitRouteContext) {
    val title = ctx.markdown!!.frontMatter["title"]?.singleOrNull()
    require(title != null) { "Markdown file must set \"title\" in frontmatter" }

    ctx.data.add(PageLayoutData(title))
}

@Composable
@Layout(".components.layouts.PageLayout")
fun MarkdownLayout(content: @Composable () -> Unit) {
    val ctx = rememberPageContext()
    val sitePalette = ColorMode.current.toSitePalette()

    val title = ctx.markdown?.frontMatter?.get("title")?.firstOrNull() ?: "Untitled"
    val date = ctx.markdown?.frontMatter?.get("date")?.firstOrNull()
    val tags = ctx.markdown?.frontMatter?.get("tags") ?: emptyList()

    Column(BlogArticleStyle.toModifier()) {
        // Back button
        Link(
            "/blog",
            BackButtonStyle.toModifier().margin(bottom = 2.cssRem),
            variant = UncoloredLinkVariant,
        ) {
            SpanText("\u2190 Back to Blog")
        }

        // Post header
        Column(BlogHeaderStyle.toModifier().gap(0.75.cssRem)) {
            // Title
            SpanText(
                title,
                Modifier
                    .fontSize(2.4.cssRem)
                    .fontWeight(FontWeight.Bold)
                    .lineHeight(1.3)
            )

            // Date
            if (date != null) {
                SpanText(
                    date,
                    Modifier
                        .fontSize(0.85.cssRem)
                        .fontFamily("JetBrains Mono", "monospace")
                        .color(sitePalette.textSecondary)
                )
            }

            // Tags
            if (tags.isNotEmpty()) {
                Row(Modifier.gap(0.5.cssRem).flexWrap(FlexWrap.Wrap)) {
                    tags.forEach { tag ->
                        SpanText(tag, ChipStyle.toModifier())
                    }
                }
            }
        }

        // Markdown content
        Div(MarkdownContentStyle.toAttrs()) {
            content()
        }

        // Footer separator + back link
        Box(
            Modifier
                .fillMaxWidth()
                .margin(top = 3.cssRem)
                .padding(top = 2.cssRem)
                .styleModifier {
                    property("border-top", "1px solid ${sitePalette.border}")
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Link(
                "/blog",
                BackButtonStyle.toModifier(),
                variant = UncoloredLinkVariant,
            ) {
                SpanText("\u2190 Back to all posts")
            }
        }
    }
}
