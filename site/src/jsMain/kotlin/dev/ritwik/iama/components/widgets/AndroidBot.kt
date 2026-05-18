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
            .styleModifier { 
                property("animation", "bot-peek-right 4s cubic-bezier(0.25, 1, 0.5, 1) infinite") 
                property("filter", "drop-shadow(-5px 10px 10px rgba(0,0,0,0.3))")
            }
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
                property("animation", "bot-peek-left 5s cubic-bezier(0.25, 1, 0.5, 1) infinite")
                property("transform", "scaleX(-1)")
                property("filter", "drop-shadow(5px 10px 10px rgba(0,0,0,0.3))")
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
        width(100)
        height(120)
        viewBox(0, 0, 100, 120)
    }) {
        // Antennas
        Line(attrs = { x1(35); y1(30); x2(25); y2(12); stroke(color); strokeWidth(3); styleModifier { property("stroke-linecap", "round") } })
        Line(attrs = { x1(65); y1(30); x2(75); y2(12); stroke(color); strokeWidth(3); styleModifier { property("stroke-linecap", "round") } })
        
        // Head (perfect semi-circle)
        Path(attrs = {
            d("M 20 60 A 30 30 0 0 1 80 60 Z")
            fill(color)
        })
        
        // Eyes
        Circle(attrs = { cx(35); cy(45); r(3.5); fill(com.varabyte.kobweb.compose.ui.graphics.Colors.White) })
        Circle(attrs = { cx(65); cy(45); r(3.5); fill(com.varabyte.kobweb.compose.ui.graphics.Colors.White) })
        
        // Body (rounded rectangle with separated top)
        Path(attrs = {
            d("M 20 63 L 80 63 L 80 90 A 15 15 0 0 1 65 105 L 35 105 A 15 15 0 0 1 20 90 Z")
            fill(color)
        })
        
        // Arms
        Rect(attrs = { x(4); y(63); width(12); height(35); rx(6); ry(6); fill(color) })
        Rect(attrs = { x(84); y(63); width(12); height(35); rx(6); ry(6); fill(color) })
    }
}

@Composable
fun AndroidBotLeft() {
    val color = ColorMode.current.toSitePalette().brand.primary.toRgb().copyf(alpha = 0.7f)

    Svg(attrs = BotPeekLeftStyle.toModifier().toAttrs {
        width(80)
        height(96)
        viewBox(0, 0, 100, 120)
    }) {
        // Antennas
        Line(attrs = { x1(35); y1(30); x2(25); y2(12); stroke(color); strokeWidth(3); styleModifier { property("stroke-linecap", "round") } })
        Line(attrs = { x1(65); y1(30); x2(75); y2(12); stroke(color); strokeWidth(3); styleModifier { property("stroke-linecap", "round") } })
        
        // Head (perfect semi-circle)
        Path(attrs = {
            d("M 20 60 A 30 30 0 0 1 80 60 Z")
            fill(color)
        })
        
        // Eyes
        Circle(attrs = { cx(35); cy(45); r(3.5); fill(com.varabyte.kobweb.compose.ui.graphics.Colors.White) })
        Circle(attrs = { cx(65); cy(45); r(3.5); fill(com.varabyte.kobweb.compose.ui.graphics.Colors.White) })
        
        // Body
        Path(attrs = {
            d("M 20 63 L 80 63 L 80 90 A 15 15 0 0 1 65 105 L 35 105 A 15 15 0 0 1 20 90 Z")
            fill(color)
        })
        
        // Only one arm visible on the left side
        Rect(attrs = { x(4); y(63); width(12); height(35); rx(6); ry(6); fill(color) })
    }
}
