package dev.ritwik.iama.components.widgets

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.silk.style.CssStyle
import com.varabyte.kobweb.silk.style.base
import com.varabyte.kobweb.silk.style.toModifier
import org.jetbrains.compose.web.css.*
import dev.ritwik.iama.toSitePalette

val BlobContainerStyle = CssStyle.base {
    Modifier
        .fillMaxWidth().fillMaxHeight()
        .position(Position.Absolute).top(0.px).left(0.px)
        .zIndex(0).overflow { x(com.varabyte.kobweb.compose.css.Overflow.Hidden); y(com.varabyte.kobweb.compose.css.Overflow.Hidden) }
}

private fun blobBase(size: CSSLengthOrPercentageValue) = Modifier
    .position(Position.Absolute)
    .width(size).height(size)
    .borderRadius(50.percent)

val Blob1Style = CssStyle.base {
    blobBase(35.cssRem)
        .top((-5).cssRem).right((-10).cssRem)
        .opacity(0.12).background(colorMode.toSitePalette().brand.primary)
        .cssFilter(blur = 80.px)
        .cssAnimation("blob-float-1 20s ease-in-out infinite")
}

val Blob2Style = CssStyle.base {
    blobBase(28.cssRem)
        .bottom((-8).cssRem).left((-8).cssRem)
        .opacity(0.1).background(colorMode.toSitePalette().brand.accent)
        .cssFilter(blur = 70.px)
        .cssAnimation("blob-float-2 25s ease-in-out infinite")
}

val Blob3Style = CssStyle.base {
    blobBase(20.cssRem)
        .top(30.percent).left(50.percent)
        .opacity(0.08).background(colorMode.toSitePalette().brand.primaryVariant)
        .cssFilter(blur = 60.px)
        .cssAnimation("blob-float-3 18s ease-in-out infinite")
}

// Extension helpers to keep CssStyle blocks clean
private fun Modifier.cssFilter(blur: CSSLengthValue): Modifier =
    styleModifier { property("filter", "blur($blur)") }

private fun Modifier.cssAnimation(value: String): Modifier =
    styleModifier { property("animation", value) }

@Composable
fun GradientBlobs() {
    Box(BlobContainerStyle.toModifier()) {
        Box(Blob1Style.toModifier())
        Box(Blob2Style.toModifier())
        Box(Blob3Style.toModifier())
    }
}
