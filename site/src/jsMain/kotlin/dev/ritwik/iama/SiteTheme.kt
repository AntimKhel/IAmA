package dev.ritwik.iama

import com.varabyte.kobweb.compose.ui.graphics.Color
import com.varabyte.kobweb.silk.init.InitSilk
import com.varabyte.kobweb.silk.init.InitSilkContext
import com.varabyte.kobweb.silk.theme.colors.palette.background
import com.varabyte.kobweb.silk.theme.colors.palette.color

/** Page background/text per color mode; must match --bg / --text in rr.css. */
@InitSilk
fun initTheme(ctx: InitSilkContext) {
    ctx.theme.palettes.light.background = Color.rgb(0xF4F6FB)
    ctx.theme.palettes.light.color = Color.rgb(0x0F141C)
    ctx.theme.palettes.dark.background = Color.rgb(0x07090D)
    ctx.theme.palettes.dark.color = Color.rgb(0xE9EEF4)
}
