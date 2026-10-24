import com.varabyte.kobweb.gradle.application.util.configAsKobwebApplication
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.html.link
import kotlinx.html.meta
import kotlinx.html.script
import kotlinx.html.unsafe

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kobweb.application)
    alias(libs.plugins.kobwebx.markdown)
}

group = "dev.ritwik.iama"
version = "1.0-SNAPSHOT"

kobweb {
    app {
        index {
            description.set("Ritwik Raj Srivastava — Tech Lead, Android. Jetpack Compose, React Native, build performance and design systems at 500M+ install scale.")

            head.add {
                // Must match `site.basePath` in .kobweb/conf.yaml; head links are not auto-prefixed.
                val base = "/IAmA"
                val siteUrl = "https://antimkhel.github.io/IAmA/"
                val title = "Ritwik Raj Srivastava — Tech Lead, Android"
                val summary = "I lead Android engineering for MyJio. Compose, React Native, OTA, R8 and design systems at scale."

                // Decide *before* JS loads whether this visit plays the boot animation, so the
                // exported static snapshot never flashes ahead of it (see rr.css: html.rr-booting).
                script {
                    unsafe {
                        raw(
                            """
                            //<![CDATA[
                            (function () {
                              try {
                                var l = location;
                                if (l.search.indexOf('_kobwebIsExporting') >= 0) return;
                                var home = l.pathname.replace(/\/+$/, '') === '$base';
                                var reduce = matchMedia('(prefers-reduced-motion: reduce)').matches;
                                if (!home || reduce || sessionStorage.getItem('rr-booted')) return;
                                document.documentElement.classList.add('rr-booting');
                              } catch (e) {}
                            })();
                            //]]>
                            """.trimIndent()
                        )
                    }
                }

                link(rel = "preconnect", href = "https://fonts.googleapis.com")
                link(rel = "preconnect", href = "https://fonts.gstatic.com") { attributes["crossorigin"] = "" }
                link(
                    rel = "stylesheet",
                    href = "https://fonts.googleapis.com/css2?family=Outfit:wght@200..900&family=Inter:wght@300..800&family=JetBrains+Mono:wght@400;500;700&display=swap"
                )
                link(rel = "stylesheet", href = "$base/css/rr.css")

                meta(name = "theme-color", content = "#07090d")
                meta(name = "author", content = "Ritwik Raj Srivastava")
                meta(name = "keywords", content = "Android, Kotlin, Jetpack Compose, React Native, Tech Lead, R8, Design System, MyJio")
                meta { attributes["property"] = "og:type"; content = "website" }
                meta { attributes["property"] = "og:title"; content = title }
                meta { attributes["property"] = "og:description"; content = summary }
                meta { attributes["property"] = "og:url"; content = siteUrl }
                meta(name = "twitter:card", content = "summary_large_image")
                meta(name = "twitter:title", content = title)
                meta(name = "twitter:description", content = summary)
            }
        }
    }
    markdown {
        defaultLayout.set(".components.layouts.MarkdownLayout")

        // Auto-discover blog posts from markdown front matter
        process.set { markdownEntries ->
            val dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)

            val blogEntries = markdownEntries
                .filter { it.filePath.startsWith("blog/") }
                .sortedByDescending { entry ->
                    val dateStr = entry.frontMatter["date"]?.firstOrNull() ?: ""
                    try { LocalDate.parse(dateStr, dateFormatter) } catch (_: Exception) { LocalDate.MIN }
                }

            if (blogEntries.isEmpty()) return@set

            fun String.escapeKotlin() = replace("\\", "\\\\").replace("\"", "\\\"")

            val entriesCode = blogEntries.joinToString(",\n    ") { entry ->
                val fm = entry.frontMatter
                val title = fm["title"]?.firstOrNull()?.escapeKotlin() ?: "Untitled"
                val date = fm["date"]?.firstOrNull()?.escapeKotlin() ?: ""
                val description = fm["description"]?.firstOrNull()?.escapeKotlin() ?: ""
                val tagsRaw = fm["tags"]?.firstOrNull() ?: ""
                val tagsList = tagsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    .joinToString(", ") { "\"${it.escapeKotlin()}\"" }

                """BlogEntry(
        title = "$title",
        date = "$date",
        description = "$description",
        route = "${entry.route}",
        tags = listOf($tagsList),
    )"""
            }

            generateKotlin(
                "dev/ritwik/iama/blog/BlogEntries.kt",
                buildString {
                    appendLine("// Auto-generated from markdown front matter. Do not edit.")
                    appendLine("package dev.ritwik.iama.blog")
                    appendLine()
                    appendLine("data class BlogEntry(")
                    appendLine("    val title: String,")
                    appendLine("    val date: String,")
                    appendLine("    val description: String,")
                    appendLine("    val route: String,")
                    appendLine("    val tags: List<String>,")
                    appendLine(")")
                    appendLine()
                    appendLine("val blogEntries: List<BlogEntry> = listOf(")
                    appendLine("    $entriesCode")
                    appendLine(")")
                }
            )
        }
    }
}

kotlin {
    configAsKobwebApplication("iama")

    sourceSets {
        jsMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.html.core)
            implementation(libs.kobweb.core)
            implementation(libs.kobweb.silk)
            implementation(libs.silk.icons.fa)
            implementation(libs.kobwebx.markdown)
        }
    }
}
