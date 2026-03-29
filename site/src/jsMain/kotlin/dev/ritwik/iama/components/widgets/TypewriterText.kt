package dev.ritwik.iama.components.widgets

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.silk.style.CssStyle
import com.varabyte.kobweb.silk.style.base
import com.varabyte.kobweb.silk.style.toModifier
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import dev.ritwik.iama.toSitePalette

val TypewriterContainerStyle = CssStyle {
    base {
        Modifier
            .padding(topBottom = 0.5.cssRem, leftRight = 1.2.cssRem)
            .borderRadius(2.cssRem)
            .fontSize(0.85.cssRem)
            .fontFamily("JetBrains Mono", "monospace")
            .margin(bottom = 1.5.cssRem)
            .border(1.px, LineStyle.Solid, colorMode.toSitePalette().border)
            .backgroundColor(colorMode.toSitePalette().nearBackground)
            .minHeight(2.2.cssRem)
            .display(DisplayStyle.Flex)
            .alignItems(AlignItems.Center)
    }
}

val CursorStyle = CssStyle.base {
    Modifier
        .display(DisplayStyle.InlineBlock)
        .width(2.px).height(1.1.cssRem)
        .backgroundColor(colorMode.toSitePalette().brand.primary)
        .margin(left = 2.px)
        .styleModifier { property("animation", "cursor-blink 1s step-end infinite") }
}

private val facts = listOf(
    "6+ years building Android at scale",
    "Tech Lead for MyJio \u2014 100M+ users",
    "Built Jio Design System in Compose",
    "AWS Certified Developer Associate",
    "NIT Hamirpur CSE \u2014 8.32 CGPI",
    "Shipped apps for Govt. of Andhra Pradesh",
    "OTA updates architect at Jio",
)

@Composable
fun TypewriterText() {
    val sitePalette = ColorMode.current.toSitePalette()
    var displayedText by remember { mutableStateOf("") }
    var factIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            val fact = facts[factIndex % facts.size]
            for (i in 1..fact.length) { displayedText = fact.substring(0, i); delay(40) }
            delay(4000)
            for (i in fact.length downTo 0) { displayedText = fact.substring(0, i); delay(25) }
            delay(300)
            factIndex++
        }
    }

    Span(TypewriterContainerStyle.toModifier().toAttrs()) {
        Span(Modifier.color(sitePalette.brand.primary).fontWeight(FontWeight.Bold).toAttrs()) { Text("> ") }
        Span(Modifier.toAttrs()) { Text(displayedText) }
        Span(CursorStyle.toModifier().toAttrs())
    }
}
