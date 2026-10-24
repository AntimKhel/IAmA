package dev.ritwik.iama.components.sections

import androidx.compose.runtime.*
import dev.ritwik.iama.components.devmode.Variant
import dev.ritwik.iama.components.guestbook.FormStyle
import dev.ritwik.iama.components.guestbook.GuestbookStore
import dev.ritwik.iama.components.guestbook.NoteCard
import dev.ritwik.iama.components.guestbook.NoteForm
import dev.ritwik.iama.data.Backend
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.dom.*
import kotlin.js.Date

/** "Leave a note": form + wall of approved notes. Hidden entirely until the backend is configured. */
@Composable
fun GuestbookSection() {
    if (!Backend.enabled) return
    LaunchedEffect(Unit) { GuestbookStore.loadFirst() }
    val scope = rememberCoroutineScope()

    SectionShell("guestbook") { v ->
        Div({ classes("wrap") }) {
            when (v) {
                Variant.MATERIAL -> {
                    SectionHead(v, 7, "Guestbook", "Leave a", "note", "Say hi, share feedback, or tell me what you're building. Notes appear here after a quick review.")
                    Div({ classes("gb-layout") }) {
                        Div({ classes("gb-form-wrap"); reveal("left") }) { NoteForm(FormStyle.CARD) }
                        Div({ classes("gb-wall-wrap") }) {
                            Wall()
                            if (GuestbookStore.hasMore) {
                                Button({
                                    classes("gb-more")
                                    onClick { scope.launch { GuestbookStore.loadMore() } }
                                }) { Text(if (GuestbookStore.loading) "Loading…" else "Load more notes") }
                            }
                        }
                    }
                }
                Variant.TERMINAL -> {
                    SectionHead(v, 7, "Guestbook", "Leave a", "note", command = "guestbook")
                    Div({ classes("gb-term-layout") }) {
                        TermWindow("tail -n 30 guestbook.log") {
                            Div({ classes("tl") }) { Span({ classes("pr") }) { Text("$ ") }; Text("tail -n 30 guestbook.log") }
                            when {
                                GuestbookStore.failed -> Div({ classes("tl", "dim") }) { Text("tail: guestbook.log: connection refused") }
                                GuestbookStore.notes.isEmpty() -> Div({ classes("tl", "dim") }) { Text(if (GuestbookStore.loaded) "(empty — be the first)" else "loading…") }
                                else -> GuestbookStore.notes.forEach { n ->
                                    Div({ classes("tl") }) {
                                        Span({ classes("hash") }) { Text("[${Date(n.createdAt).toISOString().take(10)}] ") }
                                        Span({ classes("v") }) { Text(n.name) }
                                        Text(": ${n.message}")
                                    }
                                }
                            }
                        }
                        TermWindow("vim note.txt", "gb-term-form") { NoteForm(FormStyle.TERMINAL) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Wall() {
    when {
        GuestbookStore.failed && GuestbookStore.notes.isEmpty() -> Div({ classes("gb-empty") }) {
            I({ classes("fa-solid", "fa-plug-circle-xmark") })
            Text("The wall is taking a nap. Notes will be back shortly.")
        }
        GuestbookStore.loaded && GuestbookStore.notes.isEmpty() -> Div({ classes("gb-empty") }) {
            I({ classes("fa-solid", "fa-feather-pointed") })
            Text("No notes yet. Be the first to leave one!")
        }
        else -> Div({ classes("gb-wall") }) {
            GuestbookStore.notes.forEachIndexed { i, n -> NoteCard(n, i) }
        }
    }
}

