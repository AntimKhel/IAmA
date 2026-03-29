package dev.ritwik.iama.components.sections

import androidx.compose.runtime.*
import com.varabyte.kobweb.browser.dom.ElementTarget
import com.varabyte.kobweb.compose.css.*
import com.varabyte.kobweb.compose.css.functions.clamp
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.foundation.layout.Spacer
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.graphics.Colors
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.compose.css.UserSelect
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.silk.components.icons.CloseIcon
import com.varabyte.kobweb.silk.components.icons.HamburgerIcon
import com.varabyte.kobweb.silk.components.icons.MoonIcon
import com.varabyte.kobweb.silk.components.icons.SunIcon
import com.varabyte.kobweb.silk.components.navigation.Link
import com.varabyte.kobweb.silk.components.navigation.UncoloredLinkVariant
import com.varabyte.kobweb.silk.components.navigation.UndecoratedLinkVariant
import com.varabyte.kobweb.silk.components.overlay.Overlay
import com.varabyte.kobweb.silk.components.overlay.OverlayVars
import com.varabyte.kobweb.silk.components.overlay.PopupPlacement
import com.varabyte.kobweb.silk.components.overlay.Tooltip
import com.varabyte.kobweb.silk.components.text.SpanText
import com.varabyte.kobweb.silk.style.CssStyle
import com.varabyte.kobweb.silk.style.animation.Keyframes
import com.varabyte.kobweb.silk.style.animation.toAnimation
import com.varabyte.kobweb.silk.style.base
import com.varabyte.kobweb.silk.style.breakpoint.Breakpoint
import com.varabyte.kobweb.silk.style.breakpoint.displayIfAtLeast
import com.varabyte.kobweb.silk.style.breakpoint.displayUntil
import com.varabyte.kobweb.silk.style.toModifier
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import dev.ritwik.iama.components.widgets.IconButton
import dev.ritwik.iama.toSitePalette

val NavHeaderStyle = CssStyle.base {
    Modifier
        .fillMaxWidth()
        .padding(topBottom = 1.cssRem, leftRight = 2.cssRem)
        .position(Position.Fixed)
        .top(0.px)
        .zIndex(100)
        .transition(
            Transition.of("background-color", 300.ms, TransitionTimingFunction.EaseOut),
            Transition.of("box-shadow", 300.ms, TransitionTimingFunction.EaseOut),
            Transition.of("padding", 300.ms, TransitionTimingFunction.EaseOut),
        )
}

// Scroll progress bar at the very top
val ScrollProgressStyle = CssStyle.base {
    Modifier
        .position(Position.Fixed)
        .top(0.px)
        .left(0.px)
        .height(3.px)
        .zIndex(101)
        .background(colorMode.toSitePalette().brand.primary)
        .transition(Transition.of("width", 50.ms, TransitionTimingFunction.Linear))
}

// Navigate to section: scroll if on home page, otherwise route home first
private fun navigateToSection(id: String, router: com.varabyte.kobweb.navigation.Router) {
    val el = document.getElementById(id)
    if (el != null) {
        el.asDynamic().scrollIntoView(js("({behavior: 'smooth', block: 'start'})"))
    } else {
        router.tryRoutingTo("/#$id")
    }
}

@Composable
private fun MenuItems(onNavigate: () -> Unit = {}) {
    val router = com.varabyte.kobweb.core.rememberPageContext().router
    val navMod = Modifier.cursor(Cursor.Pointer).userSelect(UserSelect.None)

    listOf("about" to "About", "experience" to "Experience", "skills" to "Skills", "education" to "Education", "certifications" to "Certs").forEach { (id, label) ->
        Span(navMod.onClick { navigateToSection(id, router); onNavigate() }.toAttrs()) { Text(label) }
    }
    Link("/blog", "Blog", variant = UndecoratedLinkVariant.then(UncoloredLinkVariant))
}

@Composable
private fun ColorModeButton() {
    var colorMode by ColorMode.currentState
    IconButton(onClick = { colorMode = colorMode.opposite }) {
        if (colorMode.isLight) MoonIcon() else SunIcon()
    }
    Tooltip(ElementTarget.PreviousSibling, "Toggle color mode", placement = PopupPlacement.BottomRight)
}

@Composable
private fun HamburgerButton(onClick: () -> Unit) {
    IconButton(onClick) { HamburgerIcon() }
}

@Composable
private fun CloseButton(onClick: () -> Unit) {
    IconButton(onClick) { CloseIcon() }
}

val SideMenuSlideInAnim = Keyframes {
    from { Modifier.translateX(100.percent) }
    to { Modifier }
}

enum class SideMenuState {
    CLOSED, OPEN, CLOSING;
    fun close() = when (this) {
        CLOSED -> CLOSED
        OPEN -> CLOSING
        CLOSING -> CLOSING
    }
}

@Composable
fun NavHeader() {
    var scrolled by remember { mutableStateOf(false) }
    var scrollProgress by remember { mutableStateOf(0.0) }
    val sitePalette = ColorMode.current.toSitePalette()

    LaunchedEffect(Unit) {
        window.addEventListener("scroll", {
            scrolled = window.scrollY > 100.0
            val docHeight = document.documentElement?.scrollHeight?.toDouble() ?: 1.0
            val winHeight = window.innerHeight.toDouble()
            val scrollTop = window.scrollY
            scrollProgress = if (docHeight > winHeight) {
                (scrollTop / (docHeight - winHeight)).coerceIn(0.0, 1.0) * 100.0
            } else 0.0
        })
    }

    // Scroll progress bar
    Box(ScrollProgressStyle.toModifier().width(scrollProgress.percent))

    val scrolledModifier = if (scrolled) {
        Modifier
            .backgroundColor(sitePalette.nearBackground)
            .boxShadow(
                offsetY = 2.px,
                blurRadius = 10.px,
                color = com.varabyte.kobweb.compose.ui.graphics.Color.rgba(0, 0, 0, 0.1f)
            )
            .padding(topBottom = 0.6.cssRem, leftRight = 2.cssRem)
    } else Modifier

    Row(
        NavHeaderStyle.toModifier().then(scrolledModifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // RRS logo - scrolls to top
        Span(
            Modifier
                .fontSize(1.4.cssRem)
                .fontWeight(FontWeight.Bold)
                .color(sitePalette.brand.primary)
                .cursor(Cursor.Pointer)
                .onClick { window.scrollTo(0.0, 0.0) }
                .toAttrs()
        ) {
            Text("RRS")
        }

        Spacer()

        Row(
            Modifier.gap(1.5.cssRem).displayIfAtLeast(Breakpoint.MD),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MenuItems()
            ColorModeButton()
        }

        Row(
            Modifier
                .fontSize(1.5.cssRem)
                .gap(1.cssRem)
                .displayUntil(Breakpoint.MD),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var menuState by remember { mutableStateOf(SideMenuState.CLOSED) }

            ColorModeButton()
            HamburgerButton(onClick = { menuState = SideMenuState.OPEN })

            if (menuState != SideMenuState.CLOSED) {
                SideMenu(
                    menuState,
                    close = { menuState = menuState.close() },
                    onAnimationEnd = { if (menuState == SideMenuState.CLOSING) menuState = SideMenuState.CLOSED }
                )
            }
        }
    }
}

@Composable
private fun SideMenu(menuState: SideMenuState, close: () -> Unit, onAnimationEnd: () -> Unit) {
    Overlay(
        Modifier
            .setVariable(OverlayVars.BackgroundColor, Colors.Transparent)
            .onClick { close() }
    ) {
        key(menuState) {
            Column(
                Modifier
                    .fillMaxHeight()
                    .width(clamp(8.cssRem, 33.percent, 10.cssRem))
                    .align(Alignment.CenterEnd)
                    .padding(top = 1.cssRem, leftRight = 1.cssRem)
                    .gap(1.5.cssRem)
                    .backgroundColor(ColorMode.current.toSitePalette().nearBackground)
                    .animation(
                        SideMenuSlideInAnim.toAnimation(
                            duration = 200.ms,
                            timingFunction = if (menuState == SideMenuState.OPEN) AnimationTimingFunction.EaseOut else AnimationTimingFunction.EaseIn,
                            direction = if (menuState == SideMenuState.OPEN) AnimationDirection.Normal else AnimationDirection.Reverse,
                            fillMode = AnimationFillMode.Forwards
                        )
                    )
                    .borderRadius(topLeft = 2.cssRem)
                    .onClick { it.stopPropagation() }
                    .onAnimationEnd { onAnimationEnd() },
                horizontalAlignment = Alignment.End
            ) {
                CloseButton(onClick = { close() })
                Column(
                    Modifier.padding(right = 0.75.cssRem).gap(1.5.cssRem).fontSize(1.4.cssRem),
                    horizontalAlignment = Alignment.End
                ) {
                    MenuItems(onNavigate = { close() })
                }
            }
        }
    }
}
