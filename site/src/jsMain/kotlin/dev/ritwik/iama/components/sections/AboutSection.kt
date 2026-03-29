package dev.ritwik.iama.components.sections

import androidx.compose.runtime.Composable
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
fun AboutSection() {
    ScrollReveal {
        Column(SectionStyle.toModifier().id("about")) {
            SectionHeader("About Me")

            SpanText(
                "A highly dedicated and proactive professional with 6+ years of experience in " +
                "designing, coding, and leading mobile applications at scale. Currently serving as " +
                "Tech Lead for MyJio, one of India's most-used Android applications. Passionate about " +
                "building robust design systems, optimizing developer experience, and delivering " +
                "products that impact millions of users.",
                Modifier
                    .fontSize(1.1.cssRem)
                    .lineHeight(1.8)
                    .color(ColorMode.current.toSitePalette().textSecondary)
            )
        }
    }
}
