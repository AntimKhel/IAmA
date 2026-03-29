package dev.ritwik.iama.components.widgets

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.silk.components.text.SpanText
import com.varabyte.kobweb.silk.style.toModifier
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import org.jetbrains.compose.web.css.*
import dev.ritwik.iama.SectionTitleStyle
import dev.ritwik.iama.toSitePalette

@Composable
fun SectionHeader(title: String) {
    Column(Modifier.gap(0.5.cssRem).margin(bottom = 3.cssRem)) {
        SpanText(title, SectionTitleStyle.toModifier())
        Box(
            Modifier
                .width(4.cssRem)
                .height(4.px)
                .borderRadius(2.px)
                .backgroundColor(ColorMode.current.toSitePalette().brand.primary)
        )
    }
}
