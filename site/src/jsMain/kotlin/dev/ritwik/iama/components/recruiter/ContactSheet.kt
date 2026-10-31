package dev.ritwik.iama.components.recruiter

import dev.ritwik.iama.components.phone.PhoneState
import dev.ritwik.iama.components.phone.AppHost
import dev.ritwik.iama.components.phone.Apps
import dev.ritwik.iama.data.Backend
import androidx.compose.runtime.*
import dev.ritwik.iama.data.Profile
import dev.ritwik.iama.util.UiState
import dev.ritwik.iama.util.copyToClipboard
import dev.ritwik.iama.util.phoneNumber
import dev.ritwik.iama.util.resumeHref
import kotlinx.browser.window
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent

/** Everything a recruiter needs, one click from anywhere on the site. */
@Composable
fun ContactSheet() {
    var visible by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }

    LaunchedEffect(UiState.contactOpen) {
        if (UiState.contactOpen) {
            closing = false
            visible = true
        } else if (visible) {
            closing = true
            delay(250)
            visible = false
            closing = false
        }
    }

    if (!visible) return

    DisposableEffect(Unit) {
        val onKey: (Event) -> Unit = { if ((it as KeyboardEvent).key == "Escape") UiState.contactOpen = false }
        window.addEventListener("keydown", onKey)
        onDispose { window.removeEventListener("keydown", onKey) }
    }

    Div({
        classes("rr-sheet-scrim")
        if (closing) classes("is-closing")
        onClick { UiState.contactOpen = false }
    })
    Div({
        classes("rr-sheet")
        if (closing) classes("is-closing")
        attr("role", "dialog")
        attr("aria-modal", "true")
        attr("aria-label", "Contact ${Profile.name}")
    }) {
        Div({ classes("rr-sheet-head") }) {
            Div({ classes("rr-avatar") }) { Text("R") }
            Div {
                H3 { Text(Profile.name) }
                P { Text("${Profile.headline} · ${Profile.yearsOfExperience} yrs · ${Profile.location}") }
            }
            Button({
                classes("rr-nav-icon", "rr-sheet-close")
                attr("aria-label", "Close")
                onClick { UiState.contactOpen = false }
            }) { I({ classes("fa-solid", "fa-xmark") }) }
        }

        Div({ classes("rr-rows") }) {
            ContactRow("fa-solid fa-envelope", "#2f6bff", "#00bfa5", "Email", Profile.email) {
                Button({ classes("rr-mini"); onClick { copyToClipboard(Profile.email, "Email") } }) {
                    I({ classes("fa-regular", "fa-copy") }); Text("Copy")
                }
                A("mailto:${Profile.email}", { classes("rr-mini") }) { Text("Write") }
            }
            PhoneRow()
            ContactRow("fa-brands fa-linkedin-in", "#0a66c2", "#2f6bff", "LinkedIn", Profile.linkedIn.removePrefix("https://www.")) {
                A(Profile.linkedIn, { classes("rr-mini"); target(ATarget.Blank); attr("rel", "noopener") }) {
                    Text("Open"); I({ classes("fa-solid", "fa-arrow-up-right-from-square") })
                }
            }
            ContactRow("fa-brands fa-github", "#3b434f", "#151a21", "GitHub", Profile.github.removePrefix("https://")) {
                A(Profile.github, { classes("rr-mini"); target(ATarget.Blank); attr("rel", "noopener") }) {
                    Text("Open"); I({ classes("fa-solid", "fa-arrow-up-right-from-square") })
                }
            }
        }

        if (Backend.enabled) {
            Button({
                classes("rr-sheet-fit")
                attr("data-origin", "")
                onClick { e ->
                    UiState.contactOpen = false
                    PhoneState.open(Apps.ask, AppHost.FULL, e.target as? org.w3c.dom.Element, "fit")
                }
            }) {
                I({ classes("fa-solid", "fa-clipboard-check") })
                Span {
                    B { Text("Hiring for a role?") }
                    Text(" Paste the job description and see how ${Profile.firstName} fits, with the evidence.")
                }
                I({ classes("fa-solid", "fa-arrow-right") })
            }
        }
        Div({ classes("rr-sheet-cta") }) {
            A(resumeHref, { classes("rr-btn", "primary"); attr("download", Profile.resumeFileName) }) {
                I({ classes("fa-solid", "fa-file-arrow-down") }); Text("Download résumé")
            }
            A("mailto:${Profile.email}?subject=Opportunity%20for%20Ritwik", { classes("rr-btn") }) {
                I({ classes("fa-solid", "fa-paper-plane") }); Text("Email me")
            }
        }
    }
}

@Composable
private fun ContactRow(icon: String, a: String, b: String, label: String, value: String, actions: @Composable () -> Unit) {
    Div({ classes("rr-row") }) {
        Div({ classes("ic"); style { property("--a", a); property("--b", b) } }) {
            I({ classes(*icon.split(" ").toTypedArray()) })
        }
        Div({ classes("txt") }) {
            Div({ classes("k") }) { Text(label) }
            Div({ classes("v") }) { Text(value) }
        }
        Div({ classes("act") }) { actions() }
    }
}

/** The number only exists in the DOM after an explicit tap. */
@Composable
private fun PhoneRow() {
    var revealed by remember { mutableStateOf(false) }
    ContactRow(
        "fa-solid fa-phone", "#3ddc84", "#00bfa5", "Phone",
        if (revealed) phoneNumber else "Tap reveal to show the number",
    ) {
        if (!revealed) {
            Button({ classes("rr-mini", "ok"); onClick { revealed = true } }) {
                I({ classes("fa-solid", "fa-eye") }); Text("Reveal")
            }
        } else {
            Button({ classes("rr-mini"); onClick { copyToClipboard(phoneNumber, "Phone number") } }) {
                I({ classes("fa-regular", "fa-copy") }); Text("Copy")
            }
            A("tel:${phoneNumber.replace(" ", "")}", { classes("rr-mini") }) { Text("Call") }
        }
    }
}

@Composable
fun Toast() {
    val message = UiState.toast ?: return
    key(message) {
        Div({ classes("rr-toast"); attr("role", "status") }) {
            I({ classes("fa-solid", "fa-circle-check") })
            Text(message)
        }
    }
}
