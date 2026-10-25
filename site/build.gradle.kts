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

// Cache-busting tag for the assets we link from <head>: the commit SHA on CI, "dev" locally.
// GitHub Pages caches files for 10 minutes, so a per-deploy URL keeps page and assets in sync.
val assetVersion: String = providers.environmentVariable("GITHUB_SHA").map { it.take(8) }.getOrElse("dev")

group = "dev.ritwik.iama"
version = "1.0-SNAPSHOT"

kobweb {
    app {
        index {
            // R monogram (same mark as the boot animation); favicon.ico / apple-touch-icon added below.
            faviconPath.set("/favicon.svg")
            // Let the pre-rendered page paint before the app bundle has downloaded and run.
            scriptAttributes.put("defer", "")
            description.set("Ritwik Raj Srivastava — Software Engineer & Tech Lead, Android & AI. 7+ years shipping MyJio, JioFinance and JioMart; agentic AI, AI-native SDLC, Compose and React Native.")

            // Copy into a local so the head lambda captures a String, not the build script
            // (script object references break Gradle's configuration cache).
            val assetVersion = assetVersion
            head.add {
                // Must match `site.basePath` in .kobweb/conf.yaml; head links are not auto-prefixed.
                val base = "/IAmA"
                val siteUrl = "https://antimkhel.github.io/IAmA/"
                val title = "Ritwik Raj Srivastava — Tech Lead, Android & AI"
                val summary = "7+ years shipping Android to hundreds of millions. Agentic AI, AI-native SDLC, Compose design systems and award-winning React Native OTA."

                // Runs before first paint: picks the color mode, and decides whether this visit plays the
                // boot animation so the exported snapshot never flashes ahead of it (rr.css: html.rr-booting).
                // Block comments only in here, to be safe if the script ever gets minified onto one line.
                script {
                    unsafe {
                        raw(
                            """
                            /*<![CDATA[*/
                            (function () {
                              try {
                                var l = location;
                                if (l.search.indexOf('_kobwebIsExporting') >= 0) return;
                                /* Color mode before first paint: the snapshot hard-codes silk-light. */
                                var saved = localStorage.getItem('iama:colorMode');
                                var dark = saved ? saved === 'DARK' : matchMedia('(prefers-color-scheme: dark)').matches;
                                var cl = document.documentElement.classList;
                                cl.remove('silk-light', 'silk-dark');
                                cl.add(dark ? 'silk-dark' : 'silk-light');
                                /* This page is the pre-painted snapshot: skip entrances Compose would replay (blog.css). */
                                cl.add('rr-snap');
                                var home = l.pathname.replace(/\/+$/, '') === '$base';
                                var reduce = matchMedia('(prefers-reduced-motion: reduce)').matches;
                                if (!home || reduce || sessionStorage.getItem('rr-booted')) return;
                                document.documentElement.classList.add('rr-booting');
                              } catch (e) {}
                            })();
                            /*]]>*/
                            """.trimIndent()
                        )
                    }
                }

                link(rel = "icon", href = "$base/favicon.ico") { attributes["sizes"] = "32x32" }
                link(rel = "apple-touch-icon", href = "$base/apple-touch-icon.png")

                link(rel = "preconnect", href = "https://fonts.googleapis.com")
                link(rel = "preconnect", href = "https://fonts.gstatic.com") { attributes["crossorigin"] = "" }
                // Web fonts and icons load without blocking the first paint (text shows in a system font
                // for a moment, then swaps). media=print + onload is the standard non-blocking stylesheet trick.
                val asyncCss = { href: String ->
                    link(rel = "stylesheet", href = href) {
                        attributes["media"] = "print"
                        // Skipped while exporting, or the snapshot would bake in the swapped (render-blocking) state.
                        attributes["onload"] = "if(location.search.indexOf('_kobwebIsExporting')<0)this.media='all'"
                    }
                }
                link(rel = "preconnect", href = "https://cdnjs.cloudflare.com") { attributes["crossorigin"] = "" }
                asyncCss("https://fonts.googleapis.com/css2?family=Outfit:wght@200..900&family=Inter:wght@300..800&family=JetBrains+Mono:wght@400;500;700&display=swap")
                asyncCss("https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css")
                link(rel = "stylesheet", href = "$base/css/rr.css?v=$assetVersion")
                link(rel = "stylesheet", href = "$base/css/sections.css?v=$assetVersion")
                link(rel = "stylesheet", href = "$base/css/blog.css?v=$assetVersion")
                link(rel = "stylesheet", href = "$base/css/guestbook.css?v=$assetVersion")
                // Cloudflare Turnstile (spam check for guestbook notes), rendered explicitly by the note form.
                script(src = "https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit") {
                    attributes["async"] = ""
                    attributes["defer"] = ""
                }
                // Syntax highlighting for blog code blocks (themed in blog.css); hl.js adds a ProGuard/R8 grammar.
                script(src = "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js") { attributes["defer"] = "" }
                script(src = "$base/js/hl.js?v=$assetVersion") { attributes["defer"] = "" }

                meta(name = "theme-color", content = "#07090d")
                meta(name = "author", content = "Ritwik Raj Srivastava")
                meta(name = "keywords", content = "Android, Kotlin, Jetpack Compose, React Native, Tech Lead, Agentic AI, LLM, AI-native SDLC, Forward Deployed Engineer, MyJio")
                meta { attributes["property"] = "og:type"; content = "website" }
                meta { attributes["property"] = "og:title"; content = title }
                meta { attributes["property"] = "og:description"; content = summary }
                meta { attributes["property"] = "og:url"; content = siteUrl }
                meta { attributes["property"] = "og:image"; content = "${siteUrl}icon-512.png" }
                meta(name = "twitter:card", content = "summary")
                meta(name = "twitter:title", content = title)
                meta(name = "twitter:description", content = summary)
            }
        }
    }
    markdown {
        defaultLayout.set(".components.layouts.MarkdownLayout")

        // Fenced code blocks keep their language and get a header + copy button (see components/blog/CodeBlock.kt).
        handlers {
            code.set { block ->
                val lang = block.info.trim().substringBefore(' ').lowercase()
                "dev.ritwik.iama.components.blog.CodeBlock(\"\"\"${block.literal.trimEnd().escapeTripleQuotedText()}\"\"\", \"$lang\")"
            }
        }

        // Auto-discover blog posts from markdown front matter. Posts with `draft: true` are left out
        // of the index (their pages still build, so they can be previewed by URL).
        val markdownDir = layout.projectDirectory.dir("src/jsMain/resources/markdown").asFile
        process.set { markdownEntries ->
            val dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)

            val blogEntries = markdownEntries
                .filter { it.filePath.startsWith("blog/") }
                .filter { it.frontMatter["draft"]?.firstOrNull()?.trim() != "true" }
                .sortedByDescending { entry ->
                    val dateStr = entry.frontMatter["date"]?.firstOrNull() ?: ""
                    try { LocalDate.parse(dateStr, dateFormatter) } catch (_: Exception) { LocalDate.MIN }
                }

            if (blogEntries.isEmpty()) return@set

            fun String.escapeKotlin() = replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$")

            fun readingMinutes(filePath: String): Int {
                val body = markdownDir.resolve(filePath).takeIf { it.exists() }?.readText()
                    ?.substringAfter("\n---") ?: return 1 // body after the closing front-matter fence
                val words = body.split(Regex("\\s+")).count { it.isNotBlank() }
                return maxOf(1, (words + 219) / 220)
            }

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
        readingMinutes = ${readingMinutes(entry.filePath)},
        content = { ${entry.fqn}() },
    )"""
            }

            generateKotlin(
                "dev/ritwik/iama/blog/BlogEntries.kt",
                buildString {
                    appendLine("// Auto-generated from markdown front matter. Do not edit.")
                    appendLine("package dev.ritwik.iama.blog")
                    appendLine()
                    appendLine("import androidx.compose.runtime.Composable")
                    appendLine()
                    appendLine("class BlogEntry(")
                    appendLine("    val title: String,")
                    appendLine("    val date: String,")
                    appendLine("    val description: String,")
                    appendLine("    val route: String,")
                    appendLine("    val tags: List<String>,")
                    appendLine("    val readingMinutes: Int,")
                    appendLine("    /** The post body, renderable anywhere (e.g. inside the phone's Blog app). */")
                    appendLine("    val content: @Composable () -> Unit,")
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
            implementation(libs.kobwebx.markdown)
        }
    }
}
