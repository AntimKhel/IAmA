package dev.ritwik.iama.components.widgets

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.dom.svg.*
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.silk.style.CssStyle
import com.varabyte.kobweb.silk.style.base
import com.varabyte.kobweb.silk.style.breakpoint.Breakpoint
import com.varabyte.kobweb.silk.style.toModifier
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import org.jetbrains.compose.web.css.*
import dev.ritwik.iama.toSitePalette

// Bot peeking from the right side
val BotPeekRightStyle = CssStyle {
    base {
        Modifier
            .position(Position.Absolute)
            .right((-1).cssRem)
            .bottom(15.percent)
            .zIndex(2)
            .opacity(0.85)
            .styleModifier { property("animation", "bot-peek-right 3s ease-in-out infinite") }
            .display(DisplayStyle.None)
    }
    Breakpoint.MD {
        Modifier.display(DisplayStyle.Block)
    }
}

// Bot peeking from the left side
val BotPeekLeftStyle = CssStyle {
    base {
        Modifier
            .position(Position.Absolute)
            .left((-1).cssRem)
            .top(20.percent)
            .zIndex(2)
            .opacity(0.75)
            .styleModifier {
                property("animation", "bot-peek-left 4s ease-in-out infinite")
                property("transform", "scaleX(-1)")
            }
            .display(DisplayStyle.None)
    }
    Breakpoint.LG {
        Modifier.display(DisplayStyle.Block)
    }
}

@Composable
fun AndroidBotRight() {
    val color = ColorMode.current.toSitePalette().brand.primary

    Svg(attrs = BotPeekRightStyle.toModifier().toAttrs {
        width(80)
        height(100)
        viewBox(0, 0, 80, 100)
    }) {
        // Head
        Rect(attrs = {
            x(10); y(20); width(60); height(40); rx(20); ry(20)
            fill(color)
        })
        // Eyes
        Circle(attrs = { cx(30); cy(38); r(5); fill(com.varabyte.kobweb.compose.ui.graphics.Colors.White) })
        Circle(attrs = { cx(50); cy(38); r(5); fill(com.varabyte.kobweb.compose.ui.graphics.Colors.White) })
        Circle(attrs = { cx(31); cy(37); r(2); fill(com.varabyte.kobweb.compose.ui.graphics.Colors.Black) })
        Circle(attrs = { cx(51); cy(37); r(2); fill(com.varabyte.kobweb.compose.ui.graphics.Colors.Black) })
        // Antenna
        Line(attrs = { x1(30); y1(20); x2(25); y2(8); stroke(color); strokeWidth(3) })
        Circle(attrs = { cx(25); cy(7); r(3); fill(color) })
        Line(attrs = { x1(50); y1(20); x2(55); y2(8); stroke(color); strokeWidth(3) })
        Circle(attrs = { cx(55); cy(7); r(3); fill(color) })
        // Body
        Rect(attrs = {
            x(15); y(58); width(50); height(35); rx(5); ry(5)
            fill(color)
        })
        // Arms
        Rect(attrs = {
            x(0); y(60); width(12); height(28); rx(6); ry(6)
            fill(color)
        })
        Rect(attrs = {
            x(68); y(60); width(12); height(28); rx(6); ry(6)
            fill(color)
        })
    }
}

@Composable
fun AndroidBotLeft() {
    val color = ColorMode.current.toSitePalette().brand.primary.toRgb().copyf(alpha = 0.7f)

    Svg(attrs = BotPeekLeftStyle.toModifier().toAttrs {
        width(60)
        height(75)
        viewBox(0, 0, 80, 100)
    }) {
        // Head
        Rect(attrs = {
            x(10); y(20); width(60); height(40); rx(20); ry(20)
            fill(color)
        })
        // Eyes
        Circle(attrs = { cx(30); cy(38); r(5); fill(com.varabyte.kobweb.compose.ui.graphics.Colors.White) })
        Circle(attrs = { cx(50); cy(38); r(5); fill(com.varabyte.kobweb.compose.ui.graphics.Colors.White) })
        Circle(attrs = { cx(31); cy(37); r(2); fill(com.varabyte.kobweb.compose.ui.graphics.Colors.Black) })
        Circle(attrs = { cx(51); cy(37); r(2); fill(com.varabyte.kobweb.compose.ui.graphics.Colors.Black) })
        // Antenna
        Line(attrs = { x1(30); y1(20); x2(25); y2(8); stroke(color); strokeWidth(3) })
        Circle(attrs = { cx(25); cy(7); r(3); fill(color) })
        // Body
        Rect(attrs = {
            x(15); y(58); width(50); height(35); rx(5); ry(5)
            fill(color)
        })
    }
}
