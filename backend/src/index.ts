import { ASK_LIMITS, buildMessages, loadKnowledge, retrieve, sseToText, type Knowledge, type Turn } from "./ask";

/**
 * iama-api — visitor counter + guestbook for https://antimkhel.github.io/IAmA/
 *
 * Public:
 *   POST /visit                      count this visitor once per day, returns { visitors, counted }
 *   GET  /stats                      { visitors }
 *   GET  /notes?before=<id>          approved notes, newest first, paged
 *   POST /notes                      { name, message, turnstileToken } → stored as pending (202)
 *   POST /ask                        { question, history } → streamed plain-text answer ("Ask Ritwik", Workers AI)
 * Admin (Authorization: Bearer <ADMIN_TOKEN>):
 *   GET    /admin/notes?status=pending|approved
 *   POST   /admin/notes/:id/approve
 *   DELETE /admin/notes/:id
 *
 * Privacy: no cookies and no raw IPs. Visitors are counted with a salted daily hash that is deleted after a
 * day; notes keep a salted IP hash only for rate limiting.
 */

export interface Env {
  DB: D1Database;
  ALLOWED_ORIGINS: string;
  TURNSTILE_SECRET: string;
  ADMIN_TOKEN: string;
  HASH_SALT: string;
  AI: Ai;
  ASK_MODEL: string;
  KNOWLEDGE_URL: string;
}

export const LIMITS = {
  name: 40,
  message: 280,
  notesPerDay: 3,
  page: 30,
  bodyBytes: 4096,
} as const;

const TURNSTILE_VERIFY = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
const DAY_MS = 24 * 60 * 60 * 1000;

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const cors = corsHeaders(request.headers.get("Origin"), env);
    if (request.method === "OPTIONS") return new Response(null, { status: 204, headers: cors });

    let response: Response;
    try {
      response = await route(request, env);
    } catch (err) {
      console.error("unhandled", err);
      response = json({ error: "Something went wrong." }, 500);
    }
    for (const [k, v] of Object.entries(cors)) response.headers.set(k, v);
    return response;
  },

  async scheduled(_event: ScheduledController, env: Env): Promise<void> {
    await env.DB.prepare("DELETE FROM visits WHERE day < ?").bind(dayKey(-1)).run();
    await env.DB.prepare("DELETE FROM ask_usage WHERE day < ?").bind(dayKey(-1)).run();
    await env.DB.prepare("DELETE FROM counters WHERE name LIKE 'ask:%' AND name < ?").bind(`ask:${dayKey(-1)}`).run();
  },
};

async function route(request: Request, env: Env): Promise<Response> {
  const url = new URL(request.url);
  const path = url.pathname.replace(/\/+$/, "") || "/";
  const method = request.method;

  if (path === "/visit" && method === "POST") return visit(request, env);
  if (path === "/stats" && method === "GET") return stats(env);
  if (path === "/notes" && method === "GET") return listNotes(url, env);
  if (path === "/notes" && method === "POST") return createNote(request, env);
  if (path === "/ask" && method === "POST") return ask(request, env);

  if (path.startsWith("/admin/")) {
    if (!(await isAdmin(request, env))) return json({ error: "Unauthorized" }, 401);
    if (path === "/admin/notes" && method === "GET") return adminList(url, env);
    const m = path.match(/^\/admin\/notes\/(\d+)(\/approve)?$/);
    if (m && method === "POST" && m[2]) return approve(Number(m[1]), env);
    if (m && method === "DELETE" && !m[2]) return remove(Number(m[1]), env);
  }

  return json({ error: "Not found" }, 404);
}

// ---------------------------------------------------------------------------
// Visitor counter
// ---------------------------------------------------------------------------

async function visit(request: Request, env: Env): Promise<Response> {
  const day = dayKey(0);
  const hash = await sha256(`${clientIp(request)}|${request.headers.get("User-Agent") ?? ""}|${day}|${env.HASH_SALT}`);
  const inserted = await env.DB.prepare("INSERT OR IGNORE INTO visits (day, hash) VALUES (?, ?)").bind(day, hash).run();
  const counted = (inserted.meta.changes ?? 0) > 0;
  if (counted) {
    await env.DB.prepare("UPDATE counters SET value = value + 1 WHERE name = 'unique_visitors'").run();
  }
  return json({ visitors: await readVisitors(env), counted }, 200, { "Cache-Control": "no-store" });
}

async function stats(env: Env): Promise<Response> {
  return json({ visitors: await readVisitors(env) }, 200, { "Cache-Control": "public, max-age=60" });
}

async function readVisitors(env: Env): Promise<number> {
  const row = await env.DB.prepare("SELECT value FROM counters WHERE name = 'unique_visitors'").first<{ value: number }>();
  return row?.value ?? 0;
}

// ---------------------------------------------------------------------------
// Guestbook
// ---------------------------------------------------------------------------

interface NoteRow {
  id: number;
  name: string;
  message: string;
  created_at: number;
  status?: string;
}

const toNote = (r: NoteRow) => ({ id: r.id, name: r.name, message: r.message, createdAt: r.created_at, status: r.status });

async function listNotes(url: URL, env: Env): Promise<Response> {
  const before = Number(url.searchParams.get("before")) || Number.MAX_SAFE_INTEGER;
  const { results } = await env.DB.prepare(
    "SELECT id, name, message, created_at FROM notes WHERE status = 'approved' AND id < ? ORDER BY id DESC LIMIT ?",
  )
    .bind(before, LIMITS.page + 1)
    .all<NoteRow>();
  const page = results.slice(0, LIMITS.page).map(toNote);
  return json({ notes: page, hasMore: results.length > LIMITS.page }, 200, { "Cache-Control": "public, max-age=30" });
}

async function createNote(request: Request, env: Env): Promise<Response> {
  const raw = await request.text();
  if (raw.length > LIMITS.bodyBytes) return json({ error: "That's a bit long." }, 413);

  let body: { name?: unknown; message?: unknown; turnstileToken?: unknown };
  try {
    body = JSON.parse(raw);
  } catch {
    return json({ error: "Invalid request." }, 400);
  }

  const name = cleanLine(body.name);
  const message = cleanMessage(body.message);
  if (!name || name.length > LIMITS.name) return json({ error: `Please add a name (up to ${LIMITS.name} characters).` }, 400);
  if (!message || message.length > LIMITS.message) {
    return json({ error: `Notes can be 1–${LIMITS.message} characters.` }, 400);
  }
  if ((message.match(/https?:\/\/|www\./gi) ?? []).length > 1) {
    return json({ error: "One link per note, please." }, 400);
  }

  const ip = clientIp(request);
  if (!(await verifyTurnstile(body.turnstileToken, ip, env))) {
    return json({ error: "Couldn't verify you're human. Please try again." }, 403);
  }

  const ipHash = await sha256(`${ip}|${env.HASH_SALT}`);
  const recent = await env.DB.prepare("SELECT COUNT(*) AS n FROM notes WHERE ip_hash = ? AND created_at > ?")
    .bind(ipHash, Date.now() - DAY_MS)
    .first<{ n: number }>();
  if ((recent?.n ?? 0) >= LIMITS.notesPerDay) {
    return json({ error: "Thanks for the enthusiasm! Try again tomorrow." }, 429);
  }

  await env.DB.prepare("INSERT INTO notes (name, message, created_at, status, ip_hash) VALUES (?, ?, ?, 'pending', ?)")
    .bind(name, message, Date.now(), ipHash)
    .run();
  return json({ status: "pending" }, 202);
}

/** Single line: no control characters, collapsed whitespace. */
export function cleanLine(value: unknown): string {
  if (typeof value !== "string") return "";
  return value.replace(/[\p{Cc}\p{Cf}]/gu, " ").replace(/\s+/g, " ").trim();
}

/** Multi-line note: keeps line breaks (at most one blank line in a row), strips other control characters. */
export function cleanMessage(value: unknown): string {
  if (typeof value !== "string") return "";
  return value
    .replace(/\r\n?/g, "\n")
    .replace(/[^\S\n]+/g, " ")
    .replace(/[\p{Cc}\p{Cf}]/gu, (c) => (c === "\n" ? c : ""))
    .replace(/\n{3,}/g, "\n\n")
    .split("\n")
    .map((l) => l.trim())
    .join("\n")
    .trim();
}

async function verifyTurnstile(token: unknown, ip: string, env: Env): Promise<boolean> {
  if (typeof token !== "string" || !token || token.length > 2048 || !env.TURNSTILE_SECRET) return false;
  const form = new FormData();
  form.append("secret", env.TURNSTILE_SECRET);
  form.append("response", token);
  if (ip) form.append("remoteip", ip);
  try {
    const res = await fetch(TURNSTILE_VERIFY, { method: "POST", body: form });
    const data = (await res.json()) as { success?: boolean };
    return data.success === true;
  } catch (err) {
    console.error("turnstile", err);
    return false;
  }
}

// ---------------------------------------------------------------------------
// Ask Ritwik
// ---------------------------------------------------------------------------

async function ask(request: Request, env: Env): Promise<Response> {
  // Only the site itself may ask (keeps casual scripts off the shared daily budget).
  if (!isAllowedOrigin(request.headers.get("Origin"), env)) return json({ error: "Forbidden" }, 403);

  const raw = await request.text();
  if (raw.length > ASK_LIMITS.bodyBytes) return json({ error: "That's a bit long." }, 413);
  let body: { question?: unknown; history?: unknown };
  try {
    body = JSON.parse(raw);
  } catch {
    return json({ error: "Invalid request." }, 400);
  }
  const question = cleanLine(body.question);
  if (question.length < 2 || question.length > ASK_LIMITS.question) {
    return json({ error: `Questions can be up to ${ASK_LIMITS.question} characters.` }, 400);
  }
  const history: Turn[] = (Array.isArray(body.history) ? body.history : [])
    .slice(-ASK_LIMITS.history)
    .map((t: { q?: unknown; a?: unknown }) => ({
      q: cleanLine(t?.q).slice(0, ASK_LIMITS.question),
      a: cleanMessage(t?.a).slice(0, ASK_LIMITS.historyAnswer),
    }))
    .filter((t) => t.q && t.a);

  const day = dayKey(0);
  const visitor = await sha256(`${clientIp(request)}|${request.headers.get("User-Agent") ?? ""}|${day}|${env.HASH_SALT}`);
  const mine = await env.DB.prepare(
    "INSERT INTO ask_usage (day, hash, count) VALUES (?, ?, 1) ON CONFLICT (day, hash) DO UPDATE SET count = count + 1 RETURNING count",
  )
    .bind(day, visitor)
    .first<{ count: number }>();
  const used = mine?.count ?? 1;
  if (used > ASK_LIMITS.perVisitorPerDay) {
    return json({ error: "That's all the questions for today. Email Ritwik at raj.ritwik96@gmail.com, he'd be glad to hear from you." }, 429);
  }
  const total = await env.DB.prepare(
    "INSERT INTO counters (name, value) VALUES (?, 1) ON CONFLICT (name) DO UPDATE SET value = value + 1 RETURNING value",
  )
    .bind(`ask:${day}`)
    .first<{ value: number }>();
  if ((total?.value ?? 1) > ASK_LIMITS.perDay) {
    return json({ error: "Ask is resting for today. Try Search, or email Ritwik at raj.ritwik96@gmail.com." }, 503);
  }

  let kb: Knowledge;
  try {
    kb = await loadKnowledge(env.KNOWLEDGE_URL);
  } catch (err) {
    console.error("knowledge", err);
    return json({ error: "Ask can't reach its notes right now. Try Search, or email Ritwik at raj.ritwik96@gmail.com." }, 503);
  }
  const lastQuestion = history.at(-1)?.q ?? "";
  const { context, actions } = retrieve(kb, `${question} ${lastQuestion}`);
  let stream: ReadableStream<Uint8Array>;
  try {
    stream = (await env.AI.run(env.ASK_MODEL as keyof AiModels, {
      messages: buildMessages(question, history, context),
      max_tokens: ASK_LIMITS.maxTokens,
      temperature: 0.3,
      stream: true,
      // The default models "think" first, which spends the token (and free neuron) budget on hidden text.
      chat_template_kwargs: { enable_thinking: false },
    } as never)) as unknown as ReadableStream<Uint8Array>;
  } catch (err) {
    // On the free plan this is usually the daily allocation running out.
    console.error("ai", err);
    return json({ error: "Ask is resting for today. Try Search, or email Ritwik at raj.ritwik96@gmail.com." }, 503);
  }

  const toText = sseToText((u) =>
    console.log(JSON.stringify({ ask: "usage", model: env.ASK_MODEL, in: u.prompt_tokens, out: u.completion_tokens, neurons: u.neurons })),
  );
  return new Response(stream.pipeThrough(toText), {
    headers: {
      // The body is plain text, but labelled as an event stream: Cloudflare compresses text/plain, and
      // compression buffers the whole answer, which would then arrive all at once instead of word by word.
      "Content-Type": "text/event-stream; charset=utf-8",
      "Cache-Control": "no-store, no-transform",
      "X-Ask-Actions": actions.join(","),
      "X-Ask-Remaining": String(Math.max(0, ASK_LIMITS.perVisitorPerDay - used)),
    },
  });
}

// ---------------------------------------------------------------------------
// Admin
// ---------------------------------------------------------------------------

async function isAdmin(request: Request, env: Env): Promise<boolean> {
  const expected = env.ADMIN_TOKEN ?? "";
  if (expected.length < 16) return false; // refuse weak or missing tokens outright
  const given = (request.headers.get("Authorization") ?? "").replace(/^Bearer\s+/i, "");
  const a = new TextEncoder().encode(given);
  const b = new TextEncoder().encode(expected);
  if (a.byteLength !== b.byteLength) return false;
  return crypto.subtle.timingSafeEqual(a, b);
}

async function adminList(url: URL, env: Env): Promise<Response> {
  const status = url.searchParams.get("status") === "approved" ? "approved" : "pending";
  const { results } = await env.DB.prepare(
    "SELECT id, name, message, created_at, status FROM notes WHERE status = ? ORDER BY id DESC LIMIT 200",
  )
    .bind(status)
    .all<NoteRow>();
  return json({ notes: results.map(toNote) }, 200, { "Cache-Control": "no-store" });
}

async function approve(id: number, env: Env): Promise<Response> {
  const r = await env.DB.prepare("UPDATE notes SET status = 'approved' WHERE id = ?").bind(id).run();
  return (r.meta.changes ?? 0) > 0 ? json({ ok: true }) : json({ error: "Not found" }, 404);
}

async function remove(id: number, env: Env): Promise<Response> {
  const r = await env.DB.prepare("DELETE FROM notes WHERE id = ?").bind(id).run();
  return (r.meta.changes ?? 0) > 0 ? json({ ok: true }) : json({ error: "Not found" }, 404);
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function isAllowedOrigin(origin: string | null, env: Env): boolean {
  const allowed = (env.ALLOWED_ORIGINS ?? "").split(",").map((s) => s.trim()).filter(Boolean);
  return !!origin && allowed.includes(origin);
}

function corsHeaders(origin: string | null, env: Env): Record<string, string> {
  if (!isAllowedOrigin(origin, env)) return { Vary: "Origin" };
  return {
    "Access-Control-Allow-Origin": origin!,
    "Access-Control-Allow-Methods": "GET, POST, DELETE, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type, Authorization",
    "Access-Control-Expose-Headers": "X-Ask-Actions, X-Ask-Remaining",
    "Access-Control-Max-Age": "86400",
    Vary: "Origin",
  };
}

function json(data: unknown, status = 200, headers: Record<string, string> = {}): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8", ...headers },
  });
}

function clientIp(request: Request): string {
  return request.headers.get("CF-Connecting-IP") ?? "";
}

/** UTC date as YYYY-MM-DD, offset by whole days. */
export function dayKey(offsetDays: number, now = Date.now()): string {
  return new Date(now + offsetDays * DAY_MS).toISOString().slice(0, 10);
}

async function sha256(text: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(text));
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, "0")).join("");
}
