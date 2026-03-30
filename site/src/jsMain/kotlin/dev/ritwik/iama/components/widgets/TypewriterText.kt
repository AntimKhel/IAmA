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
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import dev.ritwik.iama.toSitePalette

val TypewriterContainerStyle = CssStyle {
    base {
        Modifier
            .padding(topBottom = 0.6.cssRem, leftRight = 1.2.cssRem)
            .borderRadius(0.75.cssRem)
            .fontSize(0.85.cssRem)
            .fontFamily("JetBrains Mono", "monospace")
            .margin(bottom = 1.5.cssRem)
            .border(1.px, LineStyle.Solid, colorMode.toSitePalette().border)
            .backgroundColor(colorMode.toSitePalette().nearBackground)
            .display(DisplayStyle.InlineBlock)
            .lineHeight(1.8)
    }
}

val CursorStyle = CssStyle.base {
    Modifier
        .display(DisplayStyle.InlineBlock)
        .width(2.px).height(1.1.cssRem)
        .backgroundColor(colorMode.toSitePalette().brand.primary)
        .margin(left = 2.px)
        .styleModifier { property("animation", "cursor-blink 1s step-end infinite") }
        .styleModifier { property("vertical-align", "middle") }
}

private val lines = listOf(
    "Built Jio Design System in Compose",
    "Building apps for 100M+ users",
)

@Composable
fun TypewriterText() {
    val sitePalette = ColorMode.current.toSitePalette()
    var completedLines by remember { mutableStateOf(0) }
    var currentLineText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        for (lineIndex in lines.indices) {
            val line = lines[lineIndex]
            for (i in 1..line.length) {
                currentLineText = line.substring(0, i)
                delay(40)
            }
            if (lineIndex < lines.lastIndex) {
                delay(500)
                completedLines = lineIndex + 1
                currentLineText = ""
            }
        }
    }

    Span(TypewriterContainerStyle.toModifier().toAttrs()) {
        for (i in 0 until completedLines) {
            Span(Modifier.color(sitePalette.brand.primary).fontWeight(FontWeight.Bold).toAttrs()) { Text("> ") }
            Span(Modifier.toAttrs()) { Text(lines[i]) }
            Br()
        }
        Span(Modifier.color(sitePalette.brand.primary).fontWeight(FontWeight.Bold).toAttrs()) { Text("> ") }
        Span(Modifier.toAttrs()) { Text(currentLineText) }
        Span(CursorStyle.toModifier().toAttrs())
    }
}
