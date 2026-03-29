package dev.ritwik.iama

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.browser.document
import org.jetbrains.compose.web.css.vh
import org.w3c.dom.HTMLLinkElement
import org.w3c.dom.HTMLStyleElement

private const val COLOR_MODE_KEY = "iama:colorMode"

@InitSilk
fun initColorMode(ctx: InitSilkContext) {
    ctx.config.initialColorMode = ColorMode.loadFromLocalStorage(COLOR_MODE_KEY) ?: ColorMode.DARK
}

@App
@Composable
fun AppEntry(content: @Composable () -> Unit) {
    SilkApp {
        val colorMode = ColorMode.current
        LaunchedEffect(colorMode) {
            colorMode.saveToLocalStorage(COLOR_MODE_KEY)
        }

        LaunchedEffect(Unit) {
            loadGoogleFonts()
            injectBlobAnimations()
        }

        Surface(SmoothColorStyle.toModifier().minHeight(100.vh)) {
            content()
        }
    }
}

private fun loadGoogleFonts() {
    val head = document.head ?: return

    val preconnect = (document.createElement("link") as HTMLLinkElement).apply {
        rel = "preconnect"
        href = "https://fonts.googleapis.com"
    }
    head.appendChild(preconnect)

    val fontLink = (document.createElement("link") as HTMLLinkElement).apply {
        rel = "stylesheet"
        href = "https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap"
    }
    head.appendChild(fontLink)
}

private fun injectBlobAnimations() {
    val head = document.head ?: return
    val style = (document.createElement("style") as HTMLStyleElement)
    style.textContent = """
        @keyframes blob-float-1 {
            0% { transform: translate(0, 0); }
            25% { transform: translate(5%, -10%); }
            50% { transform: translate(-5%, 5%); }
            75% { transform: translate(10%, -5%); }
            100% { transform: translate(0, 0); }
        }
        @keyframes blob-float-2 {
            0% { transform: translate(0, 0); }
            25% { transform: translate(-8%, 6%); }
            50% { transform: translate(4%, -8%); }
            75% { transform: translate(-6%, 3%); }
            100% { transform: translate(0, 0); }
        }
        @keyframes blob-float-3 {
            0% { transform: translate(0, 0) rotate(0deg); }
            33% { transform: translate(7%, -5%) rotate(5deg); }
            66% { transform: translate(-3%, 8%) rotate(-3deg); }
            100% { transform: translate(0, 0) rotate(0deg); }
        }
        @keyframes glow-pulse {
            0% { opacity: 0.4; }
            50% { opacity: 0.7; }
            100% { opacity: 0.4; }
        }
        @keyframes dot-pulse {
            0% { transform: scale(1); box-shadow: 0 0 12px rgba(61, 220, 132, 0.6); }
            50% { transform: scale(1.1); box-shadow: 0 0 20px rgba(61, 220, 132, 0.8); }
            100% { transform: scale(1); box-shadow: 0 0 12px rgba(61, 220, 132, 0.6); }
        }
    """.trimIndent()
    head.appendChild(style)
}
