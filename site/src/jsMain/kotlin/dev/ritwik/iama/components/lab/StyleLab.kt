package dev.ritwik.iama.components.lab

import androidx.compose.runtime.*
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.*

/** The three design directions each section can be rendered in. */
enum class Variant(val code: String, val label: String) {
    MATERIAL("A", "Material You"),
    TERMINAL("B", "Neon terminal"),
    EDITORIAL("C", "Editorial"),
}

class LabSection(val id: String, val label: String)

val labSections = listOf(
    LabSection("products", "Products"),
    LabSection("impact", "Impact"),
    LabSection("experience", "Experience"),
    LabSection("roadmap", "Upcoming work"),
    LabSection("skills", "Skills"),
    LabSection("blog", "Blog"),
    LabSection("contact", "Contact"),
)

/**
 * Per-section variant choice. Defaults to Material You; the lab panel (localhost or `?lab`)
 * lets Ritwik flip each section and remembers the picks in localStorage.
 */
object StyleLab {
    private const val KEY = "rr-lab"
    private val picks = mutableStateMapOf<String, Variant>()

    val enabled: Boolean by lazy {
        val loc = window.location
        loc.hostname == "localhost" || loc.hostname == "127.0.0.1" || loc.search.contains("lab")
    }

    init {
        if (enabled) {
            runCatching { window.localStorage.getItem(KEY) }.getOrNull()?.split(";")?.forEach { pair ->
                val (id, code) = pair.split("=").takeIf { it.size == 2 } ?: return@forEach
                Variant.entries.firstOrNull { it.code == code }?.let { picks[id] = it }
            }
        }
    }

    fun of(id: String): Variant = picks[id] ?: Variant.MATERIAL

    fun set(id: String, v: Variant) {
        picks[id] = v
        persist()
    }

    fun setAll(v: Variant) {
        labSections.forEach { picks[it.id] = v }
        persist()
    }

    private fun persist() {
        runCatching { window.localStorage.setItem(KEY, picks.entries.joinToString(";") { "${it.key}=${it.value.code}" }) }
    }
}

@Composable
fun StyleLabPanel() {
    if (!StyleLab.enabled) return
    var open by remember { mutableStateOf(true) }
    Div({ classes("lab"); if (!open) classes("is-collapsed") }) {
        Button({ classes("lab-toggle"); onClick { open = !open } }) {
            I({ classes("fa-solid", "fa-flask") })
            Span { Text("Style Lab") }
            I({ classes("fa-solid", if (open) "fa-chevron-down" else "fa-chevron-up") })
        }
        if (open) {
            Div({ classes("lab-legend") }) {
                Variant.entries.forEach { v -> Span { B { Text(v.code) }; Text(" ${v.label}") } }
            }
            Div({ classes("lab-row", "all") }) {
                Span({ classes("lab-name") }) { Text("All sections") }
                Div({ classes("lab-seg") }) {
                    Variant.entries.forEach { v ->
                        Button({ onClick { StyleLab.setAll(v) }; attr("title", v.label) }) { Text(v.code) }
                    }
                }
            }
            labSections.forEach { s ->
                Div({ classes("lab-row") }) {
                    A("#${s.id}", { classes("lab-name") }) { Text(s.label) }
                    Div({ classes("lab-seg") }) {
                        Variant.entries.forEach { v ->
                            Button({
                                if (StyleLab.of(s.id) == v) classes("on")
                                attr("title", v.label)
                                onClick { StyleLab.set(s.id, v) }
                            }) { Text(v.code) }
                        }
                    }
                }
            }
        }
    }
}
