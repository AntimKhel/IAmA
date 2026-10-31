package dev.ritwik.iama.components.phone.apps

import androidx.compose.runtime.*
import dev.ritwik.iama.components.phone.AppBar
import dev.ritwik.iama.util.Logcat
import dev.ritwik.iama.util.copyToClipboard
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.HTMLElement
import kotlin.js.Date

/**
 * Logcat for this site: the real events of this tab (see util/Logcat.kt), with Android Studio's levels,
 * colours and query syntax: `tag:Ask`, `level:w`, or plain text.
 */
@Composable
fun LogcatApp() {
    var minLevel by remember { mutableStateOf(Logcat.Level.V) }
    var query by remember { mutableStateOf("") }
    var frozen by remember { mutableStateOf<List<Logcat.Line>?>(null) }
    var follow by remember { mutableStateOf(true) }
    var list by remember { mutableStateOf<HTMLElement?>(null) }

    val source: List<Logcat.Line> = frozen ?: Logcat.lines
    val filter = remember(query) { LogFilter.parse(query) }
    val shown = source.filter { it.level >= minLevel && filter.matches(it) }.takeLast(500)

    LaunchedEffect(shown.size, shown.lastOrNull()?.id) {
        if (follow) list?.let { it.scrollTop = it.scrollHeight.toDouble() }
    }

    AppBar("Logcat", "${Logcat.PACKAGE} · pid ${Logcat.pid}", actions = {
        Button({
            classes("rr-appbar-action")
            attr("aria-label", if (frozen == null) "Pause" else "Resume")
            attr("title", if (frozen == null) "Pause" else "Resume")
            onClick { frozen = if (frozen == null) Logcat.lines.toList() else null }
        }) { I({ classes("fa-solid", if (frozen == null) "fa-pause" else "fa-play") }) }
        Button({
            classes("rr-appbar-action")
            attr("aria-label", "Copy")
            attr("title", "Copy")
            onClick { copyToClipboard(shown.joinToString("\n") { formatLine(it) }, "Log") }
        }) { I({ classes("fa-regular", "fa-copy") }) }
        Button({
            classes("rr-appbar-action")
            attr("aria-label", "Clear logcat")
            attr("title", "Clear logcat")
            onClick { Logcat.lines.clear(); frozen = null }
        }) { I({ classes("fa-solid", "fa-trash-can") }) }
    })

    Div({ classes("lc-tools") }) {
        Div({ classes("lc-levels"); attr("role", "radiogroup"); attr("aria-label", "Minimum level") }) {
            Logcat.Level.entries.forEach { level ->
                Button({
                    classes("lc-level", "lv-${level.name.lowercase()}")
                    if (level == minLevel) classes("on")
                    attr("role", "radio")
                    attr("aria-checked", (level == minLevel).toString())
                    attr("title", levelName(level))
                    onClick { minLevel = level }
                }) { Text(level.name) }
            }
        }
        Input(InputType.Search) {
            classes("lc-query")
            value(query)
            placeholder("tag:Ask  level:w  or any text")
            attr("aria-label", "Filter logs")
            attr("data-autofocus", "")
            onInput { query = it.value }
        }
    }

    Div({
        classes("lc-list")
        attr("role", "log")
        ref { el ->
            list = el
            val onScroll: (org.w3c.dom.events.Event) -> Unit = { follow = el.scrollHeight - el.scrollTop - el.clientHeight < 24 }
            el.addEventListener("scroll", onScroll)
            onDispose { el.removeEventListener("scroll", onScroll); list = null }
        }
    }) {
        if (shown.isEmpty()) {
            Div({ classes("lc-empty") }) { Text(if (source.isEmpty()) "No logs yet. Open an app or ask something." else "Nothing matches that filter.") }
        }
        shown.forEach { line -> key(line.id) { LogRow(line) } }
    }
    Div({ classes("lc-status") }) {
        Text("${shown.size} line${if (shown.size == 1) "" else "s"}${if (frozen != null) " · paused" else ""}")
        if (!follow) {
            Button({
                classes("lc-jump")
                onClick { follow = true; list?.let { it.scrollTop = it.scrollHeight.toDouble() } }
            }) { I({ classes("fa-solid", "fa-arrow-down") }); Text(" Latest") }
        }
    }
}

@Composable
private fun LogRow(line: Logcat.Line) {
    Div({ classes("lc-row", "lv-${line.level.name.lowercase()}") }) {
        Span({ classes("lc-time") }) { Text(timeOf(line.time)) }
        Span({ classes("lc-pid") }) { Text("${Logcat.pid}") }
        Span({ classes("lc-lv") }) { Text(line.level.name) }
        Span({ classes("lc-tag"); style { property("--tag-hue", "${tagHue(line.tag)}") } }) { Text(line.tag) }
        Span({ classes("lc-msg") }) { Text(line.message) }
    }
}

private class LogFilter(val tag: String?, val level: Logcat.Level?, val text: String) {
    fun matches(line: Logcat.Line): Boolean =
        (tag == null || line.tag.contains(tag, ignoreCase = true)) &&
            (level == null || line.level >= level) &&
            (text.isEmpty() || line.tag.contains(text, ignoreCase = true) || line.message.contains(text, ignoreCase = true))

    companion object {
        fun parse(query: String): LogFilter {
            var tag: String? = null
            var level: Logcat.Level? = null
            val rest = mutableListOf<String>()
            query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.forEach { word ->
                when {
                    word.startsWith("tag:", ignoreCase = true) -> tag = word.substring(4).ifEmpty { null }
                    word.startsWith("level:", ignoreCase = true) ->
                        level = Logcat.Level.entries.firstOrNull { it.name.equals(word.substring(6).take(1), ignoreCase = true) }
                    else -> rest += word
                }
            }
            return LogFilter(tag, level, rest.joinToString(" "))
        }
    }
}

private fun levelName(level: Logcat.Level) = when (level) {
    Logcat.Level.V -> "Verbose"
    Logcat.Level.D -> "Debug"
    Logcat.Level.I -> "Info"
    Logcat.Level.W -> "Warn"
    Logcat.Level.E -> "Error"
}

private fun tagHue(tag: String): Int = (tag.fold(7) { h, c -> (h * 31 + c.code) and 0xffff } % 360)

private fun two(n: Int) = n.toString().padStart(2, '0')

private fun timeOf(ms: Double): String {
    val d = Date(ms)
    return "${two(d.getMonth() + 1)}-${two(d.getDate())} ${two(d.getHours())}:${two(d.getMinutes())}:${two(d.getSeconds())}." +
        d.getMilliseconds().toString().padStart(3, '0')
}

private fun formatLine(line: Logcat.Line) = "${timeOf(line.time)}  ${Logcat.pid}  ${Logcat.pid} ${line.level.name} ${line.tag}: ${line.message}"
