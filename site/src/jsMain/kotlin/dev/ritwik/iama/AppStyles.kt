package dev.ritwik.iama

import com.varabyte.kobweb.compose.css.*
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.graphics.Colors
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.silk.components.forms.ButtonStyle
import com.varabyte.kobweb.silk.components.forms.ButtonVars
import com.varabyte.kobweb.silk.components.layout.HorizontalDividerStyle
import com.varabyte.kobweb.silk.init.InitSilk
import com.varabyte.kobweb.silk.init.InitSilkContext
import com.varabyte.kobweb.silk.init.registerStyleBase
import com.varabyte.kobweb.silk.style.CssStyle
import com.varabyte.kobweb.silk.style.addVariantBase
import com.varabyte.kobweb.silk.style.base
import com.varabyte.kobweb.silk.theme.colors.palette.color
import com.varabyte.kobweb.silk.theme.colors.palette.toPalette
import com.varabyte.kobweb.silk.theme.modifyStyleBase
import org.jetbrains.compose.web.css.*

// ---------------------------------------------------------------------------
// Global base styles
// ---------------------------------------------------------------------------

@InitSilk
fun initSiteStyles(ctx: InitSilkContext) {
    ctx.stylesheet.apply {
        registerStyle("html") {
            cssRule(CSSMediaQuery.MediaFeature("prefers-reduced-motion", StylePropertyValue("no-preference"))) {
                Modifier.scrollBehavior(ScrollBehavior.Smooth)
            }
        }
        registerStyleBase("body") {
            Modifier
                .fontFamily("Inter", "-apple-system", "BlinkMacSystemFont", "Segoe UI", "Roboto", "sans-serif")
                .fontSize(18.px)
                .lineHeight(1.5)
        }
    }
    ctx.theme.modifyStyleBase(HorizontalDividerStyle) { Modifier.fillMaxWidth() }
}

// ---------------------------------------------------------------------------
// Generic reusable text styles
// ---------------------------------------------------------------------------

val HeadlineTextStyle = CssStyle.base {
    Modifier.fontSize(3.cssRem).textAlign(TextAlign.Start).lineHeight(1.2).fontWeight(FontWeight.Bold)
}

val SubheadlineTextStyle = CssStyle.base {
    Modifier
        .fontSize(1.1.cssRem)
        .textAlign(TextAlign.Start)
        .color(colorMode.toPalette().color.toRgb().copyf(alpha = 0.7f))
        .lineHeight(1.6)
}

val HeadlineCardStyle = CssStyle.base {
    Modifier.fontSize(1.3.cssRem).fontWeight(FontWeight.SemiBold)
}

val MonoLabelStyle = CssStyle.base {
    Modifier
        .fontSize(0.8.cssRem)
        .fontFamily("JetBrains Mono", "monospace")
        .color(colorMode.toSitePalette().textSecondary)
}

// ---------------------------------------------------------------------------
// Layout styles
// ---------------------------------------------------------------------------

val SectionStyle = CssStyle {
    base {
        Modifier
            .fillMaxWidth()
            .padding(topBottom = 5.cssRem, leftRight = 2.cssRem)
            .maxWidth(70.cssRem)
            .margin(leftRight = autoLength)
    }
}

val AltSectionWrapperStyle = CssStyle.base {
    Modifier
        .fillMaxWidth()
        .backgroundColor(colorMode.toSitePalette().nearBackground)
        .position(Position.Relative)
}

val SectionTitleStyle = CssStyle.base {
    Modifier.fontSize(2.2.cssRem).fontWeight(FontWeight.Bold)
}

// ---------------------------------------------------------------------------
// Card with 3D tilt + glow border on hover
// ---------------------------------------------------------------------------

val CardStyle = CssStyle {
    base {
        Modifier
            .padding(1.5.cssRem)
            .borderRadius(1.cssRem)
            .backgroundColor(colorMode.toSitePalette().nearBackground)
            .border(1.px, LineStyle.Solid, colorMode.toSitePalette().border)
            .transition(
                Transition.of("transform", 300.ms, TransitionTimingFunction.EaseOut),
                Transition.of("box-shadow", 300.ms, TransitionTimingFunction.EaseOut),
                Transition.of("border-color", 300.ms, TransitionTimingFunction.EaseOut),
            )
    }
    cssRule(":hover") {
        Modifier
            .translateY((-4).px)
            .border(1.px, LineStyle.Solid, colorMode.toSitePalette().brand.primary)
            .boxShadow(blurRadius = 20.px, color = colorMode.toSitePalette().brand.primary.toRgb().copyf(alpha = 0.15f))
    }
}

// ---------------------------------------------------------------------------
// Chip / tag pill
// ---------------------------------------------------------------------------

val ChipStyle = CssStyle {
    base {
        Modifier
            .padding(topBottom = 0.4.cssRem, leftRight = 1.cssRem)
            .borderRadius(2.cssRem)
            .fontSize(0.85.cssRem)
            .fontWeight(FontWeight.Medium)
            .backgroundColor(colorMode.toSitePalette().brand.primary.toRgb().copyf(alpha = 0.15f))
            .color(colorMode.toSitePalette().brand.primary)
            .transition(
                Transition.of("transform", 200.ms, TransitionTimingFunction.EaseOut),
                Transition.of("background-color", 200.ms, TransitionTimingFunction.EaseOut),
            )
    }
    cssRule(":hover") {
        Modifier
            .scale(1.05)
            .backgroundColor(colorMode.toSitePalette().brand.primary.toRgb().copyf(alpha = 0.25f))
    }
}

// ---------------------------------------------------------------------------
// Shared variants
// ---------------------------------------------------------------------------

val CircleButtonVariant = ButtonStyle.addVariantBase {
    Modifier.padding(0.px).borderRadius(50.percent)
}

val UncoloredButtonVariant = ButtonStyle.addVariantBase {
    Modifier.setVariable(ButtonVars.BackgroundDefaultColor, Colors.Transparent)
}

// ---------------------------------------------------------------------------
// Utility styles for overflow / user-select / perspective
// ---------------------------------------------------------------------------

val OverflowHiddenStyle = CssStyle.base {
    Modifier.overflow { x(Overflow.Hidden); y(Overflow.Hidden) }
}

val NoSelectStyle = CssStyle.base {
    Modifier.userSelect(UserSelect.None)
}
