package dev.ritwik.iama.components.phone.apps

import androidx.compose.runtime.Composable
import dev.ritwik.iama.components.phone.AppBar
import dev.ritwik.iama.data.Profile
import dev.ritwik.iama.data.awards
import dev.ritwik.iama.data.certifications
import dev.ritwik.iama.data.educationList
import dev.ritwik.iama.data.experiences
import dev.ritwik.iama.data.skillCategories
import dev.ritwik.iama.util.resumeHref
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.dom.*

/**
 * Readable résumé inside the phone, with the PDF one tap away. Rendered from the same data as the
 * site (PDFs in iframes don't display on Android Chrome, and a download mid-browse breaks the flow).
 */
@Composable
fun ResumeApp() {
    AppBar("Résumé", Profile.headline)
    Div({ classes("rr-a-scroll") }) {
        Div({ classes("rr-a-actions") }) {
            A(resumeHref, { classes("rr-a-install"); attr("download", Profile.resumeFileName) }) {
                I({ classes("fa-solid", "fa-file-arrow-down") }); Text("Download PDF")
            }
            A(resumeHref, { classes("rr-a-install", "ghost"); target(ATarget.Blank); attr("rel", "noopener") }) {
                I({ classes("fa-solid", "fa-up-right-from-square") }); Text("Open PDF")
            }
        }

        Div({ classes("cv-head") }) {
            Div({ classes("rr-a-ptitle") }) { Text(Profile.name) }
            Div({ classes("rr-a-sub") }) { Text("${Profile.headline} · ${Profile.location}") }
            Div({ classes("rr-a-meta") }) { Text("${Profile.email} · linkedin.com/in/ritwik787") }
        }
        P({ classes("rr-a-p") }) { Text(Profile.summary) }

        Heading("Experience")
        experiences.forEach { e ->
            Div({ classes("cv-item") }) {
                Div({ classes("cv-row") }) {
                    Div({ classes("rr-a-title") }) { Text(e.company) }
                    Div({ classes("rr-a-meta") }) { Text(e.period) }
                }
                Div({ classes("rr-a-sub") }) { Text("${e.role} · ${e.location}") }
                Ul({ classes("rr-a-list") }) { e.highlights.forEach { Li { Text(it) } } }
            }
        }

        Heading("Skills")
        skillCategories.forEach { c ->
            Div({ classes("cv-skill") }) {
                Span({ classes("cv-k") }) { Text(c.name) }
                Span { Text(c.skills.joinToString(", ")) }
            }
        }

        Heading("Education")
        educationList.forEach { ed ->
            Div({ classes("cv-item") }) {
                Div({ classes("rr-a-title") }) { Text(ed.institution) }
                Div({ classes("rr-a-sub") }) { Text("${ed.degree} · ${ed.period} · ${ed.score}") }
            }
        }

        Heading("Certifications & awards")
        Ul({ classes("rr-a-list") }) {
            certifications.forEach { Li { Text("${it.name} — ${it.issuer}") } }
            awards.forEach { Li { Text(it) } }
        }
    }
}
