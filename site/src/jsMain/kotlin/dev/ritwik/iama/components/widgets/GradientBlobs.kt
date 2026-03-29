package dev.ritwik.iama.components.widgets

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.silk.style.CssStyle
import com.varabyte.kobweb.silk.style.animation.toAnimation
import com.varabyte.kobweb.silk.style.base
import com.varabyte.kobweb.silk.style.toModifier
import org.jetbrains.compose.web.css.*
import dev.ritwik.iama.*

val BlobContainerStyle = CssStyle.base {
    Modifier
        .fillMaxWidth()
        .fillMaxHeight()
        .position(Position.Absolute)
        .top(0.px)
        .left(0.px)
        .zIndex(0)
        .styleModifier { property("overflow", "hidden") }
}

val Blob1Style = CssStyle.base {
    val primary = colorMode.toSitePalette().brand.primary
    Modifier
        .position(Position.Absolute)
        .width(35.cssRem)
        .height(35.cssRem)
        .borderRadius(50.percent)
        .top((-5).cssRem)
        .right((-10).cssRem)
        .opacity(0.12)
        .background(primary)
        .styleModifier {
            property("filter", "blur(80px)")
            property("animation", "blob-float-1 20s ease-in-out infinite")
        }
}

val Blob2Style = CssStyle.base {
    val accent = colorMode.toSitePalette().brand.accent
    Modifier
        .position(Position.Absolute)
        .width(28.cssRem)
        .height(28.cssRem)
        .borderRadius(50.percent)
        .bottom((-8).cssRem)
        .left((-8).cssRem)
        .opacity(0.1)
        .background(accent)
        .styleModifier {
            property("filter", "blur(70px)")
            property("animation", "blob-float-2 25s ease-in-out infinite")
        }
}

val Blob3Style = CssStyle.base {
    val primaryVariant = colorMode.toSitePalette().brand.primaryVariant
    Modifier
        .position(Position.Absolute)
        .width(20.cssRem)
        .height(20.cssRem)
        .borderRadius(50.percent)
        .top(30.percent)
        .left(50.percent)
        .opacity(0.08)
        .background(primaryVariant)
        .styleModifier {
            property("filter", "blur(60px)")
            property("animation", "blob-float-3 18s ease-in-out infinite")
        }
}

@Composable
fun GradientBlobs() {
    Box(BlobContainerStyle.toModifier()) {
        Box(Blob1Style.toModifier())
        Box(Blob2Style.toModifier())
        Box(Blob3Style.toModifier())
    }
}
