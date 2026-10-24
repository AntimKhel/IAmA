package dev.ritwik.iama

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.minHeight
import com.varabyte.kobweb.core.App
import com.varabyte.kobweb.silk.SilkApp
import com.varabyte.kobweb.silk.components.layout.Surface
import com.varabyte.kobweb.silk.init.InitSilk
import com.varabyte.kobweb.silk.init.InitSilkContext
import com.varabyte.kobweb.silk.style.common.SmoothColorStyle
import com.varabyte.kobweb.silk.style.toModifier
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import com.varabyte.kobweb.silk.theme.colors.loadFromLocalStorage
import com.varabyte.kobweb.silk.theme.colors.saveToLocalStorage
import com.varabyte.kobweb.silk.theme.colors.systemPreference
import org.jetbrains.compose.web.css.vh

// Fonts, meta tags and the motion stylesheet are added to <head> at build time (see site/build.gradle.kts).

private const val COLOR_MODE_KEY = "iama:colorMode"

@InitSilk
fun initColorMode(ctx: InitSilkContext) {
    // An explicit choice (nav toggle) wins; otherwise follow the visitor's OS setting.
    ctx.config.initialColorMode = ColorMode.loadFromLocalStorage(COLOR_MODE_KEY) ?: ColorMode.systemPreference
}

@App
@Composable
fun AppEntry(content: @Composable () -> Unit) {
    SilkApp {
        val colorMode = ColorMode.current
        // Only persist once the visitor has actually toggled, so the OS preference keeps applying until then.
        var initialMode by remember { mutableStateOf<ColorMode?>(colorMode) }
        LaunchedEffect(colorMode) {
            if (initialMode != colorMode) {
                initialMode = null
                colorMode.saveToLocalStorage(COLOR_MODE_KEY)
            }
        }

        Surface(SmoothColorStyle.toModifier().minHeight(100.vh)) {
            content()
        }
    }
}
