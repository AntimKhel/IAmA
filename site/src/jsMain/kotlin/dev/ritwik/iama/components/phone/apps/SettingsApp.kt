package dev.ritwik.iama.components.phone.apps

import androidx.compose.runtime.*
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import dev.ritwik.iama.components.devmode.DevMode
import dev.ritwik.iama.components.devtools.DevOptions
import dev.ritwik.iama.components.devtools.scaleLabel
import dev.ritwik.iama.components.phone.AppBar
import dev.ritwik.iama.components.phone.Apps
import dev.ritwik.iama.components.phone.PhoneState
import dev.ritwik.iama.components.theme.DynamicColor
import dev.ritwik.iama.components.theme.Palette
import dev.ritwik.iama.components.theme.extractSeeds
import dev.ritwik.iama.data.Profile
import dev.ritwik.iama.util.Logcat
import dev.ritwik.iama.util.UiState
import dev.ritwik.iama.util.haptic
import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import org.w3c.files.get

private enum class SettingsPage { ROOT, WALLPAPER, DEVELOPER, ABOUT }

/** Android's Settings, cut down to what this phone has: Material You, dark theme, Developer options, About phone. */
@Composable
fun SettingsApp() {
    // Keyed on the deep link, so re-opening Settings from inside Settings (the build-number unlock) lands there.
    var page by remember(PhoneState.payload) {
        mutableStateOf(
            when (PhoneState.payload) {
                "developer" -> if (DevOptions.unlocked) SettingsPage.DEVELOPER else SettingsPage.ABOUT
                "wallpaper" -> SettingsPage.WALLPAPER
                "about" -> SettingsPage.ABOUT
                else -> SettingsPage.ROOT
            }
        )
    }
    val toRoot = { page = SettingsPage.ROOT }
    when (page) {
        SettingsPage.ROOT -> SettingsRoot { page = it }
        SettingsPage.WALLPAPER -> WallpaperPage(toRoot)
        SettingsPage.DEVELOPER -> DeveloperPage(toRoot)
        SettingsPage.ABOUT -> AboutPhonePage(toRoot)
    }
}

@Composable
private fun SettingsRoot(open: (SettingsPage) -> Unit) {
    var colorMode by ColorMode.currentState
    AppBar("Settings")
    Div({ classes("rr-a-scroll", "st") }) {
        Div({ classes("st-group") }) {
            NavRow("fa-solid fa-palette", "#7c4dff", "Wallpaper & style", "Colors, photo wallpaper, themed icons") { open(SettingsPage.WALLPAPER) }
            SwitchRow("fa-solid fa-moon", "#2f6bff", "Dark theme", "Use a dark background across the site", colorMode.isDark) {
                colorMode = colorMode.opposite
                Logcat.i("UiModeManager", "Night mode: ${if (colorMode.isDark) "yes" else "no"}")
            }
        }
        Div({ classes("st-group") }) {
            if (DevOptions.unlocked) {
                NavRow("fa-solid fa-code", "#3ddc84", "Developer options", if (DevOptions.enabled) "On" else "Off") { open(SettingsPage.DEVELOPER) }
            }
            NavRow("fa-solid fa-circle-info", "#00bfa5", "About phone", "Ritwik's Pixel") { open(SettingsPage.ABOUT) }
        }
        if (!DevOptions.unlocked) {
            P({ classes("st-foot") }) { Text("Psst: Android developers know what to do in About phone.") }
        }
    }
}

// ---------------------------------------------------------------------------
// Wallpaper & style (Material You)
// ---------------------------------------------------------------------------

@Composable
private fun WallpaperPage(onBack: () -> Unit) {
    var busy by remember { mutableStateOf(false) }
    AppBar("Wallpaper & style", onBack = onBack)
    Div({ classes("rr-a-scroll", "st") }) {
        StylePreview()

        if (DynamicColor.photo != null && DynamicColor.photoSeeds.isNotEmpty()) {
            Heading("Wallpaper colors")
            Div({ classes("st-swatches") }) {
                DynamicColor.photoSeeds.forEach { seed -> Swatch(seed, "From your wallpaper") }
            }
        }
        Heading("Basic colors")
        Div({ classes("st-swatches") }) {
            DynamicColor.basicColors.forEach { s -> Swatch(s.seed, s.name) }
        }

        Heading("Wallpaper")
        Div({ classes("st-group") }) {
            Label(attrs = { classes("st-row", "st-file") }) {
                RowIcon("fa-solid fa-image", "#ff8a3d")
                Div({ classes("st-text") }) {
                    Div({ classes("st-title") }) { Text(if (busy) "Reading your photo…" else "Use a photo") }
                    Div({ classes("st-sub") }) { Text("Stays on this device; its colours theme the site") }
                }
                Input(InputType.File) {
                    attr("accept", "image/*")
                    onChange { e ->
                        val file = (e.target as HTMLInputElement).files?.get(0) ?: return@onChange
                        busy = true
                        loadWallpaper(file) { busy = false }
                    }
                }
            }
            if (DynamicColor.photo != null) {
                NavRow("fa-solid fa-xmark", "#5f6b7a", "Remove photo", "Back to the generated wallpaper") { DynamicColor.setPhoto(null, emptyList()) }
            }
            SwitchRow("fa-solid fa-shapes", "#00bfa5", "Themed icons", "App icons pick up your colours", DynamicColor.themedIcons) {
                DynamicColor.setThemedIcons(!DynamicColor.themedIcons)
            }
        }
        Div({ classes("rr-a-actions") }) {
            Button({ classes("rr-a-install", "ghost"); onClick { DynamicColor.reset(); UiState.showToast("Back to the default style") } }) {
                I({ classes("fa-solid", "fa-rotate-left") }); Text("Reset to default")
            }
        }
    }
}

/** A tiny phone showing the wallpaper, themed icons and the palette. */
@Composable
private fun StylePreview() {
    val p = Palette.from(DynamicColor.activeSeed)
    Div({ classes("st-preview") }) {
        Div({ classes("st-mini") }) {
            Div({ classes("rr-wall") })
            Div({ classes("st-mini-clock") }) { Text("12:30") }
            Div({ classes("st-mini-icons") }) {
                listOf(Apps.work, Apps.products, Apps.blog, Apps.ask).forEach { app ->
                    Div({ classes("rr-app", "st-mini-app") }) {
                        Div({
                            classes("ico")
                            style { property("--a", app.from); property("--b", app.to) }
                        }) { I({ classes(*app.icon.split(" ").toTypedArray()) }) }
                    }
                }
            }
        }
        Div({ classes("st-palette") }) {
            listOfNotNull(p?.g1, p?.g2, p?.g4, p?.g3).forEachIndexed { i, c ->
                Div({ classes("st-chip"); style { property("background", c) } }) {
                    Span { Text(listOf("Primary", "Secondary", "Tertiary", "Accent")[i]) }
                }
            }
        }
    }
}

@Composable
private fun Swatch(seed: String, name: String) {
    val p = Palette.from(seed) ?: return
    val on = DynamicColor.activeSeed.equals(seed, ignoreCase = true)
    Button({
        classes("st-swatch")
        if (on) classes("on")
        attr("aria-label", name)
        attr("aria-pressed", on.toString())
        attr("title", name)
        style {
            property("--s1", p.g1)
            property("--s2", p.g2)
            property("--s3", p.g4)
        }
        onClick { haptic(); DynamicColor.setSeed(seed) }
    }) { if (on) I({ classes("fa-solid", "fa-check") }) }
}

/** Downscales the photo (so it fits in local storage), finds its colours, and applies both. */
private fun loadWallpaper(file: org.w3c.files.File, done: () -> Unit) {
    val reader = FileReader()
    reader.onload = {
        val img = document.createElement("img") as HTMLImageElement
        img.onload = {
            val scale = minOf(1.0, 900.0 / maxOf(img.naturalWidth, img.naturalHeight))
            val canvas = document.createElement("canvas") as HTMLCanvasElement
            canvas.width = (img.naturalWidth * scale).toInt().coerceAtLeast(1)
            canvas.height = (img.naturalHeight * scale).toInt().coerceAtLeast(1)
            val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
            ctx.drawImage(img, 0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble())
            val dataUrl = canvas.toDataURL("image/jpeg", 0.82)
            // Colours from a small copy, like Monet's quantizer would.
            val small = document.createElement("canvas") as HTMLCanvasElement
            small.width = 64; small.height = 64
            val sctx = small.getContext("2d") as CanvasRenderingContext2D
            sctx.drawImage(img, 0.0, 0.0, 64.0, 64.0)
            val seeds = extractSeeds(sctx.getImageData(0.0, 0.0, 64.0, 64.0).data.asDynamic())
            Logcat.i("Monet", "Wallpaper ${canvas.width}×${canvas.height}, ${seeds.size} colours: ${seeds.joinToString()}")
            DynamicColor.setPhoto(dataUrl, seeds)
            done()
        }
        img.onerror = { _, _, _, _, _ -> UiState.showToast("Couldn't read that image"); done() }
        img.src = reader.result as String
    }
    reader.readAsDataURL(file)
}

// ---------------------------------------------------------------------------
// Developer options
// ---------------------------------------------------------------------------

private enum class ScaleKind(val title: String) { WINDOW("Window animation scale"), TRANSITION("Transition animation scale"), ANIMATOR("Animator duration scale") }

@Composable
private fun DeveloperPage(onBack: () -> Unit) {
    var choosing by remember { mutableStateOf<ScaleKind?>(null) }
    val on = DevOptions.enabled
    AppBar("Developer options", onBack = onBack)
    Div({ classes("rr-a-scroll", "st") }) {
        Div({
            classes("st-master")
            if (on) classes("on")
            onClick { DevOptions.setEnabled(!on) }
        }) {
            Span { Text("Use developer options") }
            Toggle(on)
        }

        Div({ classes("st-dev"); if (!on) classes("off") }) {
            Heading("Debugging")
            Div({ classes("st-group") }) {
                NavRow("fa-solid fa-bug", "#3ddc84", "Logcat", "This site's own log, live", enabled = on) {
                    PhoneState.open(Apps.logcat, PhoneState.host, null)
                }
                SwitchRow("fa-solid fa-triangle-exclamation", "#ff5252", "Strict mode enabled", "Flash screen when the main thread is blocked", DevOptions.strictMode, on) {
                    DevOptions.setStrictMode(!DevOptions.strictMode)
                }
            }
            Heading("Input")
            Div({ classes("st-group") }) {
                SwitchRow("fa-solid fa-hand-pointer", "#2f6bff", "Show taps", "Show visual feedback for taps", DevOptions.showTaps, on) {
                    DevOptions.setShowTaps(!DevOptions.showTaps)
                }
                SwitchRow("fa-solid fa-crosshairs", "#7c4dff", "Pointer location", "Screen overlay showing current touch data", DevOptions.pointerLocation, on) {
                    DevOptions.setPointerLocation(!DevOptions.pointerLocation)
                }
            }
            Heading("Drawing")
            Div({ classes("st-group") }) {
                SwitchRow("fa-solid fa-vector-square", "#ff3d7f", "Show layout bounds", "Show clip bounds, margins, etc.", DevOptions.layoutBounds, on) {
                    DevOptions.setLayoutBounds(!DevOptions.layoutBounds)
                }
                ScaleKind.entries.forEach { kind ->
                    val value = when (kind) {
                        ScaleKind.WINDOW -> DevOptions.windowScale
                        ScaleKind.TRANSITION -> DevOptions.transitionScale
                        ScaleKind.ANIMATOR -> DevOptions.animatorScale
                    }
                    NavRow(null, null, kind.title, scaleLabel(value), enabled = on) { choosing = kind }
                }
            }
            Heading("Hardware accelerated rendering")
            Div({ classes("st-group") }) {
                SwitchRow("fa-solid fa-chart-column", "#00bfa5", "Profile HWUI rendering", "On screen as bars", DevOptions.gpuBars, on) {
                    DevOptions.setGpuBars(!DevOptions.gpuBars)
                }
            }
            Heading("Apps")
            Div({ classes("st-group") }) {
                SwitchRow("fa-solid fa-arrow-left", "#ff8a3d", "Predictive back animations", "Swipe in from an app's edge to preview back", DevOptions.predictiveBack, on) {
                    DevOptions.setPredictiveBack(!DevOptions.predictiveBack)
                }
                SwitchRow("fa-solid fa-terminal", "#3b434f", "Terminal skin", "Re-skin the home page as terminals (or press `)", DevMode.enabled, on) {
                    PhoneState.close()
                    DevMode.toggle()
                }
            }
        }
    }
    choosing?.let { kind -> ScaleDialog(kind) { choosing = null } }
}

@Composable
private fun ScaleDialog(kind: ScaleKind, close: () -> Unit) {
    val current = when (kind) {
        ScaleKind.WINDOW -> DevOptions.windowScale
        ScaleKind.TRANSITION -> DevOptions.transitionScale
        ScaleKind.ANIMATOR -> DevOptions.animatorScale
    }
    Div({ classes("st-scrim"); onClick { close() } })
    Div({ classes("st-dialog"); attr("role", "dialog"); attr("aria-label", kind.title) }) {
        Div({ classes("st-dialog-title") }) { Text(kind.title) }
        DevOptions.scales.forEach { x ->
            Button({
                classes("st-radio")
                if (x == current) classes("on")
                onClick {
                    when (kind) {
                        ScaleKind.WINDOW -> DevOptions.setWindowScale(x)
                        ScaleKind.TRANSITION -> DevOptions.setTransitionScale(x)
                        ScaleKind.ANIMATOR -> DevOptions.setAnimatorScale(x)
                    }
                    close()
                }
            }) {
                Span({ classes("dot") })
                Text(scaleLabel(x))
            }
        }
        Button({ classes("st-dialog-cancel"); onClick { close() } }) { Text("Cancel") }
    }
}

// ---------------------------------------------------------------------------
// About phone (the build number lives here, as on Android)
// ---------------------------------------------------------------------------

@Composable
private fun AboutPhonePage(onBack: () -> Unit) {
    AppBar("About phone", onBack = onBack)
    Div({ classes("rr-a-scroll", "st") }) {
        AboutPhoneCard()
    }
}

/** Device details and the build number: tap it seven times to unlock Developer options. Also in the About app. */
@Composable
fun AboutPhoneCard() {
    var taps by remember { mutableStateOf(0) }
    Heading("About phone")
    Div({ classes("rr-a-card", "rr-a-fields", "about-phone") }) {
        listOf(
            "Device name" to "Ritwik's Pixel",
            "OS version" to "RitwikOS 7.0",
            "Kernel" to "Kotlin · Compose · React Native",
            "Uptime" to "${Profile.yearsOfExperience} years",
        ).forEach { (k, v) ->
            Div({ classes("f") }) {
                Div({ classes("txt") }) {
                    Div({ classes("rr-a-meta") }) { Text(k) }
                    Div({ classes("rr-a-title") }) { Text(v) }
                }
            }
        }
        Button({
            classes("f", "about-build")
            onClick {
                haptic(12)
                taps++
                when {
                    DevOptions.unlocked -> UiState.showToast("No need, you are already a developer.")
                    taps >= 7 -> {
                        taps = 0
                        DevOptions.unlock()
                        UiState.showToast("You are now a developer!")
                        PhoneState.open(Apps.settings, PhoneState.host, null, "developer")
                    }
                    taps >= 3 -> UiState.showToast("You are now ${7 - taps} step${if (7 - taps == 1) "" else "s"} away from being a developer.")
                }
            }
        }) {
            Div({ classes("txt") }) {
                Div({ classes("rr-a-meta") }) { Text("Build number") }
                Div({ classes("rr-a-title") }) { Text("SKOCH.GOLD.2026") }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Rows
// ---------------------------------------------------------------------------

@Composable
private fun RowIcon(icon: String?, color: String?) {
    if (icon == null) { Div({ classes("st-icon", "blank") }); return }
    Div({ classes("st-icon"); style { property("--c", color ?: "#5f6b7a") } }) { I({ classes(*icon.split(" ").toTypedArray()) }) }
}

@Composable
private fun NavRow(icon: String?, color: String?, title: String, sub: String?, enabled: Boolean = true, onClick: () -> Unit) {
    Button({
        classes("st-row")
        if (!enabled) attr("disabled", "")
        onClick { onClick() }
    }) {
        RowIcon(icon, color)
        Div({ classes("st-text") }) {
            Div({ classes("st-title") }) { Text(title) }
            if (sub != null) Div({ classes("st-sub") }) { Text(sub) }
        }
    }
}

@Composable
private fun SwitchRow(icon: String, color: String, title: String, sub: String, checked: Boolean, enabled: Boolean = true, onToggle: () -> Unit) {
    Button({
        classes("st-row")
        attr("role", "switch")
        attr("aria-checked", checked.toString())
        if (!enabled) attr("disabled", "")
        onClick { haptic(); onToggle() }
    }) {
        RowIcon(icon, color)
        Div({ classes("st-text") }) {
            Div({ classes("st-title") }) { Text(title) }
            Div({ classes("st-sub") }) { Text(sub) }
        }
        Toggle(checked)
    }
}

@Composable
private fun Toggle(on: Boolean) {
    Span({ classes("st-toggle"); if (on) classes("on"); attr("aria-hidden", "true") }) { Span({ classes("knob") }) }
}
