package dev.ritwik.iama.components.sections

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.css.TextAlign
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.silk.components.navigation.Link
import com.varabyte.kobweb.silk.components.navigation.UncoloredLinkVariant
import com.varabyte.kobweb.silk.components.text.SpanText
import com.varabyte.kobweb.silk.style.CssStyle
import com.varabyte.kobweb.silk.style.base
import com.varabyte.kobweb.silk.style.toModifier
import com.varabyte.kobweb.silk.style.vars.color.ColorVar
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import org.jetbrains.compose.web.css.*
import dev.ritwik.iama.toSitePalette

val FooterStyle = CssStyle.base {
    Modifier
        .backgroundColor(colorMode.toSitePalette().nearBackground)
        .padding(topBottom = 2.cssRem, leftRight = 2.cssRem)
}

@Composable
fun Footer(modifier: Modifier = Modifier) {
    val sitePalette = ColorMode.current.toSitePalette()

    Column(
        FooterStyle.toModifier().then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Contact links
        Row(
            Modifier.gap(2.cssRem).margin(bottom = 1.cssRem),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Link(
                "mailto:raj.ritwik96@gmail.com",
                "Email",
                Modifier.setVariable(ColorVar, sitePalette.brand.primary),
                variant = UncoloredLinkVariant,
            )
            SpanText("|", Modifier.color(sitePalette.textSecondary))
            Link(
                "https://www.linkedin.com/in/ritwik787/",
                "LinkedIn",
                Modifier.setVariable(ColorVar, sitePalette.brand.primary),
                variant = UncoloredLinkVariant,
            )
            SpanText("|", Modifier.color(sitePalette.textSecondary))
            Link(
                "https://github.com/ritwik-srivastava",
                "GitHub",
                Modifier.setVariable(ColorVar, sitePalette.brand.primary),
                variant = UncoloredLinkVariant,
            )
        }

        SpanText(
            "Built with Kotlin & Kobweb | Ritwik Raj Srivastava",
            Modifier
                .fontSize(0.85.cssRem)
                .color(sitePalette.textSecondary)
                .textAlign(TextAlign.Center)
        )
    }
}
