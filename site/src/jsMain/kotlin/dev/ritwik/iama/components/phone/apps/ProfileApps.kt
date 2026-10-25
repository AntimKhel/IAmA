package dev.ritwik.iama.components.phone.apps

import androidx.compose.runtime.*
import dev.ritwik.iama.components.devmode.DevMode
import dev.ritwik.iama.components.phone.AppBar
import dev.ritwik.iama.components.phone.PhoneState
import dev.ritwik.iama.data.Profile
import dev.ritwik.iama.data.awards
import dev.ritwik.iama.data.certifications
import dev.ritwik.iama.data.educationList
import dev.ritwik.iama.data.heroStats
import dev.ritwik.iama.data.skillCategories
import dev.ritwik.iama.util.UiState
import dev.ritwik.iama.util.copyToClipboard
import dev.ritwik.iama.util.haptic
import dev.ritwik.iama.util.phoneNumber
import dev.ritwik.iama.util.resumeHref
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.dom.*

@Composable
fun AboutApp() {
    AppBar("About", Profile.location)
    Div({ classes("rr-a-scroll") }) {
        Div({ classes("rr-a-profile") }) {
            Div({ classes("rr-a-bigavatar") }) { Text("R") }
            Div({ classes("rr-a-ptitle") }) { Text(Profile.name) }
            Div({ classes("rr-a-sub") }) { Text(Profile.headline) }
        }
        P({ classes("rr-a-p") }) { Text(Profile.summary) }
        Div({ classes("rr-a-stats") }) {
            heroStats.forEach { s ->
                Div({ classes("rr-a-stat") }) {
                    Div({ classes("v") }) { Text(s.value) }
                    Div({ classes("l") }) { Text(s.label) }
                }
            }
        }
        Heading("Education")
        educationList.forEach { e ->
            Div({ classes("rr-a-card") }) {
                Div({ classes("rr-a-title") }) { Text(e.institution) }
                Div({ classes("rr-a-sub") }) { Text(e.degree) }
                Div({ classes("rr-a-meta") }) { Text("${e.period} · ${e.score}") }
            }
        }
        AboutPhone()
        Div({ classes("rr-a-actions") }) {
            A(resumeHref, { classes("rr-a-install"); attr("download", Profile.resumeFileName) }) {
                I({ classes("fa-solid", "fa-file-arrow-down") }); Text("Résumé")
            }
            A(Profile.linkedIn, { classes("rr-a-install", "ghost"); target(ATarget.Blank); attr("rel", "noopener") }) {
                I({ classes("fa-brands", "fa-linkedin-in") }); Text("LinkedIn")
            }
        }
    }
}

@Composable
fun SkillsApp() {
    AppBar("Skills", "${skillCategories.sumOf { it.skills.size }} tools & practices")
    Div({ classes("rr-a-scroll") }) {
        skillCategories.forEachIndexed { i, c ->
            Div({ classes("rr-a-card", "rr-a-stagger"); style { property("--i", i) } }) {
                Div({ classes("rr-a-title") }) { Text(c.name) }
                Chips(c.skills)
            }
        }
    }
}

@Composable
fun AwardsApp() {
    AppBar("Awards", "Recognition & certifications")
    Div({ classes("rr-a-scroll") }) {
        Div({ classes("rr-a-trophy") }) {
            Div({ classes("medal") }) { I({ classes("fa-solid", "fa-trophy") }) }
            Div({ classes("k") }) { Text("Skoch Award 2026") }
            Div({ classes("v") }) { Text("Gold") }
            Div({ classes("s") }) { Text("for JioAirSync — a custom over-the-air update platform for React Native") }
        }
        Heading("Certifications")
        certifications.forEachIndexed { i, c ->
            Div({ classes("rr-a-row", "static", "rr-a-stagger"); style { property("--i", i) } }) {
                Div({ classes("rr-a-picon"); style { property("--a", "#2f6bff"); property("--b", "#00bfa5") } }) {
                    I({ classes("fa-solid", "fa-certificate") })
                }
                Div({ classes("rr-a-row-text") }) {
                    Div({ classes("rr-a-title") }) { Text(c.name) }
                    Div({ classes("rr-a-sub") }) { Text("${c.issuer} · ${c.note}") }
                }
            }
        }
        Heading("More")
        Ul({ classes("rr-a-list") }) { awards.drop(1).forEach { Li { Text(it) } } }
    }
}

@Composable
fun ContactApp() {
    var revealed by remember { mutableStateOf(false) }
    AppBar("Contact", "One tap to reach me")
    Div({ classes("rr-a-scroll") }) {
        Div({ classes("rr-a-profile") }) {
            Div({ classes("rr-a-bigavatar") }) { Text("R") }
            Div({ classes("rr-a-ptitle") }) { Text(Profile.name) }
            Div({ classes("rr-a-sub") }) { Text(Profile.availability) }
        }
        Div({ classes("rr-a-quick") }) {
            Button({ classes("q"); onClick { revealed = true } }) {
                Span({ classes("ic") }) { I({ classes("fa-solid", "fa-phone") }) }; Text("Call")
            }
            A("mailto:${Profile.email}", { classes("q") }) {
                Span({ classes("ic") }) { I({ classes("fa-solid", "fa-envelope") }) }; Text("Email")
            }
            A(Profile.linkedIn, { classes("q"); target(ATarget.Blank); attr("rel", "noopener") }) {
                Span({ classes("ic") }) { I({ classes("fa-brands", "fa-linkedin-in") }) }; Text("LinkedIn")
            }
            A(resumeHref, { classes("q"); attr("download", Profile.resumeFileName) }) {
                Span({ classes("ic") }) { I({ classes("fa-solid", "fa-file-arrow-down") }) }; Text("Résumé")
            }
        }
        Div({ classes("rr-a-card", "rr-a-fields") }) {
            Div({ classes("f") }) {
                I({ classes("fa-solid", "fa-phone") })
                Div({ classes("txt") }) {
                    Div({ classes("rr-a-title") }) { Text(if (revealed) phoneNumber else "Hidden from bots") }
                    Div({ classes("rr-a-meta") }) { Text("Mobile") }
                }
                if (revealed) {
                    A("tel:${phoneNumber.replace(" ", "")}", { classes("rr-mini") }) { Text("Call") }
                } else {
                    Button({ classes("rr-mini", "ok"); onClick { revealed = true } }) { Text("Reveal") }
                }
            }
            Div({ classes("f") }) {
                I({ classes("fa-solid", "fa-envelope") })
                Div({ classes("txt") }) {
                    Div({ classes("rr-a-title") }) { Text(Profile.email) }
                    Div({ classes("rr-a-meta") }) { Text("Email") }
                }
                Button({ classes("rr-mini"); onClick { copyToClipboard(Profile.email, "Email") } }) { Text("Copy") }
            }
            Div({ classes("f") }) {
                I({ classes("fa-solid", "fa-location-dot") })
                Div({ classes("txt") }) {
                    Div({ classes("rr-a-title") }) { Text(Profile.location) }
                    Div({ classes("rr-a-meta") }) { Text("Location") }
                }
            }
        }
    }
}

/** Android's "About phone", including the classic easter egg: tap Build number 7 times → dev mode. */
@Composable
private fun AboutPhone() {
    var taps by remember { mutableStateOf(0) }
    Heading("About phone")
    Div({ classes("rr-a-card", "rr-a-fields", "about-phone") }) {
        listOf(
            "Device name" to "Ritwik's Pixel",
            "OS version" to "RitwikOS 7.0",
            "Kernel" to "Kotlin · Compose · React Native",
            "Uptime" to "${Profile.yearsOfExperience} years",
        ).forEach { (k, v) ->
            Div({ classes("f") }) {
                Div({ classes("txt") }) {
                    Div({ classes("rr-a-meta") }) { Text(k) }
                    Div({ classes("rr-a-title") }) { Text(v) }
                }
            }
        }
        Button({
            classes("f", "about-build")
            onClick {
                haptic(12)
                taps++
                when {
                    DevMode.enabled -> UiState.showToast("No need, you are already a developer.")
                    taps >= 7 -> {
                        taps = 0
                        UiState.showToast("You are now a developer!")
                        PhoneState.close()
                        DevMode.toggle()
                    }
                    taps >= 3 -> UiState.showToast("You are now ${7 - taps} step${if (7 - taps == 1) "" else "s"} away from being a developer.")
                }
            }
        }) {
            Div({ classes("txt") }) {
                Div({ classes("rr-a-meta") }) { Text("Build number") }
                Div({ classes("rr-a-title") }) { Text("SKOCH.GOLD.2026") }
            }
        }
    }
}
