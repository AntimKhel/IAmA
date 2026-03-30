package dev.ritwik.iama.components.sections

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.css.*
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.silk.components.text.SpanText
import com.varabyte.kobweb.silk.style.breakpoint.Breakpoint
import com.varabyte.kobweb.silk.style.toModifier
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import org.jetbrains.compose.web.css.*
import dev.ritwik.iama.*
import dev.ritwik.iama.components.widgets.ScrollReveal
import dev.ritwik.iama.components.widgets.SectionHeader

@Composable
fun SkillsSection() {
    Column(SectionStyle.toModifier().id("skills")) {
        ScrollReveal {
            SectionHeader("Skills & Education")
        }

        Row(
            Modifier
                .fillMaxWidth()
                .gap(3.cssRem)
                .flexWrap(FlexWrap.Wrap)
        ) {
            Column(Modifier.flexGrow(1).flexBasis(0.px).minWidth(20.cssRem).gap(1.5.cssRem)) {
                skillCategories.forEachIndexed { catIndex, category ->
                    ScrollReveal(
                        keyframes = FadeInUp,
                        delay = (catIndex * 100).ms,
                    ) {
                        SkillCategoryRow(category)
                    }
                }
            }

            Column(Modifier.flexBasis(0.px).minWidth(16.cssRem).gap(1.5.cssRem)) {
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
    }
}

@Composable
private fun SkillCategoryRow(category: SkillCategory) {
    val sitePalette = ColorMode.current.toSitePalette()

    Row(Modifier.fillMaxWidth().gap(0.75.cssRem).flexWrap(FlexWrap.Wrap).alignItems(org.jetbrains.compose.web.css.AlignItems.Center)) {
        SpanText(
            category.name,
            Modifier
                .fontSize(1.05.cssRem)
                .fontWeight(FontWeight.SemiBold)
                .color(sitePalette.brand.primary)
        )
        category.skills.forEach { skill ->
            SpanText(skill, ChipStyle.toModifier())
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
            education.score,
            Modifier.fontSize(0.95.cssRem).fontWeight(FontWeight.Medium)
        )
    }
}
