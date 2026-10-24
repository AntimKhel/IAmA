package dev.ritwik.iama

import com.varabyte.kobweb.compose.css.ScrollBehavior
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.silk.components.layout.HorizontalDividerStyle
import com.varabyte.kobweb.silk.init.InitSilk
import com.varabyte.kobweb.silk.init.InitSilkContext
import com.varabyte.kobweb.silk.init.registerStyleBase
import com.varabyte.kobweb.silk.theme.modifyStyleBase
import org.jetbrains.compose.web.css.*

// Global base styles. Everything visual lives in resources/public/css (rr.css, sections.css, blog.css),
// driven by the theme tokens there; Silk only provides the color mode and page background.

@InitSilk
fun initSiteStyles(ctx: InitSilkContext) {
    ctx.stylesheet.apply {
        registerStyle("html") {
            cssRule(CSSMediaQuery.MediaFeature("prefers-reduced-motion", StylePropertyValue("no-preference"))) {
                Modifier.scrollBehavior(ScrollBehavior.Smooth)
            }
        }
        registerStyleBase("body") {
            Modifier
                .fontFamily("Inter", "system-ui", "-apple-system", "BlinkMacSystemFont", "Segoe UI", "Roboto", "sans-serif")
                .fontSize(18.px)
                .lineHeight(1.5)
        }
    }
    ctx.theme.modifyStyleBase(HorizontalDividerStyle) { Modifier.fillMaxWidth() }
}
