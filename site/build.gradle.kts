import com.varabyte.kobweb.gradle.application.util.configAsKobwebApplication
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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
            description.set("Ritwik Raj Srivastava - Software Engineer & Tech Lead specializing in Android development")
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
