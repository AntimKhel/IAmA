package dev.ritwik.iama.components.phone.apps

import androidx.compose.runtime.*
import dev.ritwik.iama.components.phone.AppBar
import dev.ritwik.iama.data.WorldMap
import dev.ritwik.iama.util.Api
import dev.ritwik.iama.util.Logcat
import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import kotlin.js.Date
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

/** /stats/world, kept for the session so reopening the app is instant (refreshed after a minute). */
private object WorldStats {
    class Country(val code: String, val visitors: Int)
    class Arrival(val code: String, val at: Double)
    class Data(
        val since: String, val you: String?, val visitors: Int, val askAnswered: Int, val fitChecks: Int, val notes: Int,
        val countries: List<Country>, val recent: List<Arrival>,
    )

    var data by mutableStateOf<Data?>(null)
    var failed by mutableStateOf(false)
    private var fetchedAt = 0.0

    suspend fun load() {
        if (data != null && Date.now() - fetchedAt < 60_000) return
        val res = Api.get("/stats/world")
        if (res?.ok != true) { failed = data == null; return }
        val b = res.body
        data = Data(
            since = b.since as String,
            you = b.you as? String,
            visitors = (b.visitors as Number).toInt(),
            askAnswered = (b.askAnswered as Number).toInt(),
            fitChecks = (b.fitChecks as Number).toInt(),
            notes = (b.notes as Number).toInt(),
            countries = (b.countries as Array<dynamic>).map { Country(it.code as String, (it.visitors as Number).toInt()) },
            recent = (b.recent as Array<dynamic>).map { Arrival(it.code as String, (it.at as Number).toDouble()) },
        )
        failed = false
        fetchedAt = Date.now()
        Logcat.d("VisitorMap", "${data!!.countries.size} countries, ${data!!.visitors} visitors, you=${data!!.you ?: "?"}")
    }
}

/** Where visitors come from: a dotted world map with a pulse per country (country codes only, no IPs). */
@Composable
fun StatsApp() {
    LaunchedEffect(Unit) { WorldStats.load() }
    val data = WorldStats.data

    AppBar("Stats", "Visitors around the world")
    Div({ classes("rr-a-scroll", "wm") }) {
        if (data == null) {
            P({ classes("rr-a-p") }) { Text(if (WorldStats.failed) "Couldn't load the map right now. Try again in a bit." else "Loading the map…") }
            return@Div
        }
        data.you?.let { code ->
            Div({ classes("wm-you") }) {
                Span({ classes("wm-flag") }) { Text(flag(code)) }
                Text("Hello from ${countryName(code)}! You're on the map.")
            }
        }
        Div({ classes("wm-tiles") }) {
            Tile(data.visitors, "visitors")
            Tile(data.countries.size, if (data.countries.size == 1) "country" else "countries")
            Tile(data.askAnswered, "questions answered")
            Tile(data.notes, "guestbook notes")
        }

        WorldMapView(data)

        if (data.countries.isNotEmpty()) {
            Heading("Top countries")
            val top = data.countries.take(8)
            val most = top.first().visitors.coerceAtLeast(1)
            Div({ classes("wm-list") }) {
                top.forEach { c ->
                    Div({ classes("wm-row"); if (c.code == data.you) classes("you") }) {
                        Span({ classes("wm-flag") }) { Text(flag(c.code)) }
                        Span({ classes("wm-name") }) { Text(countryName(c.code)) }
                        Span({ classes("wm-bar") }) { Span({ style { property("width", "${(c.visitors * 100 / most).coerceAtLeast(4)}%") } }) }
                        Span({ classes("wm-count") }) { Text("${c.visitors}") }
                    }
                }
            }
        }
        if (data.recent.isNotEmpty()) {
            Heading("Latest arrivals")
            Div({ classes("wm-recent") }) {
                data.recent.take(6).forEach { a ->
                    Div({ classes("wm-arrival") }) {
                        Span({ classes("wm-flag") }) { Text(flag(a.code)) }
                        Span { Text(countryName(a.code)) }
                        Span({ classes("rr-a-meta") }) { Text(ago(a.at)) }
                    }
                }
            }
        }
        P({ classes("wm-note") }) {
            Text("Countries counted since ${formatSince(data.since)}. Only the country code Cloudflare reports is kept: no IP addresses, no cities.")
        }
    }
}

@Composable
private fun Tile(value: Int, label: String) {
    Div({ classes("wm-tile") }) {
        Div({ classes("v") }) { Text(value.asDynamic().toLocaleString() as String) }
        Div({ classes("l") }) { Text(label) }
    }
}

private class LonLat(val lon: Double, val lat: Double)

private val countryPoints: Map<String, LonLat> by lazy {
    WorldMap.COUNTRY_POINTS.split(";").mapNotNull { entry ->
        val code = entry.substringBefore(":")
        val (lon, lat) = entry.substringAfter(":").split(",").map { it.toDouble() }
        if (code.length == 2) code to LonLat(lon, lat) else null
    }.toMap()
}

private val landBits: ByteArray by lazy {
    val raw = window.atob(WorldMap.LAND_BITS)
    ByteArray(raw.length) { raw[it].code.toByte() }
}

private fun isLand(row: Int, col: Int): Boolean {
    val k = row * WorldMap.COLS + col
    return (landBits[k shr 3].toInt() and (1 shl (7 - (k and 7)))) != 0
}

@Composable
private fun WorldMapView(data: WorldStats.Data) {
    val most = data.countries.maxOfOrNull { it.visitors } ?: 1
    val fresh = data.recent.filter { Date.now() - it.at < 30 * 60_000 }.map { it.code }.toSet()
    Div({ classes("wm-map") }) {
        Canvas({
            classes("wm-dots")
            attr("aria-hidden", "true")
            ref { el ->
                val canvas = el.unsafeCast<HTMLCanvasElement>()
                val draw = { drawDots(canvas) }
                draw()
                val onResize: (org.w3c.dom.events.Event) -> Unit = { draw() }
                window.addEventListener("resize", onResize)
                onDispose { window.removeEventListener("resize", onResize) }
            }
        })
        val span = WorldMap.LAT_TOP - WorldMap.LAT_BOTTOM
        data.countries.forEach { c ->
            val p = countryPoints[c.code] ?: return@forEach
            val size = 7 + 18 * sqrt(c.visitors.toDouble() / most)
            Span({
                classes("wm-pulse")
                if (c.code in fresh) classes("fresh")
                if (c.code == data.you) classes("you")
                attr("title", "${countryName(c.code)}: ${c.visitors} visitor${if (c.visitors == 1) "" else "s"}")
                style {
                    property("left", "${(p.lon + 180) / 360 * 100}%")
                    property("top", "${(WorldMap.LAT_TOP - p.lat) / span * 100}%")
                    property("--size", "${size}px")
                }
            })
        }
    }
}

/** Land dots, spaced so they stay distinct at any width, in the theme's primary colour. */
private fun drawDots(canvas: HTMLCanvasElement) {
    val box = (canvas.parentElement as? HTMLElement) ?: return
    val w = box.clientWidth.toDouble().coerceAtLeast(100.0)
    val h = w * WorldMap.ROWS / WorldMap.COLS
    val dpr = window.devicePixelRatio
    canvas.width = (w * dpr).toInt()
    canvas.height = (h * dpr).toInt()
    canvas.style.height = "${h}px"
    val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
    val cell = w / WorldMap.COLS
    val step = max(1, ceil(3.6 / cell).toInt())
    val radius = cell * step * 0.32 * dpr
    val primary = window.getComputedStyle(document.documentElement!!).getPropertyValue("--g1").trim().ifEmpty { "#3ddc84" }
    ctx.fillStyle = primary
    ctx.globalAlpha = 0.34
    var row = 0
    while (row < WorldMap.ROWS) {
        var col = 0
        while (col < WorldMap.COLS) {
            if (isLand(row, col)) {
                ctx.beginPath()
                ctx.arc((col + 0.5) * cell * dpr, (row + 0.5) * cell * dpr, radius, 0.0, 6.2832)
                ctx.fill()
            }
            col += step
        }
        row += step
    }
}

private fun flag(code: String): String {
    if (code.length != 2) return "🏳️"
    val base = 0x1F1E6 - 'A'.code
    return js("String").fromCodePoint(base + code[0].uppercaseChar().code, base + code[1].uppercaseChar().code) as String
}

private val regionNames: dynamic by lazy { runCatching { js("new Intl.DisplayNames(['en'], { type: 'region' })") }.getOrNull() }

private fun countryName(code: String): String =
    runCatching { regionNames?.of(code) as? String }.getOrNull() ?: code

private fun ago(at: Double): String {
    val minutes = ((Date.now() - at) / 60_000).toInt()
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 24 * 60 -> "${minutes / 60} h ago"
        else -> Date(at).toLocaleDateString("en", js("({ month: 'short', day: 'numeric' })"))
    }
}

private fun formatSince(day: String): String =
    runCatching { Date("${day}T00:00:00Z").toLocaleDateString("en", js("({ day: 'numeric', month: 'short', year: 'numeric' })")) }.getOrDefault(day)
