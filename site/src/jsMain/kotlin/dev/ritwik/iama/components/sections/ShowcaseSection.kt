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
import com.varabyte.kobweb.silk.components.graphics.Image
import com.varabyte.kobweb.silk.components.text.SpanText
import com.varabyte.kobweb.silk.style.CssStyle
import com.varabyte.kobweb.silk.style.base
import com.varabyte.kobweb.silk.style.breakpoint.Breakpoint
import com.varabyte.kobweb.silk.style.toModifier
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import org.jetbrains.compose.web.css.*
import dev.ritwik.iama.*
import dev.ritwik.iama.components.widgets.ScrollReveal
import dev.ritwik.iama.components.widgets.SectionHeader

// ---------------------------------------------------------------------------
// Phone mockup frame
// ---------------------------------------------------------------------------

val PhoneFrameStyle = CssStyle {
    base {
        Modifier
            .width(16.cssRem)
            .borderRadius(1.5.cssRem)
            .border(3.px, LineStyle.Solid, colorMode.toSitePalette().border)
            .backgroundColor(colorMode.toSitePalette().nearBackground)
            .padding(0.4.cssRem)
            .boxShadow(
                blurRadius = 30.px,
                spreadRadius = (-5).px,
                color = colorMode.toSitePalette().brand.primary.toRgb().copyf(alpha = 0.2f)
            )
            .overflow { x(Overflow.Hidden); y(Overflow.Hidden) }
            .transition(
                Transition.of("transform", 500.ms, TransitionTimingFunction.EaseOut),
                Transition.of("box-shadow", 500.ms, TransitionTimingFunction.EaseOut),
            )
    }
    cssRule(":hover") {
        Modifier
            .boxShadow(
                blurRadius = 40.px,
                spreadRadius = 0.px,
                color = colorMode.toSitePalette().brand.primary.toRgb().copyf(alpha = 0.35f)
            )
    }
}

val PhoneScreenStyle = CssStyle.base {
    Modifier
        .fillMaxWidth()
        .borderRadius(1.cssRem)
        .display(DisplayStyle.Block)
}

// ---------------------------------------------------------------------------
// 3D Perspective container for the phone grid
// ---------------------------------------------------------------------------

val ShowcaseGridStyle = CssStyle {
    base {
        Modifier
            .fillMaxWidth()
            .display(DisplayStyle.Flex)
            .flexWrap(FlexWrap.Wrap)
            .justifyContent(JustifyContent.Center)
            .gap(2.cssRem)
            .styleModifier { property("perspective", "1200px") }
    }
}

// Individual phone positions with 3D transforms
val Phone1Style = CssStyle.base {
    Modifier.styleModifier {
        property("transform", "rotateY(-8deg) rotateX(3deg) translateZ(20px)")
    }
}
val Phone2Style = CssStyle.base {
    Modifier.styleModifier {
        property("transform", "rotateY(-3deg) rotateX(-2deg) translateZ(40px) scale(1.05)")
    }
}
val Phone3Style = CssStyle.base {
    Modifier.styleModifier {
        property("transform", "rotateY(0deg) rotateX(0deg) translateZ(60px) scale(1.1)")
    }
}
val Phone4Style = CssStyle.base {
    Modifier.styleModifier {
        property("transform", "rotateY(3deg) rotateX(-2deg) translateZ(40px) scale(1.05)")
    }
}
val Phone5Style = CssStyle.base {
    Modifier.styleModifier {
        property("transform", "rotateY(8deg) rotateX(3deg) translateZ(20px)")
    }
}

// Feature label badge
val FeatureLabelStyle = CssStyle.base {
    Modifier
        .padding(topBottom = 0.3.cssRem, leftRight = 0.8.cssRem)
        .borderRadius(0.5.cssRem)
        .fontSize(0.75.cssRem)
        .fontFamily("JetBrains Mono", "monospace")
        .fontWeight(FontWeight.Medium)
        .backgroundColor(colorMode.toSitePalette().brand.primary.toRgb().copyf(alpha = 0.15f))
        .color(colorMode.toSitePalette().brand.primary)
        .textAlign(TextAlign.Center)
        .margin(top = 0.75.cssRem)
}

// ---------------------------------------------------------------------------
// Section title area with stats
// ---------------------------------------------------------------------------

val ShowcaseStatStyle = CssStyle.base {
    Modifier
        .textAlign(TextAlign.Center)
        .padding(1.cssRem)
}

val StatNumberStyle = CssStyle.base {
    Modifier
        .fontSize(2.5.cssRem)
        .fontWeight(FontWeight.Bold)
        .color(colorMode.toSitePalette().brand.primary)
        .lineHeight(1.2)
}

val StatLabelStyle = CssStyle.base {
    Modifier
        .fontSize(0.85.cssRem)
        .color(colorMode.toSitePalette().textSecondary)
        .margin(top = 0.25.cssRem)
}

// ---------------------------------------------------------------------------
// Showcase data
// ---------------------------------------------------------------------------

private data class PhoneShowcase(
    val image: String,
    val label: String,
    val style: CssStyle,
)

private val showcasePhones = listOf(
    PhoneShowcase("/images/myjio-1.webp", "Home Dashboard", Phone1Style),
    PhoneShowcase("/images/myjio-2.webp", "Recharge Flow", Phone2Style),
    PhoneShowcase("/images/myjio-3.webp", "Plan Selection", Phone3Style),
    PhoneShowcase("/images/myjio-4.webp", "Account Hub", Phone4Style),
    PhoneShowcase("/images/myjio-5.webp", "Jio Services", Phone5Style),
)

private data class Stat(val number: String, val label: String)

private val stats = listOf(
    Stat("100M+", "Monthly Active Users"),
    Stat("4.1\u2605", "Play Store Rating"),
    Stat("50+", "Modules Shipped"),
)

// ---------------------------------------------------------------------------
// Composable
// ---------------------------------------------------------------------------

@Composable
fun ShowcaseSection() {
    Column(SectionStyle.toModifier().id("showcase").maxWidth(80.cssRem)) {
        ScrollReveal {
            SectionHeader("Featured Work")
        }

        ScrollReveal(keyframes = FadeInUp, delay = 100.ms) {
            Column(Modifier.gap(0.25.cssRem).margin(bottom = 2.cssRem)) {
                SpanText(
                    "MyJio \u2014 India's #1 Telecom App",
                    Modifier.fontSize(1.5.cssRem).fontWeight(FontWeight.SemiBold)
                )
                SpanText(
                    "Tech Lead \u00b7 Jetpack Compose \u00b7 Design System \u00b7 React Native",
                    MonoLabelStyle.toModifier()
                )
            }
        }

        // Stats row
        ScrollReveal(keyframes = FadeInUp, delay = 200.ms) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .justifyContent(JustifyContent.Center)
                    .gap(3.cssRem)
                    .flexWrap(FlexWrap.Wrap)
                    .margin(bottom = 3.cssRem)
            ) {
                stats.forEach { stat ->
                    Column(ShowcaseStatStyle.toModifier()) {
                        SpanText(stat.number, StatNumberStyle.toModifier())
                        SpanText(stat.label, StatLabelStyle.toModifier())
                    }
                }
            }
        }

        // Phone showcase grid with 3D perspective
        Box(ShowcaseGridStyle.toModifier()) {
            showcasePhones.forEachIndexed { index, phone ->
                ScrollReveal(
                    keyframes = FadeInUp,
                    delay = (200 + index * 150).ms,
                ) {
                    Column(
                        phone.style.toModifier(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(PhoneFrameStyle.toModifier()) {
                            Image(
                                phone.image,
                                "MyJio - ${phone.label}",
                                PhoneScreenStyle.toModifier()
                            )
                        }
                        SpanText(phone.label, FeatureLabelStyle.toModifier())
                    }
                }
            }
        }
    }
}
