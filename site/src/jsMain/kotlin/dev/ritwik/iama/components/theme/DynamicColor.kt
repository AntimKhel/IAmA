package dev.ritwik.iama.components.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.ritwik.iama.util.Logcat
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement

/**
 * Material You for the whole site: pick a seed colour (or a photo to take colours from) and every accent,
 * gradient, glow and the phone's wallpaper follow it. Stored in this browser; the head script re-applies the
 * saved CSS before first paint (see site/build.gradle.kts), so there's no flash of the default green.
 */
object DynamicColor {
    // Keys shared with the head script in build.gradle.kts.
    const val CSS_KEY = "rr-monet-css"
    const val SEED_KEY = "rr-monet-seed"
    const val PHOTO_KEY = "rr-wallpaper"
    const val ICONS_KEY = "rr-themed-icons"

    const val DEFAULT_SEED = "#3ddc84"

    class Swatch(val name: String, val seed: String)

    /** Android-style "Basic colors". The first is the site's own green. */
    val basicColors = listOf(
        Swatch("Android green", DEFAULT_SEED),
        Swatch("Ocean", "#2f6bff"),
        Swatch("Grape", "#7c4dff"),
        Swatch("Rose", "#ff3d7f"),
        Swatch("Sunset", "#ff8a3d"),
        Swatch("Gold", "#e8b23a"),
        Swatch("Sky", "#00a0e3"),
        Swatch("Teal", "#00bfa5"),
    )

    var seed by mutableStateOf(stored(SEED_KEY)); private set
    var photo by mutableStateOf(stored(PHOTO_KEY)); private set
    var photoSeeds by mutableStateOf<List<String>>(emptyList()); private set
    var themedIcons by mutableStateOf(stored(ICONS_KEY) == "1"); private set

    private fun stored(key: String): String? = runCatching { window.localStorage.getItem(key) }.getOrNull()
    private fun store(key: String, value: String?) {
        runCatching { if (value == null) window.localStorage.removeItem(key) else window.localStorage.setItem(key, value) }
    }

    /** The seed in effect (the site default when none was picked). */
    val activeSeed: String get() = seed ?: DEFAULT_SEED

    fun setSeed(hex: String?) {
        seed = if (hex.equals(DEFAULT_SEED, ignoreCase = true)) null else hex
        store(SEED_KEY, seed)
        apply()
    }

    /** A photo as wallpaper (a downscaled data URL) and the colours found in it; its first colour becomes the seed. */
    fun setPhoto(dataUrl: String?, seeds: List<String>) {
        photo = dataUrl
        photoSeeds = seeds
        val saved = runCatching { if (dataUrl == null) window.localStorage.removeItem(PHOTO_KEY) else window.localStorage.setItem(PHOTO_KEY, dataUrl); true }
            .getOrDefault(false)
        if (dataUrl != null && !saved) Logcat.w("Monet", "Wallpaper too large to remember; it will reset on reload")
        applyWallpaper()
        if (seeds.isNotEmpty()) setSeed(seeds.first()) else apply()
    }

    fun setThemedIcons(on: Boolean) {
        themedIcons = on
        store(ICONS_KEY, if (on) "1" else null)
        document.documentElement?.classList?.toggle("themed-icons", on)
        Logcat.i("Monet", "Themed icons ${if (on) "on" else "off"}")
    }

    fun reset() {
        setPhoto(null, emptyList())
        setSeed(null)
        setThemedIcons(false)
    }

    /** Re-applies everything (also called once at startup, in case storage changed in another tab). */
    fun apply() {
        val css = seed?.let { Palette.from(it) }?.let { cssFor(it) }
        val head = document.head ?: return
        var style = document.getElementById("rr-monet")
        if (css == null) {
            style?.remove()
            store(CSS_KEY, null)
            Logcat.i("Monet", "Dynamic color off: default palette")
        } else {
            if (style == null) {
                style = document.createElement("style").also { it.id = "rr-monet" }
                head.appendChild(style)
            }
            style.textContent = css
            store(CSS_KEY, css)
            val p = Palette.from(seed!!)!!
            Logcat.i("Monet", "Dynamic color from seed $seed: primary ${p.g1}, secondary ${p.g2}, tertiary ${p.g4}, accent ${p.g3}")
        }
        document.documentElement?.classList?.toggle("themed-icons", themedIcons)
        applyWallpaper()
    }

    private fun applyWallpaper() {
        val root = document.documentElement as? HTMLElement ?: return
        if (photo != null) root.style.setProperty("--wall-photo", "url(\"$photo\")") else root.style.removeProperty("--wall-photo")
    }

    /**
     * CSS for a palette. The selectors are more specific than the stylesheet's own (`:root`, `html.silk-light`),
     * so they win no matter where in <head> the head script inserts them.
     */
    fun cssFor(p: Palette): String = """
        html:root { --g1: ${p.g1}; --g2: ${p.g2}; --g3: ${p.g3}; --g4: ${p.g4}; --g1l: ${p.g1Light}; --g3l: ${p.g3Light}; --n1: ${p.nameTintDark}; --n6: ${p.nameEndDark}; }
        html.silk-light:root { --n1: ${p.nameTintLight}; --n6: ${p.nameEndLight}; }
    """.trimIndent()
}
