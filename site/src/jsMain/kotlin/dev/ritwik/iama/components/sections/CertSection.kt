package dev.ritwik.iama.components.sections

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.css.*
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.silk.components.text.SpanText
import com.varabyte.kobweb.silk.style.toModifier
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Li
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Ul
import dev.ritwik.iama.*
import dev.ritwik.iama.components.widgets.ScrollReveal
import dev.ritwik.iama.components.widgets.SectionHeader

@Composable
fun CertSection() {
    Column(SectionStyle.toModifier().id("certifications")) {
        ScrollReveal {
            SectionHeader("Certifications & Awards")
        }

        Row(Modifier.fillMaxWidth().flexWrap(FlexWrap.Wrap).gap(1.5.cssRem)) {
            certifications.forEachIndexed { index, cert ->
                ScrollReveal(
                    keyframes = PopIn,
                    delay = (index * 100).ms,
                ) {
                    CertCard(cert)
                }
            }
        }

        ScrollReveal(
            keyframes = FadeInUp,
            delay = 300.ms,
            modifier = Modifier.margin(top = 2.5.cssRem),
        ) {
            Column(Modifier.fillMaxWidth().gap(0.75.cssRem)) {
                SpanText(
                    "Awards",
                    Modifier
                        .fontSize(1.3.cssRem)
                        .fontWeight(FontWeight.SemiBold)
                        .color(ColorMode.current.toSitePalette().brand.primary)
                )
                Ul(Modifier.padding(left = 1.2.cssRem).margin(top = 0.25.cssRem).toAttrs()) {
                    awards.forEach { award ->
                        Li(Modifier.margin(bottom = 0.4.cssRem).lineHeight(1.6).toAttrs()) {
                            Text(award)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CertCard(cert: Certification) {
    val sitePalette = ColorMode.current.toSitePalette()

    Column(
        CardStyle.toModifier()
            .width(16.cssRem)
            .gap(0.4.cssRem)
    ) {
        SpanText(
            cert.name,
            Modifier.fontSize(1.05.cssRem).fontWeight(FontWeight.SemiBold)
        )
        SpanText(
            cert.issuer,
            Modifier.fontSize(0.9.cssRem).color(sitePalette.textSecondary)
        )
        SpanText(
            cert.year,
            ChipStyle.toModifier().margin(top = 0.5.cssRem)
        )
    }
}
