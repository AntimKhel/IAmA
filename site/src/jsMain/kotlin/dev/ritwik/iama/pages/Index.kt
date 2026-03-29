package dev.ritwik.iama.pages

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.data.add
import com.varabyte.kobweb.core.init.InitRoute
import com.varabyte.kobweb.core.init.InitRouteContext
import com.varabyte.kobweb.core.layout.Layout
import com.varabyte.kobweb.silk.style.toModifier
import dev.ritwik.iama.AltSectionWrapperStyle
import dev.ritwik.iama.components.layouts.PageLayoutData
import dev.ritwik.iama.components.sections.*

@InitRoute
fun initHomePage(ctx: InitRouteContext) {
    ctx.data.add(PageLayoutData("Home"))
}

@Page
@Layout(".components.layouts.PageLayout")
@Composable
fun HomePage() {
    HeroSection()
    AboutSection()
    Box(AltSectionWrapperStyle.toModifier().fillMaxWidth()) {
        ExperienceSection()
    }
    SkillsSection()
    Box(AltSectionWrapperStyle.toModifier().fillMaxWidth()) {
        EducationSection()
    }
    CertSection()
}
