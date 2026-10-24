package dev.ritwik.iama.components.phone.apps

import androidx.compose.runtime.Composable
import dev.ritwik.iama.components.phone.AppBar
import dev.ritwik.iama.data.Profile
import dev.ritwik.iama.data.experiences
import org.jetbrains.compose.web.dom.*

/** Shared bits used across the in-phone apps. */
@Composable
internal fun Chips(items: List<String>) {
    Div({ classes("rr-a-chips") }) { items.forEach { Span({ classes("rr-a-chip") }) { Text(it) } } }
}

@Composable
internal fun Heading(text: String) {
    Div({ classes("rr-a-heading") }) { Text(text) }
}

internal fun initials(name: String) = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }

private val companyColors = listOf("#2f6bff" to "#7c4dff", "#e2231a" to "#ff8a3d", "#00bfa5" to "#3ddc84")

@Composable
fun WorkApp() {
    AppBar("Work", "${Profile.yearsOfExperience} years · ${experiences.size} companies")
    Div({ classes("rr-a-scroll") }) {
        Div({ classes("rr-a-hero") }) {
            Div({ classes("k") }) { Text("Currently") }
            Div({ classes("v") }) { Text("Tech Lead at Reliance Jio, building MyJio with an AI-native team.") }
        }
        Div({ classes("rr-a-timeline") }) {
            experiences.forEachIndexed { i, exp ->
                val (a, b) = companyColors[i % companyColors.size]
                Div({ classes("rr-a-card", "rr-a-stagger"); style { property("--i", i) } }) {
                    Div({ classes("rr-a-card-head") }) {
                        Div({ classes("rr-a-avatar"); style { property("--a", a); property("--b", b) } }) { Text(initials(exp.company)) }
                        Div {
                            Div({ classes("rr-a-title") }) { Text(exp.company) }
                            Div({ classes("rr-a-sub") }) { Text(exp.role) }
                            Div({ classes("rr-a-meta") }) { Text("${exp.period} · ${exp.location}") }
                        }
                        if (i == 0) Span({ classes("rr-a-live") }) { Text("Now") }
                    }
                    Ul({ classes("rr-a-list") }) { exp.highlights.forEach { Li { Text(it) } } }
                    Chips(exp.techStack)
                }
            }
        }
    }
}
