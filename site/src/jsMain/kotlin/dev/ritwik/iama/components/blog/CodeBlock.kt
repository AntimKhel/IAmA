package dev.ritwik.iama.components.blog

import androidx.compose.runtime.Composable
import dev.ritwik.iama.util.copyToClipboard
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.HTMLElement

private val labels = mapOf(
    "kotlin" to "Kotlin", "kt" to "Kotlin", "kts" to "Gradle (Kotlin)", "java" to "Java",
    "proguard" to "ProGuard / R8", "pro" to "ProGuard / R8", "bash" to "Shell", "sh" to "Shell",
    "json" to "JSON", "typescript" to "TypeScript", "ts" to "TypeScript", "tsx" to "TSX",
    "javascript" to "JavaScript", "js" to "JavaScript", "xml" to "XML", "yaml" to "YAML",
    "text" to "Text", "" to "Code",
)

/**
 * Fenced code block for markdown posts (wired via `markdown.handlers.code` in build.gradle.kts):
 * language label, copy button, and highlight.js once it has loaded.
 */
@Composable
fun CodeBlock(code: String, lang: String) {
    val hlLang = when (lang) {
        "kts", "kt" -> "kotlin"
        "pro", "r8" -> "proguard"
        "sh" -> "bash"
        "ts" -> "typescript"
        "text", "" -> "plaintext"
        else -> lang
    }
    Div({ classes("code-block") }) {
        Div({ classes("code-head") }) {
            Span({ classes("code-dots") }) { Span(); Span(); Span() }
            Span({ classes("code-lang") }) { Text(labels[lang] ?: lang.uppercase()) }
            Button({
                classes("code-copy")
                attr("aria-label", "Copy code")
                onClick { copyToClipboard(code, "Code") }
            }) { I({ classes("fa-regular", "fa-copy") }); Span { Text("Copy") } }
        }
        Pre {
            Code({
                classes("language-$hlLang")
                ref { el ->
                    highlight(el, 0)
                    onDispose { }
                }
            }) { Text(code) }
        }
    }
}

/** highlight.js is loaded with `defer`; retry briefly until it (and our ProGuard grammar) is ready. */
private fun highlight(el: HTMLElement, attempt: Int) {
    val hljs = window.asDynamic().hljs
    val ready = hljs != null && (!el.className.contains("language-proguard") || hljs.getLanguage("proguard") != null)
    if (ready) {
        if (el.getAttribute("data-highlighted") == null) hljs.highlightElement(el)
    } else if (attempt < 40) {
        window.setTimeout({ highlight(el, attempt + 1) }, 100)
    }
}
