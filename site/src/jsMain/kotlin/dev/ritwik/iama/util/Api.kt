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

    private suspend fun request(method: String, path: String, body: dynamic, token: String?, timeoutMs: Int = 8000): ApiResult? {
        val base = Backend.apiBase ?: return null
        val controller: dynamic = js("new AbortController()")
        val timer = window.setTimeout({ controller.abort() }, timeoutMs)
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
            ApiResult(res.status.toInt(), if (text.isNotEmpty()) JSON.parse<dynamic>(text) else null)
        } catch (_: Throwable) {
            null
        } finally {
            window.clearTimeout(timer)
        }
    }
}

/** Builds a plain JS object, e.g. `jsObject("name" to n, "message" to m)`. */
fun jsObject(vararg pairs: Pair<String, Any?>): dynamic {
    val o: dynamic = js("({})")
    pairs.forEach { (k, v) -> o[k] = v }
    return o
}
