package dev.ritwik.iama.components.phone.apps

import androidx.compose.runtime.*
import dev.ritwik.iama.blog.blogEntries
import dev.ritwik.iama.components.phone.AppBar
import dev.ritwik.iama.components.phone.Apps
import dev.ritwik.iama.components.phone.LauncherApp
import dev.ritwik.iama.components.phone.PhoneState
import dev.ritwik.iama.data.Profile
import dev.ritwik.iama.data.products
import dev.ritwik.iama.util.AskTurn
import dev.ritwik.iama.util.askRitwik
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.maxLength
import org.jetbrains.compose.web.attributes.onSubmit
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.HTMLElement

/**
 * "Ask Ritwik": a chat that answers from what's published on this site (Cloudflare Workers AI, see
 * backend/src/ask.ts). The conversation lives for the session, so it survives closing the app or
 * following one of its buttons.
 */
object AskState {
    class Message(val mine: Boolean, text: String = "") {
        var text by mutableStateOf(text)
        var actions by mutableStateOf(emptyList<String>())
        var pending by mutableStateOf(!mine)
        var error by mutableStateOf(false)
    }

    val messages = mutableStateListOf<Message>()
    var busy by mutableStateOf(false)
        private set
    var remaining by mutableStateOf<Int?>(null)
        private set

    // Not tied to the app window: an answer keeps streaming if the visitor closes Ask meanwhile.
    private val scope = MainScope()

    fun ask(question: String) {
        val q = question.trim()
        if (q.isEmpty() || busy) return
        val history = messages.zipWithNext()
            .filter { (a, b) -> a.mine && !b.mine && !b.error && b.text.isNotBlank() }
            .takeLast(2)
            .map { (a, b) -> AskTurn(a.text, b.text) }
        messages += Message(mine = true, text = q)
        val answer = Message(mine = false).also { messages += it }
        busy = true
        scope.launch {
            val error = askRitwik(
                q, history,
                onStart = { actions, left -> answer.actions = actions; remaining = left },
                onText = { answer.text += it },
            )
            if (error != null) {
                answer.text = error
                answer.error = true
                answer.actions = emptyList()
            }
            answer.pending = false
            busy = false
        }
    }
}

private val starters = listOf(
    "What has Ritwik built at Jio?",
    "What's his experience with AI agents?",
    "Tell me about the Skoch award",
    "Is he open to new roles?",
)

@Composable
fun AskApp() {
    var draft by remember { mutableStateOf("") }
    var log by remember { mutableStateOf<HTMLElement?>(null) }
    val messages = AskState.messages

    // Opened from Search with a question: ask it straight away.
    LaunchedEffect(Unit) {
        (PhoneState.payload as? String)?.let { AskState.ask(it) }
    }
    // Follow the answer as it streams in.
    val last = messages.lastOrNull()
    LaunchedEffect(messages.size, last?.text?.length) {
        log?.let { it.scrollTop = it.scrollHeight.toDouble() }
    }

    AppBar("Ask Ritwik", "AI answers from this site")
    Div({
        classes("rr-a-scroll", "ask-log")
        attr("aria-live", "polite")
        ref { el -> log = el; onDispose { log = null } }
    }) {
        Div({ classes("ask-intro") }) {
            Div({ classes("ask-avatar") }) { Text("R") }
            P {
                Text("Hi! I'm an AI that has read everything on this site. Ask me about ${Profile.firstName}'s work, ")
                Text("products, skills or blog posts.")
            }
        }
        if (messages.isEmpty()) {
            Div({ classes("ask-starters") }) {
                starters.forEach { s ->
                    Button({ classes("ask-chip"); onClick { AskState.ask(s) } }) { Text(s) }
                }
            }
        }
        messages.forEach { m -> MessageBubble(m) }
    }

    Form(attrs = {
        classes("ask-composer")
        onSubmit { e ->
            e.preventDefault()
            if (!AskState.busy && draft.isNotBlank()) {
                AskState.ask(draft)
                draft = ""
            }
        }
    }) {
        Input(InputType.Text) {
            value(draft)
            placeholder("Ask about ${Profile.firstName}'s work…")
            maxLength(300)
            attr("aria-label", "Your question")
            attr("autocomplete", "off")
            attr("enterkeyhint", "send")
            attr("data-autofocus", "")
            onInput { draft = it.value }
        }
        Button({
            classes("ask-send")
            attr("type", "submit")
            attr("aria-label", "Send")
            if (AskState.busy || draft.isBlank()) attr("disabled", "")
        }) { I({ classes("fa-solid", "fa-arrow-up") }) }
    }
    Div({ classes("ask-note") }) {
        val left = AskState.remaining
        Text(
            if (left != null && left <= 5) "$left question${if (left == 1) "" else "s"} left today · "
            else ""
        )
        Text("AI can be wrong; the résumé is the source of truth.")
    }
}

@Composable
private fun MessageBubble(m: AskState.Message) {
    Div({
        classes("ask-msg", if (m.mine) "me" else "bot")
        if (m.error) classes("error")
    }) {
        when {
            m.mine -> Text(m.text)
            m.pending && m.text.isEmpty() -> Span({ classes("ask-typing"); attr("aria-label", "Thinking") }) {
                repeat(3) { Span() }
            }
            else -> Answer(m.text)
        }
    }
    if (!m.mine && !m.pending && m.actions.isNotEmpty()) {
        Div({ classes("ask-actions") }) {
            m.actions.mapNotNull(::resolve).forEach { (label, open) ->
                Button({ classes("ask-chip", "action"); onClick { open() } }) { Text(label) }
            }
        }
    }
}

/** Plain-text answer with the little formatting small models use anyway: "- " bullets and **bold**. */
@Composable
private fun Answer(text: String) {
    val lines = text.trim().lines()
    var i = 0
    while (i < lines.size) {
        val line = lines[i].trim()
        if (line.startsWith("- ") || line.startsWith("* ") || line.startsWith("• ")) {
            val items = mutableListOf<String>()
            while (i < lines.size && lines[i].trim().let { it.startsWith("- ") || it.startsWith("* ") || it.startsWith("• ") }) {
                items += lines[i].trim().drop(2)
                i++
            }
            Ul { items.forEach { Li { Inline(it) } } }
        } else {
            if (line.isNotEmpty()) P { Inline(line) }
            i++
        }
    }
}

@Composable
private fun Inline(text: String) {
    text.split("**").forEachIndexed { i, part -> if (i % 2 == 1) B { Text(part) } else Text(part) }
}

/** Turns an action from the Worker ("app:work", "product:myjio", "post:r8-story", "resume") into a button. */
private fun resolve(action: String): Pair<String, () -> Unit>? {
    val kind = action.substringBefore(":")
    val id = action.substringAfter(":", "")
    fun open(app: LauncherApp, payload: Any? = null): () -> Unit = { PhoneState.open(app, PhoneState.host, null, payload) }
    return when (kind) {
        "app" -> when (id) {
            "work" -> "Open Work" to open(Apps.work)
            "about" -> "Open About" to open(Apps.about)
            "skills" -> "Open Skills" to open(Apps.skills)
            "awards" -> "Open Awards" to open(Apps.awards)
            "contact" -> "Contact Ritwik" to open(Apps.contact)
            "blog" -> "Open Blog" to open(Apps.blog)
            else -> null
        }
        "resume" -> "Résumé" to open(Apps.resume)
        "product" -> products.firstOrNull { it.id == id }?.let { p -> p.name to open(Apps.products, p) }
        "post" -> blogEntries.firstOrNull { it.route.endsWith("/$id") }?.let { post ->
            val title = post.title.substringBefore(":").substringBefore(" — ").take(34)
            "Read: $title" to open(Apps.blog, post)
        }
        else -> null
    }
}
