package dev.ritwik.iama.components.sections

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.css.TextAlign
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.core.rememberPageContext
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.silk.components.text.SpanText
import com.varabyte.kobweb.silk.style.CssStyle
import com.varabyte.kobweb.silk.style.animation.toAnimation
import com.varabyte.kobweb.silk.style.base
import com.varabyte.kobweb.silk.style.breakpoint.Breakpoint
import com.varabyte.kobweb.silk.style.toModifier
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import com.varabyte.kobweb.silk.theme.colors.ColorPalettes
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import dev.ritwik.iama.*
import dev.ritwik.iama.components.widgets.GradientBlobs

val HeroStyle = CssStyle {
    base {
        Modifier
            .fillMaxWidth()
            .minHeight(100.vh)
            .display(DisplayStyle.Flex)
            .flexDirection(FlexDirection.Column)
            .justifyContent(JustifyContent.Center)
            .alignItems(AlignItems.Center)
            .textAlign(TextAlign.Center)
            .padding(leftRight = 2.cssRem)
            .position(Position.Relative)
            .styleModifier { property("overflow", "hidden") }
    }
}

val HeroNameStyle = CssStyle {
    base {
        Modifier
            .fontSize(2.5.cssRem)
            .fontWeight(FontWeight.Bold)
            .lineHeight(1.2)
            .animation(
                HeroFadeIn.toAnimation(
                    duration = 800.ms,
                    timingFunction = AnimationTimingFunction.EaseOut,
                    fillMode = AnimationFillMode.Both,
                )
            )
    }
    Breakpoint.MD {
        Modifier.fontSize(4.5.cssRem)
    }
}

val HeroSubtitleStyle = CssStyle {
    base {
        Modifier
            .fontSize(1.1.cssRem)
            .fontWeight(FontWeight.Normal)
            .margin(top = 1.cssRem)
            .animation(
                HeroFadeIn.toAnimation(
                    duration = 800.ms,
                    timingFunction = AnimationTimingFunction.EaseOut,
                    delay = 200.ms,
                    fillMode = AnimationFillMode.Both,
                )
            )
    }
    Breakpoint.MD {
        Modifier.fontSize(1.4.cssRem)
    }
}

val HeroTaglineStyle = CssStyle {
    base {
        Modifier
            .fontSize(1.cssRem)
            .margin(top = 0.75.cssRem)
            .fontFamily("JetBrains Mono", "monospace")
            .animation(
                HeroFadeIn.toAnimation(
                    duration = 800.ms,
                    timingFunction = AnimationTimingFunction.EaseOut,
                    delay = 400.ms,
                    fillMode = AnimationFillMode.Both,
                )
            )
    }
    Breakpoint.MD {
        Modifier.fontSize(1.1.cssRem)
    }
}

val HeroButtonsStyle = CssStyle {
    base {
        Modifier
            .margin(top = 2.5.cssRem)
            .gap(1.cssRem)
            .display(DisplayStyle.Flex)
            .flexWrap(FlexWrap.Wrap)
            .justifyContent(JustifyContent.Center)
            .animation(
                HeroFadeIn.toAnimation(
                    duration = 800.ms,
                    timingFunction = AnimationTimingFunction.EaseOut,
                    delay = 600.ms,
                    fillMode = AnimationFillMode.Both,
                )
            )
    }
}

// Decorative terminal-like badge
val TerminalBadgeStyle = CssStyle {
    base {
        Modifier
            .padding(topBottom = 0.5.cssRem, leftRight = 1.2.cssRem)
            .borderRadius(2.cssRem)
            .fontSize(0.85.cssRem)
            .fontFamily("JetBrains Mono", "monospace")
            .margin(bottom = 1.5.cssRem)
            .border(1.px, LineStyle.Solid, colorMode.toSitePalette().border)
            .backgroundColor(colorMode.toSitePalette().nearBackground)
            .animation(
                HeroFadeIn.toAnimation(
                    duration = 800.ms,
                    timingFunction = AnimationTimingFunction.EaseOut,
                    delay = 100.ms,
                    fillMode = AnimationFillMode.Both,
                )
            )
    }
}

// Glowing line separator under hero
val GlowLineStyle = CssStyle {
    base {
        Modifier
            .width(8.cssRem)
            .height(3.px)
            .borderRadius(2.px)
            .margin(top = 3.cssRem)
            .background(colorMode.toSitePalette().brand.primary)
            .styleModifier { property("animation", "glow-pulse 3s ease-in-out infinite") }
    }
}

@Composable
fun HeroSection() {
    val sitePalette = ColorMode.current.toSitePalette()
    val ctx = rememberPageContext()

    Box(HeroStyle.toModifier()) {
        // Animated gradient blobs in the background
        GradientBlobs()

        // Content on top
        Column(
            Modifier.zIndex(1).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Terminal-like badge
            Span(TerminalBadgeStyle.toModifier().toAttrs()) {
                SpanText("> ", Modifier.color(sitePalette.brand.primary))
                Text("6+ years building Android at scale")
            }

            SpanText(
                "Ritwik Raj Srivastava",
                HeroNameStyle.toModifier()
            )

            SpanText(
                "Software Engineer | Tech Lead - Android",
                HeroSubtitleStyle.toModifier().color(sitePalette.textSecondary)
            )

            Div(
                HeroTaglineStyle.toModifier().toAttrs()
            ) {
                SpanText("Building apps used by ", Modifier.color(sitePalette.textSecondary))
                SpanText("100M+", Modifier.color(sitePalette.brand.primary).fontWeight(FontWeight.Bold))
                SpanText(" users at ", Modifier.color(sitePalette.textSecondary))
                SpanText("Jio", Modifier.color(sitePalette.brand.primary).fontWeight(FontWeight.Bold))
            }

            Row(HeroButtonsStyle.toModifier()) {
                Button(
                    onClick = { ctx.router.tryRoutingTo("/#experience") },
                    colorPalette = ColorPalettes.Green,
                ) {
                    SpanText("View Experience")
                }

                Button(
                    onClick = { ctx.router.tryRoutingTo("/blog") },
                    colorPalette = ColorPalettes.Blue,
                ) {
                    SpanText("Read Blog")
                }
            }

            Box(GlowLineStyle.toModifier())
        }
    }
}
