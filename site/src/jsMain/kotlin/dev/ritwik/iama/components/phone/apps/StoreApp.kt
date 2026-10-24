package dev.ritwik.iama.components.phone.apps

import androidx.compose.runtime.*
import com.varabyte.kobweb.navigation.BasePath
import dev.ritwik.iama.components.phone.AppBar
import dev.ritwik.iama.components.phone.PhoneState
import dev.ritwik.iama.data.Product
import dev.ritwik.iama.data.products
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.dom.*

/** A Play-Store-style shelf of the products Ritwik has shipped. */
@Composable
fun StoreApp() {
    var selected by remember { mutableStateOf(PhoneState.payload as? Product) }
    val product = selected
    if (product == null) {
        AppBar("Products", "Things I've shipped")
        Div({ classes("rr-a-scroll") }) {
            Div({ classes("rr-a-search") }) {
                I({ classes("fa-solid", "fa-magnifying-glass") })
                Text("Apps & platforms by Ritwik")
            }
            Div({ classes("rr-a-banner") }) {
                Div({ classes("k") }) { Text("Combined reach") }
                Div({ classes("v") }) { Text("610M+ installs") }
                Div({ classes("s") }) { Text("MyJio · JioMart · JioFinance on Google Play") }
            }
            Heading("Apps & platforms")
            products.forEachIndexed { i, p ->
                Button({
                    classes("rr-a-row", "rr-a-stagger")
                    style { property("--i", i) }
                    onClick { selected = p }
                }) {
                    ProductIcon(p)
                    Div({ classes("rr-a-row-text") }) {
                        Div({ classes("rr-a-title") }) { Text(p.name) }
                        Div({ classes("rr-a-sub") }) { Text(p.tagline) }
                        Div({ classes("rr-a-meta") }) {
                            when {
                                p.badge != null -> Span({ classes("rr-a-gold") }) { I({ classes("fa-solid", "fa-trophy") }); Text(" ${p.badge}") }
                                p.rating != null -> Text("${p.rating} ★ · ${p.installs} installs")
                                else -> Text(p.role)
                            }
                        }
                    }
                    I({ classes("fa-solid", "fa-chevron-right", "rr-a-chev") })
                }
            }
        }
    } else {
        ProductPage(product, onBack = { selected = null })
    }
}

@Composable
internal fun ProductIcon(p: Product, big: Boolean = false) {
    Div({
        classes("rr-a-picon")
        if (big) classes("big")
        style { property("--a", p.accent); property("--b", p.accent2) }
    }) {
        if (p.iconImage != null) {
            Img(BasePath.prependTo(p.iconImage), p.name)
        } else {
            I({ classes(*p.icon.split(" ").toTypedArray()) })
        }
    }
}

@Composable
private fun ProductPage(p: Product, onBack: () -> Unit) {
    AppBar(p.name, p.role, onBack = onBack)
    Div({ classes("rr-a-scroll") }) {
        Div({ classes("rr-a-phead") }) {
            ProductIcon(p, big = true)
            Div {
                Div({ classes("rr-a-ptitle") }) { Text(p.name) }
                Div({ classes("rr-a-sub") }) { Text(p.tagline) }
                if (p.badge != null) {
                    Div({ classes("rr-a-gold", "pill") }) { I({ classes("fa-solid", "fa-trophy") }); Text(" ${p.badge}") }
                }
            }
        }

        Div({ classes("rr-a-stats") }) {
            if (p.rating != null) Stat("${p.rating} ★", "Play rating")
            if (p.installs != null) Stat(p.installs, "Downloads")
            Stat(p.role.substringBefore(" ·"), "My role")
        }

        if (p.url != null) {
            A(p.url, { classes("rr-a-install"); target(ATarget.Blank); attr("rel", "noopener") }) {
                I({ classes("fa-brands", "fa-google-play") }); Text("View on Google Play")
            }
        }

        if (p.screenshots.isNotEmpty()) {
            Div({ classes("rr-a-shots") }) {
                p.screenshots.forEachIndexed { i, src ->
                    Img(BasePath.prependTo(src), "${p.name} screenshot ${i + 1}") {
                        classes("rr-a-shot")
                        attr("loading", "lazy")
                    }
                }
            }
        } else {
            Div({ classes("rr-a-art"); style { property("--a", p.accent); property("--b", p.accent2) } }) {
                I({ classes(*p.icon.split(" ").toTypedArray()) })
            }
        }

        Heading("What I built")
        P({ classes("rr-a-p") }) { Text(p.myWork) }
        Ul({ classes("rr-a-list") }) { p.highlights.forEach { Li { Text(it) } } }
        Heading("Stack")
        Chips(p.tech)
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Div({ classes("rr-a-stat") }) {
        Div({ classes("v") }) { Text(value) }
        Div({ classes("l") }) { Text(label) }
    }
}
