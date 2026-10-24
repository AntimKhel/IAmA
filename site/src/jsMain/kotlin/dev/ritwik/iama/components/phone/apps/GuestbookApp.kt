package dev.ritwik.iama.components.phone.apps

import androidx.compose.runtime.*
import dev.ritwik.iama.components.guestbook.FormStyle
import dev.ritwik.iama.components.guestbook.GuestbookStore
import dev.ritwik.iama.components.guestbook.NoteCard
import dev.ritwik.iama.components.guestbook.NoteForm
import dev.ritwik.iama.components.phone.AppBar
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.dom.*

@Composable
fun GuestbookApp() {
    LaunchedEffect(Unit) { GuestbookStore.loadFirst() }
    val scope = rememberCoroutineScope()
    AppBar("Guestbook", "Leave a note")
    Div({ classes("rr-a-scroll") }) {
        Div({ classes("rr-a-hero") }) {
            Div({ classes("k") }) { Text("Say hi") }
            Div({ classes("v") }) { Text("Notes show up on the wall after a quick review.") }
        }
        NoteForm(FormStyle.COMPACT)
        Heading("On the wall")
        if (GuestbookStore.loaded && GuestbookStore.notes.isEmpty()) {
            P({ classes("rr-a-p") }) { Text("No notes yet. Be the first!") }
        }
        GuestbookStore.notes.forEachIndexed { i, n -> NoteCard(n, i, "gb-note gb-note-app") }
        if (GuestbookStore.hasMore) {
            Button({ classes("rr-a-install", "ghost"); onClick { scope.launch { GuestbookStore.loadMore() } } }) {
                Text(if (GuestbookStore.loading) "Loading…" else "Load more")
            }
        }
    }
}
