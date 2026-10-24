package dev.ritwik.iama.components.sections

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.navigation.BasePath
import dev.ritwik.iama.components.devmode.Variant
import dev.ritwik.iama.components.phone.AppHost
import dev.ritwik.iama.components.phone.Apps
import dev.ritwik.iama.components.phone.PhoneState
import dev.ritwik.iama.data.Product
import dev.ritwik.iama.data.products
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event

@Composable
fun ProductsSection() {
    SectionShell("products") { v ->
        when (v) {
            Variant.MATERIAL -> ProductsMaterial()
            Variant.TERMINAL -> ProductsTerminal()
        }
    }
}

private fun openProduct(p: Product, el: Element?) = PhoneState.open(Apps.products, AppHost.FULL, el, p)

private fun Product.stat(): String = when {
    badge != null -> badge
    installs != null -> "$installs installs · ★ $rating"
    else -> role
}

// ---------------------------------------------------------------------------
// A — Material You: bento of tilting glass cards
// ---------------------------------------------------------------------------

@Composable
private fun ProductsMaterial() {
    Div({ classes("wrap") }) {
        SectionHead(Variant.MATERIAL, 1, "Products", "Things I've", "shipped",
            "Apps used by hundreds of millions, and the platforms underneath them.")
        Div({ classes("pa-grid") }) {
            products.forEachIndexed { i, p ->
                Div({
                    classes("pa-card", "pa-${p.id}")
                    if (i == 0) classes("big")
                    attr("data-origin", "")
                    style { property("--a", p.accent); property("--b", p.accent2) }
                    reveal("up", (i % 3) * 90)
                }) {
                    Div({
                        classes("pa-inner")
                        ref { el -> tilt(el); onDispose { } }
                        onClick { openProduct(p, it.target as? Element) }
                    }) {
                        Div({ classes("pa-glow") })
                        Div({ classes("pa-top") }) {
                            Icon(p, "pa-icon")
                            if (p.badge != null) Span({ classes("pa-badge") }) { I({ classes("fa-solid", "fa-trophy") }); Text(" ${p.badge}") }
                            else if (p.installs != null) Span({ classes("pa-badge", "soft") }) { Text("${p.installs} · ★ ${p.rating}") }
                        }
                        H3({ classes("pa-name") }) { Text(p.name) }
                        Div({ classes("pa-tag") }) { Text(p.tagline) }
                        P({ classes("pa-work") }) { Text(p.myWork) }
                        if (i == 0 && p.screenshots.isNotEmpty()) {
                            Div({ classes("pa-reel") }) {
                                Div({ classes("pa-reel-track") }) {
                                    (p.screenshots + p.screenshots).forEach { src ->
                                        Img(BasePath.prependTo(src), "") { classes("pa-shot"); attr("loading", "lazy") }
                                    }
                                }
                            }
                        }
                        Div({ classes("pa-foot") }) {
                            Div({ classes("pa-tech") }) { p.tech.forEach { Span { Text(it) } } }
                            Span({ classes("pa-open") }) { Text("Open"); I({ classes("fa-solid", "fa-arrow-right") }) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun Icon(p: Product, cls: String) {
    Div({ classes(cls); style { property("--a", p.accent); property("--b", p.accent2) } }) {
        if (p.iconImage != null) Img(BasePath.prependTo(p.iconImage), p.name)
        else I({ classes(*p.icon.split(" ").toTypedArray()) })
    }
}

/** Pointer-driven 3D tilt + glare position, written as CSS vars on the card. */
private fun tilt(el: HTMLElement) {
    val move: (Event) -> Unit = { e ->
        val m = e.asDynamic()
        val r = el.getBoundingClientRect()
        val x = ((m.clientX as Double) - r.left) / r.width
        val y = ((m.clientY as Double) - r.top) / r.height
        el.style.setProperty("--rx", "${(0.5 - y) * 10}deg")
        el.style.setProperty("--ry", "${(x - 0.5) * 12}deg")
        el.style.setProperty("--gx", "${x * 100}%")
        el.style.setProperty("--gy", "${y * 100}%")
    }
    val leave: (Event) -> Unit = {
        el.style.setProperty("--rx", "0deg")
        el.style.setProperty("--ry", "0deg")
    }
    el.addEventListener("pointermove", move)
    el.addEventListener("pointerleave", leave)
}

// ---------------------------------------------------------------------------
// B — Neon terminal: adb package listing
// ---------------------------------------------------------------------------

@Composable
private fun ProductsTerminal() {
    Div({ classes("wrap") }) {
        SectionHead(Variant.TERMINAL, 1, "Products", "Things I've", "shipped", command = "products")
        TermWindow("adb — ritwik@pixel") {
            Div({ classes("tl") }) {
                Span({ classes("pr") }) { Text("$ ") }
                Span({ classes("typed") }) { Text("adb shell pm list packages --shipped-by ritwik") }
            }
            products.forEachIndexed { i, p ->
                Button({
                    classes("tb-pkg")
                    attr("data-origin", "")
                    style { property("--i", i) }
                    onClick { openProduct(p, it.target as? Element) }
                }) {
                    Div({ classes("tl") }) {
                        Span({ classes("k") }) { Text("package:") }
                        Span({ classes("v") }) { Text(p.name.lowercase().replace(" ", "-")) }
                        Span({ classes("c") }) { Text("  # ${p.stat()}") }
                    }
                    Div({ classes("tl", "dim") }) { Text("  ├─ role   ${p.role}") }
                    Div({ classes("tl", "dim") }) { Text("  └─ built  ${p.myWork}") }
                }
            }
            Div({ classes("tl") }) {
                Span({ classes("pr") }) { Text("$ ") }
                Span({ classes("cursor") })
            }
        }
        P({ classes("tb-hint") }) { Text("› click a package to open it") }
    }
}
