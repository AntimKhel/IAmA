package dev.ritwik.iama.components.sections

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
import com.varabyte.kobweb.silk.components.text.SpanText
import com.varabyte.kobweb.silk.style.CssStyle
import com.varabyte.kobweb.silk.style.animation.toAnimation
import com.varabyte.kobweb.silk.style.base
import com.varabyte.kobweb.silk.style.toModifier
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Li
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Ul
import dev.ritwik.iama.*
import dev.ritwik.iama.components.widgets.ScrollReveal
import dev.ritwik.iama.components.widgets.SectionHeader

// Glowing dot on the timeline
val TimelineDotStyle = CssStyle.base {
    val primary = colorMode.toSitePalette().brand.primary
    Modifier
        .size(16.px)
        .borderRadius(50.percent)
        .backgroundColor(primary)
        .boxShadow(
            blurRadius = 12.px,
            color = primary.toRgb().copyf(alpha = 0.6f)
        )
        .flexShrink(0)
        .position(Position.Relative)
        .zIndex(2)
}

// The glowing vertical connector line
val TimelineLineStyle = CssStyle.base {
    val primary = colorMode.toSitePalette().brand.primary
    Modifier
        .width(2.px)
        .flexGrow(1)
        .minHeight(2.cssRem)
        .background(primary.toRgb().copyf(alpha = 0.3f))
        .position(Position.Relative)
        .zIndex(1)
}

// Outer ring around the dot for the "current" position
val TimelineDotCurrentStyle = CssStyle.base {
    val primary = colorMode.toSitePalette().brand.primary
    Modifier
        .size(16.px)
        .borderRadius(50.percent)
        .backgroundColor(primary)
        .boxShadow(
            blurRadius = 20.px,
            spreadRadius = 4.px,
            color = primary.toRgb().copyf(alpha = 0.5f)
        )
        .flexShrink(0)
        .position(Position.Relative)
        .zIndex(2)
        .styleModifier { property("animation", "dot-pulse 2s ease-in-out infinite") }
}

// Period badge
val PeriodBadgeStyle = CssStyle.base {
    Modifier
        .padding(topBottom = 0.3.cssRem, leftRight = 0.8.cssRem)
        .borderRadius(1.cssRem)
        .fontSize(0.8.cssRem)
        .fontFamily("JetBrains Mono", "monospace")
        .fontWeight(FontWeight.Medium)
        .backgroundColor(colorMode.toSitePalette().brand.primary.toRgb().copyf(alpha = 0.15f))
        .color(colorMode.toSitePalette().brand.primary)
        .border(1.px, LineStyle.Solid, colorMode.toSitePalette().brand.primary.toRgb().copyf(alpha = 0.3f))
}

@Composable
fun ExperienceSection() {
    Column(SectionStyle.toModifier().id("experience")) {
        ScrollReveal {
            SectionHeader("Experience")
        }

        Column(Modifier.fillMaxWidth()) {
            experiences.forEachIndexed { index, experience ->
                ScrollReveal(
                    keyframes = FadeInUp,
                    delay = (index * 200).ms,
                ) {
                    TimelineItem(
                        experience,
                        isFirst = index == 0,
                        isLast = index == experiences.lastIndex
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineItem(experience: Experience, isFirst: Boolean, isLast: Boolean) {
    val sitePalette = ColorMode.current.toSitePalette()

    Row(Modifier.fillMaxWidth().gap(1.5.cssRem)) {
        // Timeline connector column
        Column(
            Modifier.styleModifier { property("align-items", "center") },
        ) {
            // Pulsing dot for current job, static for past
            if (isFirst) {
                Box(TimelineDotCurrentStyle.toModifier())
            } else {
                Box(TimelineDotStyle.toModifier())
            }
            if (!isLast) {
                Box(TimelineLineStyle.toModifier())
            }
        }

        // Content card
        Column(
            CardStyle.toModifier()
                .fillMaxWidth()
                .margin(bottom = 2.cssRem)
                .gap(0.75.cssRem)
        ) {
            // Company + period row
            Row(
                Modifier
                    .fillMaxWidth()
                    .flexWrap(FlexWrap.Wrap)
                    .gap(0.75.cssRem)
                    .styleModifier { property("align-items", "center") }
            ) {
                SpanText(
                    experience.company,
                    Modifier
                        .fontSize(1.3.cssRem)
                        .fontWeight(FontWeight.Bold)
                        .color(sitePalette.brand.primary)
                )
                SpanText(experience.period, PeriodBadgeStyle.toModifier())
            }

            SpanText(
                experience.role,
                Modifier.fontSize(1.05.cssRem).fontWeight(FontWeight.Medium)
            )

            Ul(Modifier.margin(top = 0.5.cssRem).padding(left = 1.2.cssRem).toAttrs()) {
                experience.highlights.forEach { highlight ->
                    Li(Modifier.margin(bottom = 0.4.cssRem).lineHeight(1.7).toAttrs()) {
                        Text(highlight)
                    }
                }
            }

            Row(Modifier.flexWrap(FlexWrap.Wrap).gap(0.5.cssRem).margin(top = 0.5.cssRem)) {
                experience.techStack.forEach { tech ->
                    SpanText(tech, ChipStyle.toModifier())
                }
            }
        }
    }
}
