package dev.ritwik.iama.components.phone.apps

import androidx.compose.runtime.*
import dev.ritwik.iama.blog.blogEntries
import dev.ritwik.iama.components.ask.Speech
import dev.ritwik.iama.components.ask.Voice
import dev.ritwik.iama.components.phone.AppBar
import dev.ritwik.iama.components.phone.Apps
import dev.ritwik.iama.components.phone.LauncherApp
import dev.ritwik.iama.components.phone.PhoneState
import dev.ritwik.iama.data.Profile
import dev.ritwik.iama.data.products
import dev.ritwik.iama.util.Api
import dev.ritwik.iama.util.AskTurn
import dev.ritwik.iama.util.Logcat
import dev.ritwik.iama.util.UiState
import dev.ritwik.iama.util.askRitwik
import dev.ritwik.iama.util.jsObject
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.maxLength
import org.jetbrains.compose.web.attributes.onSubmit
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.HTMLElement

/**
 * "Ask Ritwik": a chat that answers from what's published on this site (Cloudflare Workers AI, see
 * backend/src/ask.ts), plus Fit check (paste a job description). The conversation lives for the session, so
 * it survives closing the app or following one of its buttons.
 */
object AskState {
    enum class Mode { CHAT, FIT }

    private var seq = 0

    class Message(val mine: Boolean, text: String = "", val spoken: Boolean = false) {
        val id = ++seq
        var text by mutableStateOf(text)
        var actions by mutableStateOf(emptyList<String>())
        var pending by mutableStateOf(!mine)
        var error by mutableStateOf(false)
    }

    val messages = mutableStateListOf<Message>()
    var mode by mutableStateOf(Mode.CHAT)
    var busy by mutableStateOf(false)
        private set
    var remaining by mutableStateOf<Int?>(null)
        private set

    // Not tied to the app window: an answer keeps streaming if the visitor closes Ask meanwhile.
    private val scope = MainScope()

    /** [spoken] questions get their answer read aloud. */
    fun ask(question: String, spoken: Boolean = false) {
        val q = question.trim()
        if (q.isEmpty() || busy) return
        val history = messages.zipWithNext()
            .filter { (a, b) -> a.mine && !b.mine && !b.error && b.text.isNotBlank() }
            .takeLast(2)
            .map { (a, b) -> AskTurn(a.text, b.text) }
        messages += Message(mine = true, text = q, spoken = spoken)
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
            if (spoken && error == null && Speech.supported) Speech.speak(answer.id, answer.text)
        }
    }
}

/** Fit check: a job description in, requirements matched to evidence from the site out (backend/src/fit.ts). */
object FitState {
    class Requirement(val requirement: String, val status: String, val evidence: String, val actions: List<String>)
    class Result(val role: String, val summary: String, val requirements: List<Requirement>, val match: Int, val partial: Int, val gap: Int)

    var jd by mutableStateOf("")
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var result by mutableStateOf<Result?>(null)
    var remaining by mutableStateOf<Int?>(null); private set
    private val scope = MainScope()

    const val MIN = 80
    const val MAX = 6000

    fun run() {
        if (busy || jd.trim().length < MIN) return
        busy = true
        error = null
        scope.launch {
            val started = window.performance.now()
            val res = Api.postSlow("/fit", jsObject("jd" to jd.trim()))
            busy = false
            if (res == null) { error = "Couldn't reach Fit check. Check your connection and try again."; return@launch }
            if (!res.ok) { error = res.error ?: "Fit check couldn't run (${res.status}). Try again in a moment."; return@launch }
            val b = res.body
            val reqs = (b.requirements as Array<dynamic>).map {
                Requirement(it.requirement as String, it.status as String, it.evidence as String, (it.actions as Array<String>).toList())
            }
            result = Result(
                role = b.role as String, summary = b.summary as String, requirements = reqs,
                match = (b.counts.match as Number).toInt(), partial = (b.counts.partial as Number).toInt(), gap = (b.counts.gap as Number).toInt(),
            )
            remaining = (b.remaining as? Number)?.toInt()
            Logcat.i("FitCheck", "${reqs.size} requirements: ${result!!.match} match, ${result!!.partial} partial, ${result!!.gap} gap (${(window.performance.now() - started).toInt()} ms)")
        }
    }
}

private val starters = listOf(
    "What has Ritwik built at Jio?",
    "What's his experience with AI agents?",
    "Tell me about the Skoch award",
    "Is he open to new roles?",
)

/** A generic job description for trying Fit check (with one requirement the site doesn't cover). */
private const val SAMPLE_JD = """Senior Android Engineer / Tech Lead

You'll lead a small Android team building a consumer app used by millions.

What we're looking for:
- 6+ years building Android apps in Kotlin
- Deep experience with Jetpack Compose and modern Android architecture (MVVM)
- Has shipped React Native or other cross-platform features
- Experience building LLM-powered or agentic AI features
- Leads technical decisions and mentors engineers across teams
- Experience running backend services on Kubernetes in Go
- Cares about build performance and app size (Gradle, R8)"""

@Composable
fun AskApp() {
    // Opened from Search with a question, or from "Check fit" with "fit".
    LaunchedEffect(Unit) {
        when (val payload = PhoneState.payload) {
            "fit" -> AskState.mode = AskState.Mode.FIT
            is String -> { AskState.mode = AskState.Mode.CHAT; AskState.ask(payload) }
        }
    }
    DisposableEffect(Unit) { onDispose { Speech.stop(); Voice.stop(submit = false) } }

    AppBar("Ask Ritwik", if (AskState.mode == AskState.Mode.FIT) "Fit check: match a role to the site" else "AI answers from this site")
    Div({ classes("ask-tabs"); attr("role", "tablist") }) {
        listOf(AskState.Mode.CHAT to "Chat", AskState.Mode.FIT to "Fit check").forEach { (mode, label) ->
            Button({
                classes("ask-tab")
                if (AskState.mode == mode) classes("on")
                attr("role", "tab")
                attr("aria-selected", (AskState.mode == mode).toString())
                onClick { AskState.mode = mode }
            }) {
                I({ classes("fa-solid", if (mode == AskState.Mode.CHAT) "fa-comments" else "fa-clipboard-check") })
                Text(" $label")
            }
        }
    }
    if (AskState.mode == AskState.Mode.CHAT) ChatView() else FitView()
}

@Composable
private fun ChatView() {
    var draft by remember { mutableStateOf("") }
    var log by remember { mutableStateOf<HTMLElement?>(null) }
    val messages = AskState.messages

    // Follow the answer as it streams in.
    val last = messages.lastOrNull()
    LaunchedEffect(messages.size, last?.text?.length) {
        log?.let { it.scrollTop = it.scrollHeight.toDouble() }
    }

    Div({
        classes("rr-a-scroll", "ask-log")
        attr("aria-live", "polite")
        ref { el -> log = el; onDispose { log = null } }
    }) {
        Div({ classes("ask-intro") }) {
            Div({ classes("ask-avatar") }) { Text("R") }
            P {
                Text("Hi! I'm an AI that has read everything on this site. Ask me about ${Profile.firstName}'s work, ")
                Text("products, skills or blog posts${if (Voice.supported) ", by typing or with the mic" else ""}.")
            }
        }
        if (messages.isEmpty()) {
            Div({ classes("ask-starters") }) {
                starters.forEach { s ->
                    Button({ classes("ask-chip"); onClick { AskState.ask(s) } }) { Text(s) }
                }
                Button({ classes("ask-chip", "fit"); onClick { AskState.mode = AskState.Mode.FIT } }) {
                    I({ classes("fa-solid", "fa-clipboard-check") }); Text(" Hiring? Check fit with a job description")
                }
            }
        }
        messages.forEach { m -> MessageBubble(m) }
    }

    when (Voice.state) {
        Voice.State.RECORDING -> RecordingBar()
        Voice.State.TRANSCRIBING -> Div({ classes("ask-composer", "voice") }) {
            Span({ classes("ask-voice-spin") })
            Span({ classes("ask-voice-text") }) { Text("Transcribing…") }
        }
        Voice.State.IDLE -> Form(attrs = {
            classes("ask-composer")
            onSubmit { e ->
                e.preventDefault()
                if (!AskState.busy && draft.isNotBlank()) {
                    AskState.ask(draft)
                    draft = ""
                }
            }
        }) {
            if (Voice.supported) {
                Button({
                    classes("ask-mic")
                    attr("type", "button")
                    attr("aria-label", "Ask with your voice")
                    attr("title", "Ask with your voice")
                    if (AskState.busy) attr("disabled", "")
                    onClick {
                        Speech.stop()
                        Voice.start(onText = { AskState.ask(it, spoken = true) }, onError = { UiState.showToast(it) })
                    }
                }) { I({ classes("fa-solid", "fa-microphone") }) }
            }
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
    }
    Div({ classes("ask-note") }) {
        val left = AskState.remaining
        Text(if (left != null && left <= 5) "$left question${if (left == 1) "" else "s"} left today · " else "")
        Text("AI can be wrong; the résumé is the source of truth.")
    }
}

/** While recording: level meter, timer, cancel and send. */
@Composable
private fun RecordingBar() {
    Div({ classes("ask-composer", "voice", "rec") }) {
        Button({
            classes("ask-voice-cancel")
            attr("aria-label", "Cancel recording")
            onClick { Voice.stop(submit = false) }
        }) { I({ classes("fa-solid", "fa-xmark") }) }
        Span({ classes("ask-rec-dot"); attr("aria-hidden", "true") })
        Span({ classes("ask-voice-text") }) { Text("Listening… 0:${Voice.seconds.toString().padStart(2, '0')}") }
        Span({ classes("ask-meter"); attr("aria-hidden", "true") }) {
            listOf(0.55, 0.85, 1.0, 0.75, 0.5).forEach { k ->
                Span({ style { property("transform", "scaleY(${0.15 + Voice.level * k})") } })
            }
        }
        Button({
            classes("ask-send")
            attr("aria-label", "Stop and ask")
            onClick { Voice.stop() }
        }) { I({ classes("fa-solid", "fa-check") }) }
    }
}

@Composable
private fun MessageBubble(m: AskState.Message) {
    Div({
        classes("ask-msg", if (m.mine) "me" else "bot")
        if (m.error) classes("error")
    }) {
        when {
            m.mine -> {
                if (m.spoken) I({ classes("fa-solid", "fa-microphone", "ask-spoken"); attr("aria-label", "Spoken") })
                Text(m.text)
            }
            m.pending && m.text.isEmpty() -> Span({ classes("ask-typing"); attr("aria-label", "Thinking") }) {
                repeat(3) { Span() }
            }
            else -> Answer(m.text)
        }
    }
    if (!m.mine && !m.pending && !m.error) {
        Div({ classes("ask-actions") }) {
            if (Speech.supported && m.text.isNotBlank()) {
                val speaking = Speech.speakingId == m.id
                Button({
                    classes("ask-chip", "listen")
                    attr("aria-label", if (speaking) "Stop reading" else "Read aloud")
                    onClick { if (speaking) Speech.stop() else Speech.speak(m.id, m.text) }
                }) { I({ classes("fa-solid", if (speaking) "fa-stop" else "fa-volume-high") }) }
            }
            m.actions.mapNotNull(::resolve).forEach { (label, open) ->
                Button({ classes("ask-chip", "action"); onClick { open() } }) { Text(label) }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Fit check
// ---------------------------------------------------------------------------

private val fitSteps = listOf("Reading the job description…", "Matching it against Ritwik's work…", "Writing it up…")

@Composable
private fun FitView() {
    val result = FitState.result
    Div({ classes("rr-a-scroll", "fit") }) {
        when {
            FitState.busy -> FitProgress()
            result != null -> FitResultView(result)
            else -> {
                P({ classes("rr-a-p") }) {
                    Text("Hiring for a role? Paste the job description and see where ${Profile.firstName}'s work matches it, with links to the evidence, and what isn't on the site. Nothing you paste is stored.")
                }
                TextArea(FitState.jd) {
                    classes("fit-jd")
                    placeholder("Paste the job description here…")
                    attr("maxlength", "${FitState.MAX}")
                    attr("aria-label", "Job description")
                    attr("data-autofocus", "")
                    onInput { FitState.jd = it.value }
                }
                Div({ classes("fit-meta") }) {
                    Text("${FitState.jd.length.asDynamic().toLocaleString()} / 6,000")
                    if (FitState.jd.isBlank()) {
                        Button({ classes("fit-link"); onClick { FitState.jd = SAMPLE_JD } }) { Text("Try an example") }
                    }
                }
                FitState.error?.let { Div({ classes("ask-msg", "bot", "error", "fit-error") }) { Text(it) } }
                Div({ classes("rr-a-actions") }) {
                    Button({
                        classes("rr-a-install")
                        if (FitState.jd.trim().length < FitState.MIN) attr("disabled", "")
                        onClick { FitState.run() }
                    }) { I({ classes("fa-solid", "fa-clipboard-check") }); Text("Check fit") }
                }
            }
        }
    }
}

@Composable
private fun FitProgress() {
    var step by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (step < fitSteps.lastIndex) { delay(2600); step++ }
    }
    Div({ classes("fit-progress"); attr("aria-live", "polite") }) {
        fitSteps.forEachIndexed { i, s ->
            Div({ classes("fit-step"); if (i < step) classes("done"); if (i == step) classes("now") }) {
                I({ classes("fa-solid", if (i < step) "fa-circle-check" else "fa-circle-notch") })
                Text(" $s")
            }
        }
        repeat(4) { Div({ classes("fit-skeleton") }) }
    }
}

@Composable
private fun FitResultView(r: FitState.Result) {
    Div({ classes("fit-head") }) {
        Div({ classes("rr-a-ptitle") }) { Text(r.role.ifBlank { "Your role" }) }
        if (r.summary.isNotBlank()) P({ classes("rr-a-p") }) { Text(r.summary) }
        Div({ classes("fit-counts") }) {
            Span({ classes("fit-count", "match") }) { Text("${r.match} match") }
            Span({ classes("fit-count", "partial") }) { Text("${r.partial} partial") }
            Span({ classes("fit-count", "gap") }) { Text("${r.gap} not on the site") }
        }
    }
    Div({ classes("fit-list") }) {
        r.requirements.forEach { q ->
            Div({ classes("fit-req", q.status) }) {
                Span({ classes("fit-icon"); attr("aria-label", q.status) }) {
                    I({ classes("fa-solid", when (q.status) { "match" -> "fa-check"; "partial" -> "fa-circle-half-stroke"; else -> "fa-minus" }) })
                }
                Div({ classes("fit-body") }) {
                    Div({ classes("fit-name") }) { Text(q.requirement) }
                    Div({ classes("fit-evidence") }) {
                        Text(if (q.status == "gap") "Not covered on the site. Worth asking ${Profile.firstName} directly." else q.evidence)
                    }
                    if (q.actions.isNotEmpty()) {
                        Div({ classes("ask-actions") }) {
                            q.actions.mapNotNull(::resolve).forEach { (label, open) ->
                                Button({ classes("ask-chip", "action"); onClick { open() } }) { Text(label) }
                            }
                        }
                    }
                }
            }
        }
    }
    Div({ classes("rr-a-actions") }) {
        A("mailto:${Profile.email}?subject=${js("encodeURIComponent")("About the ${r.role.ifBlank { "role" }}")}", { classes("rr-a-install") }) {
            I({ classes("fa-solid", "fa-envelope") }); Text("Email ${Profile.firstName} about this role")
        }
        Button({ classes("rr-a-install", "ghost"); onClick { FitState.result = null } }) {
            I({ classes("fa-solid", "fa-rotate-left") }); Text("Check another")
        }
    }
    P({ classes("ask-note", "fit-note") }) {
        FitState.remaining?.let { Text("$it check${if (it == 1) "" else "s"} left today · ") }
        Text("AI can be wrong; every match links to what the site actually says.")
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
