package dev.ritwik.iama.components.sections

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
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
fun SkillsSection() {
    Column(SectionStyle.toModifier().id("skills")) {
        ScrollReveal {
            SectionHeader("Skills")
        }

        Column(Modifier.fillMaxWidth().gap(2.cssRem)) {
            skillCategories.forEachIndexed { catIndex, category ->
                ScrollReveal(
                    keyframes = FadeInUp,
                    delay = (catIndex * 100).ms,
                ) {
                    SkillCategoryRow(category, catIndex)
                }
            }
        }
    }
}

@Composable
private fun SkillCategoryRow(category: SkillCategory, catIndex: Int) {
    val sitePalette = ColorMode.current.toSitePalette()

    Column(Modifier.fillMaxWidth().gap(0.75.cssRem)) {
        SpanText(
            category.name,
            Modifier
                .fontSize(1.05.cssRem)
                .fontWeight(FontWeight.SemiBold)
                .color(sitePalette.brand.primary)
        )
        Row(Modifier.flexWrap(FlexWrap.Wrap).gap(0.6.cssRem)) {
            category.skills.forEachIndexed { skillIndex, skill ->
                SpanText(skill, ChipStyle.toModifier())
            }
        }
    }
}
