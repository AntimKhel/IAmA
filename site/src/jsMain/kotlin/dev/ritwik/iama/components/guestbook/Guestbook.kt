package dev.ritwik.iama.components.guestbook

import androidx.compose.runtime.*
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import dev.ritwik.iama.data.Backend
import dev.ritwik.iama.util.Api
import dev.ritwik.iama.util.jsObject
import kotlinx.browser.window
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.onSubmit
import org.jetbrains.compose.web.attributes.maxLength
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.HTMLElement
import kotlin.js.Date

const val MAX_NAME = 40
const val MAX_MESSAGE = 280

class Note(val id: Int, val name: String, val message: String, val createdAt: Double)

/** Approved notes, shared by the home-page section and the phone's Guestbook app. */
object GuestbookStore {
    val notes = mutableStateListOf<Note>()
    var hasMore by mutableStateOf(false)
        private set
    var loading by mutableStateOf(false)
        private set
    var loaded by mutableStateOf(false)
        private set
    var failed by mutableStateOf(false)
        private set

    suspend fun loadFirst() {
        if (loaded || loading) return
        fetch(before = null)
    }

    suspend fun loadMore() {
        if (loading || !hasMore) return
        fetch(before = notes.lastOrNull()?.id)
    }

    private suspend fun fetch(before: Int?) {
        loading = true
        val res = Api.get(if (before == null) "/notes" else "/notes?before=$before")
        loading = false
        if (res?.ok != true) {
            failed = true
            return
        }
        failed = false
        loaded = true
        val page = (res.body.notes as Array<dynamic>).map {
            Note((it.id as Number).toInt(), it.name as String, it.message as String, (it.createdAt as Number).toDouble())
        }
        if (before == null) notes.clear()
        notes.addAll(page)
        hasMore = res.body.hasMore == true
    }
}

fun relativeTime(ms: Double): String {
    val mins = ((Date.now() - ms) / 60_000).toInt()
    return when {
        mins < 1 -> "just now"
        mins < 60 -> "${mins}m ago"
        mins < 60 * 24 -> "${mins / 60}h ago"
        mins < 60 * 24 * 30 -> "${mins / (60 * 24)}d ago"
        else -> Date(ms).asDynamic().toLocaleDateString(js("undefined"), js("({month: 'short', day: 'numeric', year: 'numeric'})")) as String
    }
}

/** Palette for sticky notes, picked deterministically per note. */
private val noteColors = listOf("green", "teal", "violet", "amber", "pink", "blue")

@Composable
fun NoteCard(note: Note, index: Int, cls: String = "gb-note") {
    Div({
        classes(*cls.split(" ").toTypedArray())
        classes("c-${noteColors[note.id % noteColors.size]}")
        style {
            property("--r", "${listOf(-2.2, 1.6, -1.1, 2.4, -0.6, 1.0)[note.id % 6]}deg")
            property("--i", index)
        }
    }) {
        P({ classes("gb-msg") }) { Text(note.message) }
        Div({ classes("gb-by") }) {
            Span({ classes("gb-name") }) { Text("— ${note.name}") }
            Span({ classes("gb-when") }) { Text(relativeTime(note.createdAt)) }
        }
    }
}

enum class FormStyle { CARD, TERMINAL, COMPACT }

private sealed interface SendState {
    data object Idle : SendState
    data object Sending : SendState
    data object Sent : SendState
    class Failed(val message: String) : SendState
}

/** Name + note + Turnstile + submit. Notes are stored as pending and appear once approved. */
@Composable
fun NoteForm(style: FormStyle = FormStyle.CARD) {
    var name by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var token by remember { mutableStateOf<String?>(null) }
    var turnstileKey by remember { mutableStateOf(0) }
    var state by remember { mutableStateOf<SendState>(SendState.Idle) }
    val scope = rememberCoroutineScope()

    val canSend = name.isNotBlank() && message.isNotBlank() && token != null && state != SendState.Sending

    fun send() {
        if (!canSend) return
        state = SendState.Sending
        scope.launch {
            val res = Api.post("/notes", jsObject("name" to name.trim(), "message" to message.trim(), "turnstileToken" to token))
            state = when {
                res == null -> SendState.Failed("Couldn't reach the guestbook. Please try again in a bit.")
                res.ok -> SendState.Sent
                else -> SendState.Failed(res.error ?: "Something went wrong.")
            }
            if (state == SendState.Sent) {
                name = ""
                message = ""
            }
            token = null
            turnstileKey++ // tokens are single-use
        }
    }

    Form(attrs = {
        classes("gb-form", "gb-form-${style.name.lowercase()}")
        onSubmit { it.preventDefault(); send() }
    }) {
        if (state == SendState.Sent) {
            Div({ classes("gb-sent"); attr("role", "status") }) {
                Span({ classes("gb-sent-ic") }) { I({ classes("fa-solid", "fa-check") }) }
                Div {
                    B { Text(if (style == FormStyle.TERMINAL) "200 OK — note queued for review" else "Thanks! Your note is on its way.") }
                    Div { Text("It'll appear on the wall once Ritwik approves it.") }
                }
            }
            Button({
                classes("gb-again")
                attr("type", "button")
                onClick { state = SendState.Idle }
            }) { Text("Leave another") }
            return@Form
        }

        Label(attrs = { classes("gb-field") }) {
            Span({ classes("gb-label") }) { Text(if (style == FormStyle.TERMINAL) "$ name=" else "Your name") }
            Input(InputType.Text) {
                value(name)
                maxLength(MAX_NAME)
                placeholder(if (style == FormStyle.TERMINAL) "\"Ada Lovelace\"" else "Ada Lovelace")
                attr("autocomplete", "name")
                onInput { name = it.value }
            }
        }
        Label(attrs = { classes("gb-field") }) {
            Span({ classes("gb-label") }) {
                Text(if (style == FormStyle.TERMINAL) "$ echo" else "Your note")
                Span({ classes("gb-count"); if (message.length > MAX_MESSAGE - 20) classes("warn") }) {
                    Text("${message.length}/$MAX_MESSAGE")
                }
            }
            TextArea(message) {
                maxLength(MAX_MESSAGE)
                attr("rows", if (style == FormStyle.COMPACT) "3" else "4")
                placeholder(if (style == FormStyle.TERMINAL) "\"Loved the R8 post…\" >> guestbook" else "Say hi, share feedback, or tell me what you're building…")
                onInput { message = it.value }
            }
        }

        // Inside the phone the screen is narrower than Turnstile's 300px flexible minimum.
        key(turnstileKey) { TurnstileBox(compact = style == FormStyle.COMPACT, onToken = { token = it }) }

        val failure = state as? SendState.Failed
        if (failure != null) Div({ classes("gb-error"); attr("role", "alert") }) { Text(failure.message) }

        Div({ classes("gb-actions") }) {
            Button({
                classes("gb-send")
                attr("type", "submit")
                if (!canSend) attr("disabled", "")
            }) {
                if (state == SendState.Sending) {
                    Span({ classes("gb-spinner") }); Text("Sending…")
                } else {
                    I({ classes("fa-solid", "fa-paper-plane") })
                    Text(if (style == FormStyle.TERMINAL) "submit --note" else "Leave note")
                }
            }
            Span({ classes("gb-hint") }) { Text("Notes appear after a quick review. No cookies, no tracking.") }
        }
    }
}

/** Cloudflare Turnstile, rendered explicitly once its script (deferred in <head>) is ready. */
@Composable
private fun TurnstileBox(compact: Boolean, onToken: (String?) -> Unit) {
    val dark = ColorMode.current.isDark
    Div({
        classes("gb-turnstile")
        ref { el ->
            var widgetId: dynamic = null
            var disposed = false
            fun mount(attempt: Int) {
                if (disposed) return
                val ts = window.asDynamic().turnstile
                if (ts == null) {
                    if (attempt < 100) window.setTimeout({ mount(attempt + 1) }, 100)
                    return
                }
                val opts = jsObject(
                    "sitekey" to Backend.turnstileSiteKey,
                    "theme" to if (dark) "dark" else "light",
                    "size" to if (compact) "compact" else "flexible",
                    "callback" to { t: String -> onToken(t) },
                    "expired-callback" to { onToken(null) },
                    "error-callback" to { onToken(null) },
                )
                widgetId = ts.render(el as HTMLElement, opts)
            }
            mount(0)
            onDispose {
                disposed = true
                if (widgetId != null) runCatching { window.asDynamic().turnstile.remove(widgetId) }
            }
        }
    })
}
