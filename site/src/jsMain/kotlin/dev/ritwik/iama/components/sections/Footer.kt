package dev.ritwik.iama.components.sections

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.toAttrs
import dev.ritwik.iama.components.widgets.VisitorBadge
import dev.ritwik.iama.data.Profile
import dev.ritwik.iama.util.resumeHref
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.dom.*
import kotlin.js.Date

@Composable
fun Footer(modifier: Modifier = Modifier) {
    Footer(modifier.toAttrs { classes("ft") }) {
        Div({ classes("ft-inner") }) {
            Div({ classes("ft-brand") }) {
                Span({ classes("ft-logo") }) { Text("R") }
                Div {
                    Div({ classes("ft-name") }) { Text(Profile.name) }
                    Div({ classes("ft-role") }) { Text("${Profile.headline} · ${Profile.location}") }
                }
            }
            Div({ classes("ft-links") }) {
                A("mailto:${Profile.email}", { attr("aria-label", "Email") }) { I({ classes("fa-solid", "fa-envelope") }) }
                A(Profile.linkedIn, { target(ATarget.Blank); attr("rel", "noopener"); attr("aria-label", "LinkedIn") }) { I({ classes("fa-brands", "fa-linkedin-in") }) }
                A(Profile.github, { target(ATarget.Blank); attr("rel", "noopener"); attr("aria-label", "GitHub") }) { I({ classes("fa-brands", "fa-github") }) }
                A(resumeHref, { attr("download", Profile.resumeFileName); attr("aria-label", "Résumé") }) { I({ classes("fa-solid", "fa-file-arrow-down") }) }
            }
        }
        Div({ classes("ft-base") }) {
            Span { Text("© ${Date().getFullYear()} ${Profile.name}") }
            VisitorBadge()
            Span { Text("Built in Kotlin with Kobweb · no JavaScript frameworks were harmed") }
        }
    }
}
