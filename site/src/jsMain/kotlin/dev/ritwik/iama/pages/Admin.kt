package dev.ritwik.iama.pages

import androidx.compose.runtime.*
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.data.add
import com.varabyte.kobweb.core.init.InitRoute
import com.varabyte.kobweb.core.init.InitRouteContext
import com.varabyte.kobweb.core.layout.Layout
import dev.ritwik.iama.components.guestbook.Note
import dev.ritwik.iama.components.guestbook.relativeTime
import dev.ritwik.iama.components.layouts.PageLayoutData
import dev.ritwik.iama.data.Backend
import dev.ritwik.iama.util.Api
import dev.ritwik.iama.util.UiState
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.onSubmit
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.*

@InitRoute
fun initAdminPage(ctx: InitRouteContext) {
    ctx.data.add(PageLayoutData("Guestbook admin"))
}

private const val TOKEN_KEY = "rr-admin-token"

/**
 * Guestbook moderation, for Ritwik only. Not linked anywhere and marked noindex; every action is
 * authorised server-side with ADMIN_TOKEN, which is kept only in this browser's localStorage.
 */
@Page
@Layout(".components.layouts.PageLayout")
@Composable
fun AdminPage() {
    var token by remember { mutableStateOf(runCatching { window.localStorage.getItem(TOKEN_KEY) }.getOrNull() ?: "") }
    var draft by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf("pending") }
    var notes by remember { mutableStateOf<List<Note>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val meta = document.createElement("meta")
        meta.setAttribute("name", "robots")
        meta.setAttribute("content", "noindex, nofollow")
        document.head?.appendChild(meta)
    }

    LaunchedEffect(token, tab, refresh) {
        if (token.isEmpty()) return@LaunchedEffect
        notes = null
        val res = Api.get("/admin/notes?status=$tab", token)
        when {
            res == null -> error = "Couldn't reach the API."
            res.status == 401 -> {
                error = "That token was rejected."
                token = ""
                runCatching { window.localStorage.removeItem(TOKEN_KEY) }
            }
            res.ok -> {
                error = null
                notes = (res.body.notes as Array<dynamic>).map {
                    Note((it.id as Number).toInt(), it.name as String, it.message as String, (it.createdAt as Number).toDouble())
                }
            }
            else -> error = res.error ?: "Request failed (${res.status})."
        }
    }

    fun act(note: Note, approve: Boolean) {
        scope.launch {
            val res = if (approve) Api.post("/admin/notes/${note.id}/approve", token = token) else Api.delete("/admin/notes/${note.id}", token)
            if (res?.ok == true) {
                notes = notes?.filter { it.id != note.id }
                UiState.showToast(if (approve) "Approved — it's on the wall" else "Deleted")
            } else {
                UiState.showToast(res?.error ?: "Action failed")
            }
        }
    }

    Section({ classes("admin-page") }) {
        Div({ classes("post-wrap") }) {
            H1({ classes("admin-title") }) { Text("Guestbook admin") }
            if (!Backend.enabled) {
                P({ classes("admin-muted") }) { Text("The backend isn't configured for this site yet (see backend/setup.sh).") }
                return@Div
            }
            if (token.isEmpty()) {
                Form(attrs = {
                    classes("admin-login")
                    onSubmit {
                        it.preventDefault()
                        if (draft.isNotBlank()) {
                            token = draft.trim()
                            runCatching { window.localStorage.setItem(TOKEN_KEY, token) }
                            draft = ""
                        }
                    }
                }) {
                    Input(InputType.Password) {
                        value(draft)
                        placeholder("Admin token")
                        attr("autocomplete", "current-password")
                        onInput { draft = it.value }
                    }
                    Button({ classes("rr-btn", "primary"); attr("type", "submit") }) { Text("Unlock") }
                }
                if (error != null) P({ classes("gb-error") }) { Text(error!!) }
                return@Div
            }

            Div({ classes("admin-bar") }) {
                Div({ classes("blog-tags") }) {
                    listOf("pending" to "Pending", "approved" to "Approved").forEach { (id, label) ->
                        Button({ if (tab == id) classes("on"); onClick { tab = id } }) { Text(label) }
                    }
                }
                Button({ classes("rr-nav-icon"); attr("title", "Refresh"); onClick { refresh++ } }) { I({ classes("fa-solid", "fa-rotate") }) }
                Button({
                    classes("admin-lock")
                    onClick {
                        token = ""
                        runCatching { window.localStorage.removeItem(TOKEN_KEY) }
                    }
                }) { I({ classes("fa-solid", "fa-lock") }); Text("Lock") }
            }

            if (error != null) P({ classes("gb-error") }) { Text(error!!) }
            val list = notes
            when {
                list == null -> P({ classes("admin-muted") }) { Text("Loading…") }
                list.isEmpty() -> P({ classes("admin-muted") }) { Text(if (tab == "pending") "Inbox zero. No notes waiting." else "Nothing approved yet.") }
                else -> Div({ classes("admin-list") }) {
                    list.forEach { n ->
                        Div({ classes("admin-item") }) {
                            Div({ classes("admin-meta") }) { B { Text(n.name) }; Text(" · ${relativeTime(n.createdAt)} · #${n.id}") }
                            P({ classes("admin-msg") }) { Text(n.message) }
                            Div({ classes("admin-actions") }) {
                                if (tab == "pending") {
                                    Button({ classes("rr-btn", "primary"); onClick { act(n, approve = true) } }) {
                                        I({ classes("fa-solid", "fa-check") }); Text("Approve")
                                    }
                                }
                                Button({ classes("rr-btn"); onClick { act(n, approve = false) } }) {
                                    I({ classes("fa-solid", "fa-trash") }); Text("Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
