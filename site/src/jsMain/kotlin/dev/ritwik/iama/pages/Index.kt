package dev.ritwik.iama.pages

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.data.add
import com.varabyte.kobweb.core.init.InitRoute
import com.varabyte.kobweb.core.init.InitRouteContext
import com.varabyte.kobweb.core.layout.Layout
import dev.ritwik.iama.components.hero.HeroScene
import dev.ritwik.iama.components.lab.StyleLabPanel
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
    HeroScene()
    ProductsSection()
    ImpactSection()
    ExperienceSection()
    RoadmapSection()
    SkillsSection()
    BlogTeaserSection()
    ContactSection()
    StyleLabPanel()
}
