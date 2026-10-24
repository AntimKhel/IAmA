package dev.ritwik.iama.components.sections

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.core.rememberPageContext
import com.varabyte.kobweb.navigation.BasePath
import dev.ritwik.iama.blog.BlogEntry
import dev.ritwik.iama.blog.blogEntries
import dev.ritwik.iama.components.lab.Variant
import dev.ritwik.iama.data.Profile
import dev.ritwik.iama.data.agenticRoadmap
import dev.ritwik.iama.data.roadmapIntro
import dev.ritwik.iama.data.skillCategories
import dev.ritwik.iama.util.UiState
import dev.ritwik.iama.util.copyToClipboard
import dev.ritwik.iama.util.resumeHref
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.dom.*

// ---------------------------------------------------------------------------
// Upcoming work — Agentic MyJio roadmap
// ---------------------------------------------------------------------------

@Composable
fun RoadmapSection() {
    SectionShell("roadmap") { v ->
        Div({ classes("wrap") }) {
            when (v) {
                Variant.MATERIAL -> {
                    SectionHead(v, 4, "Upcoming work", "Agentic MyJio,", "what's next", roadmapIntro)
                    Div({ classes("ra-track") }) {
                        Div({ classes("ra-path") }) { Div({ classes("ra-path-fill") }) }
                        agenticRoadmap.forEachIndexed { i, ph ->
                            Div({
                                classes("ra-card")
                                if (i == 0) classes("now")
                                style { property("--c", ph.accent) }
                                reveal("up", i * 120)
                            }) {
                                Div({ classes("ra-node") }) { I({ classes(*ph.icon.split(" ").toTypedArray()) }) }
                                Div({ classes("ra-meta") }) {
                                    Span({ classes("ra-phase") }) { Text("Phase ${ph.number}") }
                                    Span({ classes("ra-horizon") }) { Text(ph.horizon) }
                                }
                                H3({ classes("ra-name") }) { Text(ph.name) }
                                P({ classes("ra-sum") }) { Text(ph.summary) }
                                Ul({ classes("ra-items") }) { ph.items.forEach { Li { Text(it) } } }
                            }
                        }
                    }
                }
                Variant.TERMINAL -> {
                    SectionHead(v, 4, "Upcoming work", "Agentic MyJio,", "what's next", command = "roadmap")
                    TermWindow("ROADMAP.md — myjio-agentic") {
                        Div({ classes("tl") }) { Span({ classes("pr") }) { Text("$ ") }; Text("cat ROADMAP.md") }
                        Div({ classes("tl", "h") }) { Text("# MyJio Agentic") }
                        Div({ classes("tl", "dim") }) { Text("> $roadmapIntro") }
                        agenticRoadmap.forEachIndexed { i, ph ->
                            Div({ classes("rb-phase"); style { property("--i", i) } }) {
                                Div({ classes("tl", "h2") }) {
                                    Text("## Phase ${ph.number} · ${ph.name} ")
                                    Span({ classes("tag", "t$i") }) { Text("[${ph.horizon.uppercase()}]") }
                                }
                                ph.items.forEach { item ->
                                    Div({ classes("tl") }) {
                                        Span({ classes("box") }) { Text(if (i == 0) "- [x] " else "- [ ] ") }
                                        Text(item)
                                    }
                                }
                            }
                        }
                        Div({ classes("tl", "dim") }) {
                            Text("progress ")
                            Span({ classes("rb-bar") }) { Text("[██████░░░░░░░░░░░░░░]") }
                            Text(" phase 1 of 4")
                        }
                    }
                }
                Variant.EDITORIAL -> {
                    SectionHead(v, 4, "What's next", "Agentic MyJio,", "the road ahead", roadmapIntro)
                    Div({ classes("rc-grid") }) {
                        agenticRoadmap.forEachIndexed { i, ph ->
                            Div({ classes("rc-col"); reveal("up", i * 90) }) {
                                Div({ classes("rc-roman") }) { Text(listOf("I", "II", "III", "IV")[i]) }
                                Div({ classes("rc-horizon") }) { Text(ph.horizon) }
                                H3({ classes("rc-name") }) { Text(ph.name) }
                                P({ classes("rc-sum") }) { Text(ph.summary) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Skills
// ---------------------------------------------------------------------------

@Composable
fun SkillsSection() {
    SectionShell("skills") { v ->
        when (v) {
            Variant.MATERIAL -> Div({ classes("wrap") }) {
                SectionHead(v, 5, "Skills", "The", "toolkit", "AI-native engineering on a deep Android foundation.")
                Div({ classes("sa-layout") }) {
                    Div({ classes("sa-orbit"); attr("aria-hidden", "true") }) {
                        Div({ classes("sa-core") }) { Text("R") }
                        skillCategories.take(3).forEachIndexed { ring, cat ->
                            Div({ classes("sa-ring", "r$ring"); style { property("--n", cat.skills.size) } }) {
                                cat.skills.forEachIndexed { k, s ->
                                    Span({ classes("sa-chip"); style { property("--k", k) } }) { Span { Text(s) } }
                                }
                            }
                        }
                    }
                    Div({ classes("sa-cats") }) {
                        skillCategories.forEachIndexed { i, cat ->
                            Div({ classes("sa-cat"); reveal("up", i * 70) }) {
                                Div({ classes("sa-cat-name") }) { Text(cat.name) }
                                Div({ classes("sa-cat-skills") }) { cat.skills.forEach { Span { Text(it) } } }
                            }
                        }
                    }
                }
            }
            Variant.TERMINAL -> Div({ classes("wrap") }) {
                SectionHead(v, 5, "Skills", "The", "toolkit", command = "skills")
                TermWindow("build.gradle.kts — ritwik") {
                    Div({ classes("tl") }) { Span({ classes("kw") }) { Text("dependencies") }; Text(" {") }
                    skillCategories.forEach { cat ->
                        Div({ classes("tl", "cm") }) { Text("    // ${cat.name}") }
                        cat.skills.forEach { s ->
                            Div({ classes("tl") }) {
                                Text("    ")
                                Span({ classes("fn") }) { Text("implementation") }
                                Text("(")
                                Span({ classes("str") }) { Text("\"ritwik:${s.lowercase().replace(" ", "-").replace("/", "")}:latest\"") }
                                Text(")")
                            }
                        }
                    }
                    Div({ classes("tl") }) { Text("}") }
                }
            }
            Variant.EDITORIAL -> {
                Div({ classes("wrap") }) { SectionHead(v, 5, "Craft", "The", "toolkit") }
                Div({ classes("sc-belts") }) {
                    val all = skillCategories.flatMap { it.skills }
                    listOf(all, all.reversed(), all.shuffledStable()).forEachIndexed { row, items ->
                        Div({ classes("sc-belt", "b$row") }) {
                            Div({ classes("sc-track") }) {
                                (items + items).forEachIndexed { k, s ->
                                    Span({ classes("sc-word"); if (k % 3 == 1) classes("it") }) { Text(s) }
                                    Span({ classes("sc-star") }) { Text("✦") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun List<String>.shuffledStable() = withIndex().sortedBy { (i, s) -> (s.hashCode() * 31 + i) % 97 }.map { it.value }

// ---------------------------------------------------------------------------
// Blog teaser
// ---------------------------------------------------------------------------

@Composable
fun BlogTeaserSection() {
    val router = rememberPageContext().router
    val posts = blogEntries.take(3)
    SectionShell("blog") { v ->
        Div({ classes("wrap") }) {
            when (v) {
                Variant.MATERIAL -> {
                    SectionHead(v, 6, "Writing", "Notes from", "the field", "Long-form posts on Android, AI-native engineering and building at scale.")
                    Div({ classes("ba-grid") }) {
                        posts.forEachIndexed { i, p ->
                            A(BasePath.prependTo(p.route), {
                                classes("ba-card")
                                reveal("up", i * 100)
                                onClick { it.preventDefault(); router.tryRoutingTo(p.route) }
                            }) {
                                Div({ classes("ba-cover"); style { property("--h", hue(p)) } }) {
                                    Span({ classes("ba-glyph") }) { Text(p.title.first().toString()) }
                                }
                                Div({ classes("ba-body") }) {
                                    Div({ classes("ba-meta") }) { Text("${p.date} · ${p.readingMinutes} min") }
                                    H3({ classes("ba-title") }) { Text(p.title) }
                                    P({ classes("ba-desc") }) { Text(p.description) }
                                }
                            }
                        }
                    }
                    AllPosts(router)
                }
                Variant.TERMINAL -> {
                    SectionHead(v, 6, "Writing", "Notes from", "the field", command = "blog")
                    TermWindow("~/blog") {
                        Div({ classes("tl") }) { Span({ classes("pr") }) { Text("$ ") }; Text("ls -lt ~/blog") }
                        posts.forEach { p ->
                            A(BasePath.prependTo(p.route), {
                                classes("tl", "bb-row")
                                onClick { it.preventDefault(); router.tryRoutingTo(p.route) }
                            }) {
                                Span({ classes("dim") }) { Text("-rw-r--r--  ritwik  ${p.readingMinutes.toString().padStart(2)}min  ${p.date.padEnd(16)} ") }
                                Span({ classes("v") }) { Text(p.route.substringAfterLast("/") + ".md") }
                            }
                        }
                    }
                    AllPosts(router)
                }
                Variant.EDITORIAL -> {
                    SectionHead(v, 6, "Journal", "Notes from", "the field")
                    Div({ classes("bc-grid") }) {
                        posts.forEachIndexed { i, p ->
                            A(BasePath.prependTo(p.route), {
                                classes("bc-item")
                                if (i == 0) classes("lead")
                                reveal("up", i * 90)
                                onClick { it.preventDefault(); router.tryRoutingTo(p.route) }
                            }) {
                                Div({ classes("bc-meta") }) { Text("${p.date} — ${p.readingMinutes} min read") }
                                H3({ classes("bc-title") }) { Text(p.title) }
                                P({ classes("bc-desc") }) { Text(p.description) }
                                Span({ classes("bc-more") }) { Text("Read →") }
                            }
                        }
                    }
                    AllPosts(router)
                }
            }
        }
    }
}

private fun hue(p: BlogEntry) = (p.title.sumOf { it.code } * 37) % 360

@Composable
private fun AllPosts(router: com.varabyte.kobweb.navigation.Router) {
    Div({ classes("all-posts") }) {
        A(BasePath.prependTo("/blog"), { onClick { it.preventDefault(); router.tryRoutingTo("/blog") } }) { Text("All posts →") }
    }
}

// ---------------------------------------------------------------------------
// Contact CTA
// ---------------------------------------------------------------------------

@Composable
fun ContactSection() {
    SectionShell("contact") { v ->
        Div({ classes("wrap") }) {
            when (v) {
                Variant.MATERIAL -> {
                    Div({ classes("ca-card"); reveal("scale") }) {
                        Div({ classes("ca-glow") })
                        Span({ classes("rr-chip") }) { Span({ classes("rr-pulse") }); Text(Profile.availability) }
                        H2({ classes("ca-title") }) {
                            Text("Let's build ")
                            Span({ classes("grad") }) { Text("what's next.") }
                        }
                        P({ classes("ca-sub") }) { Text("Tech Lead, Senior Android or Forward Deployed roles — or just a good conversation about agentic apps.") }
                        ContactButtons()
                    }
                }
                Variant.TERMINAL -> {
                    TermWindow("ssh ritwik@hire") {
                        Div({ classes("tl") }) { Span({ classes("pr") }) { Text("$ ") }; Text("ssh ritwik@hire") }
                        Div({ classes("tl", "g") }) { Text("Connected. ${Profile.availability}.") }
                        Div({ classes("tl", "dim") }) { Text("Available commands:") }
                        Div({ classes("cb-cmds") }) {
                            A("mailto:${Profile.email}", { classes("cb-cmd") }) { Span({ classes("pr") }) { Text("mail ") }; Text(Profile.email) }
                            Button({ classes("cb-cmd"); onClick { UiState.contactOpen = true } }) { Span({ classes("pr") }) { Text("call ") }; Text("--reveal") }
                            A(resumeHref, { classes("cb-cmd"); attr("download", Profile.resumeFileName) }) { Span({ classes("pr") }) { Text("wget ") }; Text("resume.pdf") }
                            A(Profile.linkedIn, { classes("cb-cmd"); target(ATarget.Blank); attr("rel", "noopener") }) { Span({ classes("pr") }) { Text("open ") }; Text("linkedin") }
                        }
                        Div({ classes("tl") }) { Span({ classes("pr") }) { Text("$ ") }; Span({ classes("cursor") }) }
                    }
                }
                Variant.EDITORIAL -> {
                    Div({ classes("cc-wrap"); reveal("up") }) {
                        Div({ classes("sh-num") }) { Text("07"); Span({ classes("rule") }); Text("Contact") }
                        H2({ classes("cc-title") }) { Text("Say "); Em { Text("hello.") } }
                        Div({ classes("cc-links") }) {
                            A("mailto:${Profile.email}") { Text(Profile.email) }
                            A(Profile.linkedIn, { target(ATarget.Blank); attr("rel", "noopener") }) { Text("LinkedIn") }
                            A(resumeHref, { attr("download", Profile.resumeFileName) }) { Text("Résumé (PDF)") }
                            Button({ onClick { UiState.contactOpen = true } }) { Text("Phone") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactButtons() {
    Div({ classes("rr-cta", "ca-cta") }) {
        A(resumeHref, { classes("rr-btn", "primary"); attr("download", Profile.resumeFileName) }) {
            I({ classes("fa-solid", "fa-file-arrow-down") }); Text("Download résumé")
        }
        A("mailto:${Profile.email}", { classes("rr-btn") }) { I({ classes("fa-solid", "fa-envelope") }); Text("Email") }
        Button({ classes("rr-btn"); onClick { copyToClipboard(Profile.email, "Email") } }) { I({ classes("fa-regular", "fa-copy") }); Text("Copy email") }
        A(Profile.linkedIn, { classes("rr-btn"); target(ATarget.Blank); attr("rel", "noopener") }) { I({ classes("fa-brands", "fa-linkedin-in") }); Text("LinkedIn") }
    }
}
