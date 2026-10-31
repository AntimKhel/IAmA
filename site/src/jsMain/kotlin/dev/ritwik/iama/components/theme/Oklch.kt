package dev.ritwik.iama.components.theme

import kotlin.math.*

/** A colour in OKLCH: perceptual lightness (0..1), chroma, hue in degrees. */
data class Oklch(val l: Double, val c: Double, val h: Double)

private fun srgbToLinear(v: Double) = if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
private fun linearToSrgb(v: Double) = if (v <= 0.0031308) 12.92 * v else 1.055 * v.pow(1 / 2.4) - 0.055

fun parseHex(hex: String): Triple<Double, Double, Double>? {
    val h = hex.trim().removePrefix("#")
    if (h.length != 6 || h.any { it.lowercaseChar() !in "0123456789abcdef" }) return null
    return Triple(h.substring(0, 2).toInt(16) / 255.0, h.substring(2, 4).toInt(16) / 255.0, h.substring(4, 6).toInt(16) / 255.0)
}

fun rgbToOklch(r: Double, g: Double, b: Double): Oklch {
    val lr = srgbToLinear(r); val lg = srgbToLinear(g); val lb = srgbToLinear(b)
    val l = cbrt(0.4122214708 * lr + 0.5363325363 * lg + 0.0514459929 * lb)
    val m = cbrt(0.2119034982 * lr + 0.6806995451 * lg + 0.1073969566 * lb)
    val s = cbrt(0.0883024619 * lr + 0.2817188376 * lg + 0.6299787005 * lb)
    val L = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
    val A = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
    val B = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
    val hue = (atan2(B, A) * 180 / PI).let { if (it < 0) it + 360 else it }
    return Oklch(L, sqrt(A * A + B * B), hue)
}

fun hexToOklch(hex: String): Oklch? = parseHex(hex)?.let { (r, g, b) -> rgbToOklch(r, g, b) }

/** Linear sRGB for an OKLCH colour (may be out of gamut). */
private fun oklchToLinear(c: Oklch): Triple<Double, Double, Double> {
    val hr = c.h * PI / 180
    val a = c.c * cos(hr); val b = c.c * sin(hr)
    val l = (c.l + 0.3963377774 * a + 0.2158037573 * b).pow(3)
    val m = (c.l - 0.1055613458 * a - 0.0638541728 * b).pow(3)
    val s = (c.l - 0.0894841775 * a - 1.2914855480 * b).pow(3)
    return Triple(
        4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
        -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
        -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s,
    )
}

/** Hex for an OKLCH colour, reducing chroma until it fits in sRGB (keeps lightness and hue). */
fun oklchToHex(c: Oklch): String {
    var chroma = c.c
    var rgb = oklchToLinear(c.copy(c = chroma))
    while (chroma > 0 && listOf(rgb.first, rgb.second, rgb.third).any { it < -0.0005 || it > 1.0005 }) {
        chroma -= 0.005
        rgb = oklchToLinear(c.copy(c = max(chroma, 0.0)))
    }
    fun channel(v: Double) = (linearToSrgb(v.coerceIn(0.0, 1.0)) * 255).roundToInt().coerceIn(0, 255).toString(16).padStart(2, '0')
    return "#" + channel(rgb.first) + channel(rgb.second) + channel(rgb.third)
}

/**
 * The site's palette from one seed, the way Material You derives a scheme: the seed's hue drives everything,
 * and the other accents sit at the same hue offsets and lightness as the original green / teal / blue /
 * purple set, so a green seed gives back the default look.
 */
class Palette(val g1: String, val g2: String, val g3: String, val g4: String, val g1Light: String, val g3Light: String,
              val nameTintDark: String, val nameEndDark: String, val nameTintLight: String, val nameEndLight: String) {
    companion object {
        fun from(seedHex: String): Palette? {
            val seed = hexToOklch(seedHex) ?: return null
            val h = seed.h
            val c = seed.c.coerceIn(0.09, 0.2)
            fun at(l: Double, chroma: Double, hue: Double) = oklchToHex(Oklch(l, chroma, (hue % 360 + 360) % 360))
            return Palette(
                g1 = at(0.80, c, h),
                g2 = at(0.71, c * 0.75, h + 28),
                g3 = at(0.56, c * 1.15, h + 140),
                g4 = at(0.58, c * 1.1, h + 114),
                g1Light = at(0.88, c * 0.7, h),
                g3Light = at(0.80, c * 0.55, h + 140),
                nameTintDark = at(0.95, 0.06, h),
                nameEndDark = at(0.80, 0.11, h + 140),
                nameTintLight = at(0.42, c * 0.8, h),
                nameEndLight = at(0.36, c, h + 140),
            )
        }
    }
}

/** Dominant vibrant colours of an image (RGBA bytes), most prominent first, at least 35° of hue apart. */
fun extractSeeds(rgba: dynamic, count: Int = 4): List<String> {
    val buckets = DoubleArray(36)
    val sums = Array(36) { doubleArrayOf(0.0, 0.0, 0.0) }
    val length = (rgba.length as Int)
    var i = 0
    while (i < length) {
        val r = (rgba[i] as Int) / 255.0; val g = (rgba[i + 1] as Int) / 255.0; val b = (rgba[i + 2] as Int) / 255.0
        val alpha = rgba[i + 3] as Int
        i += 4
        if (alpha < 200) continue
        val c = rgbToOklch(r, g, b)
        if (c.c < 0.04 || c.l < 0.18 || c.l > 0.95) continue // greys, near-black and near-white don't make a theme
        val k = (c.h / 10).toInt().coerceIn(0, 35)
        val weight = c.c * (1 - abs(c.l - 0.62))
        buckets[k] += weight
        sums[k][0] += r * weight; sums[k][1] += g * weight; sums[k][2] += b * weight
    }
    val picked = mutableListOf<Int>()
    buckets.indices.sortedByDescending { buckets[it] }.forEach { k ->
        if (buckets[k] <= 0 || picked.size >= count) return@forEach
        if (picked.all { p -> min(abs(p - k), 36 - abs(p - k)) >= 4 }) picked += k
    }
    return picked.map { k ->
        val w = buckets[k]
        val c = rgbToOklch(sums[k][0] / w, sums[k][1] / w, sums[k][2] / w)
        oklchToHex(Oklch(0.72, max(c.c, 0.1), c.h))
    }
}
