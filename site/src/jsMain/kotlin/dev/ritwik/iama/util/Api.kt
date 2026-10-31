package dev.ritwik.iama.util

import dev.ritwik.iama.data.Backend
import kotlinx.browser.window
import kotlinx.coroutines.await
import org.w3c.fetch.RequestInit

/** Result of an API call; [body] is the parsed JSON (or null). */
class ApiResult(val status: Int, val body: dynamic) {
    val ok: Boolean get() = status in 200..299
    val error: String? get() = body?.error as? String
}

/**
 * Minimal JSON client for the portfolio Worker. Returns null when the API is not configured,
 * unreachable, or times out, so callers can quietly hide the feature.
 */
object Api {
    suspend fun get(path: String, token: String? = null) = request("GET", path, null, token)
    suspend fun post(path: String, body: dynamic = null, token: String? = null) = request("POST", path, body, token)
    suspend fun delete(path: String, token: String? = null) = request("DELETE", path, null, token)
    /** For slow endpoints (Fit check runs a model over a long prompt). */
    suspend fun postSlow(path: String, body: dynamic) = request("POST", path, body, null, timeoutMs = 45_000)

    private suspend fun request(method: String, path: String, body: dynamic, token: String?, timeoutMs: Int = 8000): ApiResult? {
        val base = Backend.apiBase ?: return null
        val controller: dynamic = js("new AbortController()")
        val timer = window.setTimeout({ controller.abort() }, timeoutMs)
        val started = window.performance.now()
        Logcat.d("OkHttp", "--> $method $path")
        return try {
            val headers: dynamic = js("({})")
            val init: dynamic = js("({})")
            init.method = method
            init.signal = controller.signal
            if (body != null) {
                headers["Content-Type"] = "application/json"
                init.body = JSON.stringify(body)
            }
            if (token != null) headers["Authorization"] = "Bearer $token"
            init.headers = headers
            val res = window.fetch(base + path, init.unsafeCast<RequestInit>()).await()
            val text = res.text().await()
            val ms = (window.performance.now() - started).toInt()
            val line = "<-- ${res.status} $path (${ms}ms, ${formatBytes(text.length)})"
            if (res.ok) Logcat.d("OkHttp", line) else Logcat.w("OkHttp", line)
            ApiResult(res.status.toInt(), if (text.isNotEmpty()) JSON.parse<dynamic>(text) else null)
        } catch (t: Throwable) {
            Logcat.w("OkHttp", "<-- HTTP FAILED $path: ${t.message ?: "network error"}")
            null
        } finally {
            window.clearTimeout(timer)
        }
    }
}

/** One question/answer pair sent back to Ask Ritwik as conversation context. */
class AskTurn(val question: String, val answer: String)

/**
 * Streams an Ask Ritwik answer: [onStart] gets the suggested actions (e.g. "app:work", "post:r8-story") and
 * the questions left today, then [onText] gets the answer as it arrives. Returns an error message, or null.
 */
suspend fun askRitwik(
    question: String,
    history: List<AskTurn>,
    onStart: (actions: List<String>, remaining: Int?) -> Unit,
    onText: (String) -> Unit,
): String? {
    val base = Backend.apiBase ?: return "Ask isn't available on this copy of the site."
    val controller: dynamic = js("new AbortController()")
    val timer = window.setTimeout({ controller.abort() }, 60_000)
    return try {
        val init: dynamic = js("({})")
        init.method = "POST"
        init.signal = controller.signal
        init.headers = jsObject("Content-Type" to "application/json")
        init.body = JSON.stringify(
            jsObject(
                "question" to question,
                "history" to history.map { jsObject("q" to it.question, "a" to it.answer) }.toTypedArray(),
            )
        )
        val started = window.performance.now()
        Logcat.d("OkHttp", "--> POST /ask")
        val res = window.fetch("$base/ask", init.unsafeCast<RequestInit>()).await()
        if (!res.ok) {
            val text = res.text().await()
            Logcat.w("OkHttp", "<-- ${res.status} /ask (${(window.performance.now() - started).toInt()}ms)")
            val error = runCatching { JSON.parse<dynamic>(text).error as? String }.getOrNull()
            return error ?: "Ask couldn't answer that (${res.status}). Try again in a moment."
        }
        val actions = (res.headers.get("X-Ask-Actions") ?: "").split(",").map { it.trim() }.filter { it.isNotEmpty() }
        Logcat.d("OkHttp", "<-- ${res.status} /ask (headers in ${(window.performance.now() - started).toInt()}ms, streaming)")
        onStart(actions, res.headers.get("X-Ask-Remaining")?.toIntOrNull())
        val reader = res.asDynamic().body.getReader()
        val decoder: dynamic = js("new TextDecoder()")
        var chars = 0
        var firstAt = 0.0
        while (true) {
            val chunk = (reader.read() as kotlin.js.Promise<dynamic>).await()
            if (chunk.done == true) break
            val text = decoder.decode(chunk.value, js("({ stream: true })")) as String
            if (firstAt == 0.0) firstAt = window.performance.now()
            chars += text.length
            onText(text)
        }
        val total = (window.performance.now() - started).toInt()
        Logcat.i("AskRitwik", "answer streamed: $chars chars, first words ${(firstAt - started).toInt()}ms, done ${total}ms · buttons ${actions.joinToString(",")}")
        null
    } catch (t: Throwable) {
        Logcat.w("OkHttp", "<-- HTTP FAILED /ask: ${t.message ?: "network error"}")
        "Couldn't reach Ask. Check your connection and try again."
    } finally {
        window.clearTimeout(timer)
    }
}

/** Sends recorded audio to Whisper (POST /transcribe). Returns the text, or an error message. */
suspend fun transcribeAudio(audio: dynamic, type: String): Pair<String?, String?> {
    val base = Backend.apiBase ?: return null to "Voice isn't available on this copy of the site."
    val started = window.performance.now()
    Logcat.d("OkHttp", "--> POST /transcribe (${formatBytes((audio.size as Number).toInt())} $type)")
    return try {
        val init: dynamic = js("({})")
        init.method = "POST"
        init.headers = jsObject("Content-Type" to type.substringBefore(";"))
        init.body = audio
        val res = window.fetch("$base/transcribe", init.unsafeCast<RequestInit>()).await()
        val body = JSON.parse<dynamic>(res.text().await())
        val ms = (window.performance.now() - started).toInt()
        if (res.ok) {
            Logcat.d("OkHttp", "<-- ${res.status} /transcribe (${ms}ms)")
            (body.text as? String) to null
        } else {
            Logcat.w("OkHttp", "<-- ${res.status} /transcribe (${ms}ms)")
            null to ((body.error as? String) ?: "Couldn't hear that. Try again?")
        }
    } catch (t: Throwable) {
        Logcat.w("OkHttp", "<-- HTTP FAILED /transcribe: ${t.message ?: "network error"}")
        null to "Couldn't reach the speech service. You can still type."
    }
}

/** Builds a plain JS object, e.g. `jsObject("name" to n, "message" to m)`. */
fun jsObject(vararg pairs: Pair<String, Any?>): dynamic {
    val o: dynamic = js("({})")
    pairs.forEach { (k, v) -> o[k] = v }
    return o
}
