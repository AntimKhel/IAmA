package dev.ritwik.iama

import com.varabyte.kobweb.compose.ui.graphics.Color
import com.varabyte.kobweb.silk.init.InitSilk
import com.varabyte.kobweb.silk.init.InitSilkContext
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import com.varabyte.kobweb.silk.theme.colors.palette.background
import com.varabyte.kobweb.silk.theme.colors.palette.color

class SitePalette(
    val nearBackground: Color,
    val brand: Brand,
    val textSecondary: Color,
    val border: Color,
    val codeBg: Color,
) {
    class Brand(
        val primary: Color,
        val primaryVariant: Color,
        val accent: Color,
    )
}

object SitePalettes {
    val light = SitePalette(
        nearBackground = Color.rgb(0xF0F4F0),
        brand = SitePalette.Brand(
            primary = Color.rgb(0x1B8A2E),
            primaryVariant = Color.rgb(0x0E6B1F),
            accent = Color.rgb(0x0969DA),
        ),
        textSecondary = Color.rgb(0x656D76),
        border = Color.rgb(0xD0D7DE),
        codeBg = Color.rgb(0xF6F8FA),
    )
    val dark = SitePalette(
        nearBackground = Color.rgb(0x161B22),
        brand = SitePalette.Brand(
            primary = Color.rgb(0x3DDC84),
            primaryVariant = Color.rgb(0x2ECC71),
            accent = Color.rgb(0x58A6FF),
        ),
        textSecondary = Color.rgb(0x8B949E),
        border = Color.rgb(0x30363D),
        codeBg = Color.rgb(0x1C2128),
    )
}

fun ColorMode.toSitePalette(): SitePalette {
    return when (this) {
        ColorMode.LIGHT -> SitePalettes.light
        ColorMode.DARK -> SitePalettes.dark
    }
}

@InitSilk
fun initTheme(ctx: InitSilkContext) {
    ctx.theme.palettes.light.background = Color.rgb(0xF5F7F5)
    ctx.theme.palettes.light.color = Color.rgb(0x1F2328)
    ctx.theme.palettes.dark.background = Color.rgb(0x0D1117)
    ctx.theme.palettes.dark.color = Color.rgb(0xE6EDF3)
}
