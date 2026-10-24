package dev.ritwik.iama.components.sections

import androidx.compose.runtime.Composable
import dev.ritwik.iama.components.devmode.DevMode
import dev.ritwik.iama.components.devmode.Variant
import dev.ritwik.iama.util.Reveal
import dev.ritwik.iama.util.ScrollFx
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.HTMLElement

/** CSS class for a variant: v-a (Material You, default) or v-b (dev mode terminal). */
val Variant.css get() = "v-$code"

/** Outer wrapper for every home-page section: anchor id, variant class and scroll progress vars. */
@Composable
fun SectionShell(id: String, extraClass: String? = null, content: @Composable (Variant) -> Unit) {
    val variant = DevMode.variant
    Section({
        id(id)
        classes("sx", variant.css)
        if (extraClass != null) classes(extraClass)
        ref { el ->
            ScrollFx.register(el)
            onDispose { ScrollFx.unregister(el) }
        }
    }) {
        content(variant)
    }
}

/** Registers an element with the shared reveal observer; pair with the `.rv` classes. */
fun <T : HTMLElement> AttrsScope<T>.reveal(kind: String = "up", delayMs: Int = 0) {
    classes("rv", "rv-$kind")
    if (delayMs != 0) style { property("--d", "${delayMs}ms") }
    ref { el ->
        Reveal.observe(el)
        onDispose { Reveal.unobserve(el) }
    }
}

/** A revealing block. */
@Composable
fun Rv(kind: String = "up", delayMs: Int = 0, cls: String? = null, content: @Composable () -> Unit) {
    Div({
        if (cls != null) classes(*cls.split(" ").toTypedArray())
        reveal(kind, delayMs)
    }) { content() }
}

/**
 * Section heading in the language of each variant:
 *  A — eyebrow chip + display title with a gradient word
 *  B — shell prompt + uppercase mono title with a blinking cursor (dev mode)
 */
@Composable
fun SectionHead(variant: Variant, index: Int, eyebrow: String, title: String, accent: String, subtitle: String? = null, command: String = eyebrow.lowercase().replace(" ", "-")) {
    Div({ classes("sh") }) {
        when (variant) {
            Variant.MATERIAL -> {
                Rv("up") { Span({ classes("sh-eyebrow") }) { Span({ classes("dot") }); Text(eyebrow) } }
                Rv("up", 80) {
                    H2({ classes("sh-title") }) {
                        Text("$title ")
                        Span({ classes("grad") }) { Text(accent) }
                    }
                }
            }
            Variant.TERMINAL -> {
                Rv("up") {
                    Div({ classes("sh-prompt") }) {
                        Span({ classes("u") }) { Text("ritwik@portfolio") }
                        Text(":")
                        Span({ classes("p") }) { Text("~") }
                        Text("$ ")
                        Span({ classes("cmd") }) { Text("open ./$command") }
                    }
                }
                Rv("up", 80) {
                    H2({ classes("sh-title") }) {
                        Text("$title ")
                        // Keep the cursor glued to the last word so it never wraps alone.
                        Span({ style { property("white-space", "nowrap") } }) {
                            Text(accent)
                            Span({ classes("cursor") })
                        }
                    }
                }
            }
        }
        if (subtitle != null) Rv("up", 160) { P({ classes("sh-sub") }) { Text(subtitle) } }
    }
}

/** A terminal window chrome used by the Neon variant. */
@Composable
fun TermWindow(title: String, cls: String? = null, content: @Composable () -> Unit) {
    Div({
        classes("term")
        if (cls != null) classes(cls)
        reveal("up")
    }) {
        Div({ classes("term-bar") }) {
            Span({ classes("d", "r") }); Span({ classes("d", "y") }); Span({ classes("d", "g") })
            Span({ classes("t") }) { Text(title) }
        }
        Div({ classes("term-body") }) { content() }
    }
}
