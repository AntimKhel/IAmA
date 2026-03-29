package dev.ritwik.iama.components.sections

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.silk.components.text.SpanText
import com.varabyte.kobweb.silk.style.toModifier
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import org.jetbrains.compose.web.css.*
import dev.ritwik.iama.*
import dev.ritwik.iama.components.widgets.ScrollReveal
import dev.ritwik.iama.components.widgets.SectionHeader

@Composable
fun EducationSection() {
    Column(SectionStyle.toModifier().id("education")) {
        ScrollReveal {
            SectionHeader("Education")
        }

        educationList.forEachIndexed { index, edu ->
            ScrollReveal(
                keyframes = FadeInUp,
                delay = (index * 150).ms,
            ) {
                EducationCard(edu)
            }
        }
    }
}

@Composable
private fun EducationCard(education: Education) {
    val sitePalette = ColorMode.current.toSitePalette()

    Column(
        CardStyle.toModifier()
            .fillMaxWidth()
            .gap(0.5.cssRem)
    ) {
        SpanText(
            education.institution,
            Modifier
                .fontSize(1.3.cssRem)
                .fontWeight(FontWeight.SemiBold)
                .color(sitePalette.brand.primary)
        )
        SpanText(
            education.degree,
            Modifier.fontSize(1.05.cssRem).fontWeight(FontWeight.Medium)
        )
        SpanText(
            education.period,
            Modifier.fontSize(0.9.cssRem).color(sitePalette.textSecondary)
        )
        SpanText(
            education.score,
            Modifier.fontSize(0.95.cssRem).fontWeight(FontWeight.Medium)
        )
    }
}
