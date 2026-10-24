package dev.ritwik.iama.components.sections

import androidx.compose.runtime.Composable
import dev.ritwik.iama.components.lab.Variant
import dev.ritwik.iama.data.Experience
import dev.ritwik.iama.data.experiences
import org.jetbrains.compose.web.dom.*

// ---------------------------------------------------------------------------
// Impact — public, résumé-backed numbers
// ---------------------------------------------------------------------------

private class Metric(val value: Int, val prefix: String = "", val suffix: String, val label: String, val note: String, val bar: Double)

private val metrics = listOf(
    Metric(500, suffix = "M+", label = "MyJio installs", note = "Google Play, one of India's most-installed apps", bar = 1.0),
    Metric(7, suffix = "+ yrs", label = "Android at scale", note = "Jio · HSBC · Oxigen", bar = 0.7),
    Metric(6, suffix = "", label = "Products & platforms", note = "MyJio, Agentic, JioFinance, JioMart, JioAirSync, SDKs", bar = 0.6),
    Metric(82, suffix = "%", label = "R8 obfuscation", note = "up from 24% after untangling keep rules", bar = 0.82),
)

@Composable
fun ImpactSection() {
    SectionShell("impact") { v ->
        Div({ classes("wrap") }) {
            when (v) {
                Variant.MATERIAL -> {
                    SectionHead(v, 2, "Impact", "Numbers that", "matter", "Scale I've built for, and the engineering behind it.")
                    Div({ classes("ia-grid") }) {
                        Div({ classes("ia-award"); reveal("scale") }) {
                            Div({ classes("medal") }) { I({ classes("fa-solid", "fa-trophy") }) }
                            Div {
                                Div({ classes("k") }) { Text("Skoch Award 2026") }
                                Div({ classes("v") }) { Text("Gold") }
                                Div({ classes("s") }) { Text("for JioAirSync — over-the-air updates for React Native") }
                            }
                        }
                        metrics.forEachIndexed { i, m ->
                            Div({ classes("ia-tile"); reveal("up", i * 90) }) {
                                Div({ classes("num") }) { Counter(m) }
                                Div({ classes("lbl") }) { Text(m.label) }
                                Div({ classes("note") }) { Text(m.note) }
                            }
                        }
                    }
                }
                Variant.TERMINAL -> {
                    SectionHead(v, 2, "Impact", "Numbers that", "matter", command = "impact")
                    TermWindow("htop — impact", "tb-htop") {
                        Div({ classes("tl") }) { Span({ classes("pr") }) { Text("$ ") }; Text("watch -n1 ritwik --impact") }
                        metrics.forEachIndexed { i, m ->
                            Div({ classes("hb-row"); style { property("--w", m.bar); property("--i", i) } }) {
                                Span({ classes("hb-label") }) { Text(m.label.uppercase()) }
                                Span({ classes("hb-bar") }) { Span({ classes("fill") }) }
                                Span({ classes("hb-val") }) { Counter(m) }
                            }
                        }
                        Div({ classes("tl", "gold") }) { Text("★ AWARD  Skoch 2026 · Gold · JioAirSync") }
                    }
                }
                Variant.EDITORIAL -> {
                    SectionHead(v, 2, "By the numbers", "Numbers that", "matter")
                    Div({ classes("ic-list") }) {
                        metrics.forEachIndexed { i, m ->
                            Div({ classes("ic-row"); reveal("up", i * 80) }) {
                                Div({ classes("ic-num") }) { Counter(m) }
                                Div {
                                    Div({ classes("ic-lbl") }) { Text(m.label) }
                                    Div({ classes("ic-note") }) { Text(m.note) }
                                }
                            }
                        }
                        Div({ classes("ic-row", "award"); reveal("up", 400) }) {
                            Div({ classes("ic-num") }) { Text("Gold") }
                            Div {
                                Div({ classes("ic-lbl") }) { Text("Skoch Award 2026") }
                                Div({ classes("ic-note") }) { Text("for JioAirSync, a custom React Native OTA platform") }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Counts up from 0 when revealed (CSS @property --n; see sections.css). */
@Composable
private fun Counter(m: Metric) {
    Span({
        classes("count")
        style { property("--to", m.value) }
        reveal("none")
    }) {
        if (m.prefix.isNotEmpty()) Text(m.prefix)
        Span({ classes("n") })
        Text(m.suffix)
    }
}

// ---------------------------------------------------------------------------
// Experience
// ---------------------------------------------------------------------------

private val shortHash = listOf("a1f3c9e", "7b20d4f", "e94c1a2", "3d8f6b0", "c51e7a9", "9a0b2c4", "f61d3e8", "2e7c9b1")

@Composable
fun ExperienceSection() {
    SectionShell("experience") { v ->
        Div({ classes("wrap") }) {
            when (v) {
                Variant.MATERIAL -> {
                    SectionHead(v, 3, "Experience", "Where I've", "built", "Seven years across telecom, fintech and banking.")
                    Div({ classes("ea-timeline") }) {
                        Div({ classes("ea-line") }) { Div({ classes("ea-line-fill") }) }
                        experiences.forEachIndexed { i, e ->
                            Div({ classes("ea-item"); if (i % 2 == 1) classes("right"); reveal(if (i % 2 == 0) "left" else "right") }) {
                                Div({ classes("ea-node"); if (i == 0) classes("live") })
                                ExpCard(e, i == 0)
                            }
                        }
                    }
                }
                Variant.TERMINAL -> {
                    SectionHead(v, 3, "Experience", "Where I've", "built", command = "experience")
                    TermWindow("git log --graph — career") {
                        Div({ classes("tl") }) { Span({ classes("pr") }) { Text("$ ") }; Text("git log --graph --decorate career") }
                        var h = 0
                        experiences.forEachIndexed { i, e ->
                            Div({ classes("gl-branch"); style { property("--i", i) } }) {
                                Div({ classes("tl") }) {
                                    Span({ classes("g") }) { Text("* ") }
                                    Span({ classes("hash") }) { Text(shortHash[h++ % shortHash.size]) }
                                    Span({ classes("ref") }) { Text(" (${if (i == 0) "HEAD -> " else ""}${e.company.lowercase().replace(" ", "-")}) ") }
                                    Span({ classes("v") }) { Text(e.role) }
                                }
                                Div({ classes("tl", "dim") }) { Text("|   ${e.period} · ${e.location}") }
                                e.highlights.forEach { line ->
                                    Div({ classes("tl") }) {
                                        Span({ classes("g") }) { Text("| * ") }
                                        Span({ classes("hash") }) { Text(shortHash[h++ % shortHash.size]) }
                                        Text(" $line")
                                    }
                                }
                                Div({ classes("tl", "dim") }) { Text("|/  stack: ${e.techStack.joinToString(", ")}") }
                            }
                        }
                    }
                }
                Variant.EDITORIAL -> {
                    SectionHead(v, 3, "Career", "Where I've", "built")
                    experiences.forEachIndexed { i, e ->
                        Div({ classes("ec-row"); reveal("up") }) {
                            Div({ classes("ec-years") }) { Text(e.period.replace(" — ", "\n")) }
                            Div({ classes("ec-body") }) {
                                H3({ classes("ec-company") }) { Text(e.company) }
                                Div({ classes("ec-role") }) { Text("${e.role}, ${e.location}") }
                                Ol({ classes("ec-list") }) { e.highlights.forEach { Li { Text(it) } } }
                                Div({ classes("ec-stack") }) { Text(e.techStack.joinToString(" · ")) }
                            }
                        }
                        if (i < experiences.lastIndex) Hr({ classes("ec-hr") })
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpCard(e: Experience, current: Boolean) {
    Div({ classes("ea-card") }) {
        Div({ classes("ea-head") }) {
            Div {
                H3({ classes("ea-company") }) { Text(e.company) }
                Div({ classes("ea-role") }) { Text(e.role) }
            }
            Span({ classes("ea-period"); if (current) classes("live") }) { Text(e.period) }
        }
        Ul({ classes("ea-list") }) { e.highlights.forEach { Li { Text(it) } } }
        Div({ classes("ea-chips") }) { e.techStack.forEach { Span { Text(it) } } }
    }
}
