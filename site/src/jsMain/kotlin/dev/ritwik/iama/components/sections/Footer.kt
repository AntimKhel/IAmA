package dev.ritwik.iama.components.sections

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.css.TextAlign
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.silk.components.icons.fa.FaGithub
import com.varabyte.kobweb.silk.components.icons.fa.FaLinkedin
import com.varabyte.kobweb.silk.components.icons.fa.FaTwitter
import com.varabyte.kobweb.silk.components.icons.fa.FaYoutube
import com.varabyte.kobweb.silk.components.icons.fa.IconSize
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
        Row(
            Modifier.gap(1.5.cssRem).margin(bottom = 1.cssRem),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Link(
                "https://twitter.com",
                Modifier.setVariable(ColorVar, sitePalette.textSecondary),
                variant = UncoloredLinkVariant,
            ) { FaTwitter(size = IconSize.LG) }

            Link(
                "https://www.linkedin.com/in/ritwik787/",
                Modifier.setVariable(ColorVar, sitePalette.textSecondary),
                variant = UncoloredLinkVariant,
            ) { FaLinkedin(size = IconSize.LG) }

            Link(
                "https://github.com/ritwik-srivastava",
                Modifier.setVariable(ColorVar, sitePalette.textSecondary),
                variant = UncoloredLinkVariant,
            ) { FaGithub(size = IconSize.LG) }

            Link(
                "https://youtube.com",
                Modifier.setVariable(ColorVar, sitePalette.textSecondary),
                variant = UncoloredLinkVariant,
            ) { FaYoutube(size = IconSize.LG) }
        }

        SpanText(
            "\u00a9 2026 Ritwik Raj Srivastava \u00b7 Built with Kotlin & Kobweb",
            Modifier
                .fontSize(0.85.cssRem)
                .color(sitePalette.textSecondary)
                .textAlign(TextAlign.Center)
        )
    }
}
