import type { Env } from "./env";

export const DAY_MS = 24 * 60 * 60 * 1000;

export function json(data: unknown, status = 200, headers: Record<string, string> = {}): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8", ...headers },
  });
}

/** Single line: no control characters, collapsed whitespace. */
export function cleanLine(value: unknown): string {
  if (typeof value !== "string") return "";
  return value.replace(/[\p{Cc}\p{Cf}]/gu, " ").replace(/\s+/g, " ").trim();
}

/** Multi-line text: keeps line breaks (at most one blank line in a row), strips other control characters. */
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

export function clientIp(request: Request): string {
  return request.headers.get("CF-Connecting-IP") ?? "";
}

/** UTC date as YYYY-MM-DD, offset by whole days. */
export function dayKey(offsetDays: number, now = Date.now()): string {
  return new Date(now + offsetDays * DAY_MS).toISOString().slice(0, 10);
}

export async function sha256(text: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(text));
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

/** Anonymous visitor id for today: salted hash of IP + user agent + date, never stored past tomorrow. */
export function visitorHash(request: Request, env: Env, day = dayKey(0)): Promise<string> {
  return sha256(`${clientIp(request)}|${request.headers.get("User-Agent") ?? ""}|${day}|${env.HASH_SALT}`);
}

export function isAllowedOrigin(origin: string | null, env: Env): boolean {
  const allowed = (env.ALLOWED_ORIGINS ?? "").split(",").map((s) => s.trim()).filter(Boolean);
  return !!origin && allowed.includes(origin);
}

/** Reads a small JSON body; null if it's too big or not JSON. */
export async function readJson<T>(request: Request, maxBytes: number): Promise<T | null> {
  const raw = await request.text();
  if (raw.length > maxBytes) return null;
  try {
    return JSON.parse(raw) as T;
  } catch {
    return null;
  }
}

export async function bump(env: Env, counter: string): Promise<void> {
  await env.DB.prepare("INSERT INTO counters (name, value) VALUES (?, 1) ON CONFLICT (name) DO UPDATE SET value = value + 1")
    .bind(counter)
    .run();
}
