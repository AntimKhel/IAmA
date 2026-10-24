import { afterEach, describe, expect, it, vi } from "vitest";
import { createScheduledController } from "cloudflare:test";
import { env } from "cloudflare:workers";
import worker, { cleanMessage, dayKey } from "../src/index";

const ORIGIN = "https://antimkhel.github.io";
const ADMIN = { Authorization: "Bearer test-admin-token-0123456789" };

function call(path: string, init: RequestInit & { ip?: string; ua?: string } = {}) {
  const headers = new Headers(init.headers);
  headers.set("CF-Connecting-IP", init.ip ?? "203.0.113.7");
  headers.set("User-Agent", init.ua ?? "vitest-browser");
  if (!headers.has("Origin")) headers.set("Origin", ORIGIN);
  return worker.fetch(new Request(`https://api.test${path}`, { ...init, headers }), env);
}

const postNote = (body: unknown, ip?: string) =>
  call("/notes", { method: "POST", body: JSON.stringify(body), headers: { "Content-Type": "application/json" }, ip });

function turnstile(success: boolean) {
  return vi.spyOn(globalThis, "fetch").mockImplementation(async () => Response.json({ success }));
}

afterEach(() => vi.restoreAllMocks());

describe("visitor counter", () => {
  it("counts each visitor once per day", async () => {
    const first = await (await call("/visit", { method: "POST" })).json<{ visitors: number; counted: boolean }>();
    const again = await (await call("/visit", { method: "POST" })).json<{ visitors: number; counted: boolean }>();
    expect(first).toEqual({ visitors: 1, counted: true });
    expect(again).toEqual({ visitors: 1, counted: false });

    const other = await (await call("/visit", { method: "POST", ip: "198.51.100.2" })).json<{ visitors: number }>();
    expect(other.visitors).toBe(2);
    expect(await (await call("/stats")).json()).toEqual({ visitors: 2 });
  });

  it("never stores raw IPs", async () => {
    await call("/visit", { method: "POST", ip: "192.0.2.55" });
    const row = await env.DB.prepare("SELECT hash FROM visits").first<{ hash: string }>();
    expect(row?.hash).toMatch(/^[0-9a-f]{64}$/);
    expect(row?.hash).not.toContain("192.0.2.55");
  });

  it("cron removes visitor hashes older than yesterday", async () => {
    await env.DB.prepare("INSERT INTO visits (day, hash) VALUES (?, 'old'), (?, 'today')").bind(dayKey(-3), dayKey(0)).run();
    await worker.scheduled(createScheduledController(), env);
    const { results } = await env.DB.prepare("SELECT hash FROM visits").all<{ hash: string }>();
    expect(results.map((r) => r.hash)).toEqual(["today"]);
  });
});

describe("guestbook", () => {
  it("keeps new notes hidden until approved", async () => {
    turnstile(true);
    const res = await postNote({ name: "Asha", message: "Loved the phone hero!", turnstileToken: "ok" });
    expect(res.status).toBe(202);
    expect((await (await call("/notes")).json<{ notes: unknown[] }>()).notes).toHaveLength(0);

    const pending = await (await call("/admin/notes?status=pending", { headers: ADMIN })).json<{ notes: { id: number }[] }>();
    expect(pending.notes).toHaveLength(1);
    expect((await call(`/admin/notes/${pending.notes[0].id}/approve`, { method: "POST", headers: ADMIN })).status).toBe(200);

    const pub = await (await call("/notes")).json<{ notes: { name: string; message: string }[]; hasMore: boolean }>();
    expect(pub.notes).toEqual([expect.objectContaining({ name: "Asha", message: "Loved the phone hero!" })]);
    expect(pub.hasMore).toBe(false);
  });

  it("deletes notes", async () => {
    turnstile(true);
    await postNote({ name: "Spam", message: "buy now", turnstileToken: "ok" });
    const { notes } = await (await call("/admin/notes", { headers: ADMIN })).json<{ notes: { id: number }[] }>();
    expect((await call(`/admin/notes/${notes[0].id}`, { method: "DELETE", headers: ADMIN })).status).toBe(200);
    expect((await call(`/admin/notes/${notes[0].id}`, { method: "DELETE", headers: ADMIN })).status).toBe(404);
  });

  it.each([
    [{ name: "", message: "hi" }, "name"],
    [{ name: "x".repeat(41), message: "hi" }, "name"],
    [{ name: "Ok", message: "" }, "characters"],
    [{ name: "Ok", message: "y".repeat(281) }, "characters"],
    [{ name: "Ok", message: "https://a.example and https://b.example" }, "link"],
  ])("rejects invalid input %#", async (body, hint) => {
    const fetchSpy = turnstile(true);
    const res = await postNote({ ...body, turnstileToken: "ok" });
    expect(res.status).toBe(400);
    expect((await res.json<{ error: string }>()).error.toLowerCase()).toContain(hint);
    expect(fetchSpy).not.toHaveBeenCalled(); // validation happens before Turnstile
  });

  it("requires a valid Turnstile token", async () => {
    const fetchSpy = turnstile(false);
    expect((await postNote({ name: "Bot", message: "hello", turnstileToken: "bad" })).status).toBe(403);
    expect((await postNote({ name: "Bot", message: "hello" })).status).toBe(403);
    expect(fetchSpy).toHaveBeenCalledTimes(1); // a missing token never reaches siteverify
  });

  it("rate-limits to 3 notes per visitor per day", async () => {
    turnstile(true);
    for (let i = 0; i < 3; i++) expect((await postNote({ name: "Fan", message: `note ${i}`, turnstileToken: "ok" })).status).toBe(202);
    expect((await postNote({ name: "Fan", message: "one more", turnstileToken: "ok" })).status).toBe(429);
    expect((await postNote({ name: "Other", message: "hi", turnstileToken: "ok" }, "198.51.100.9")).status).toBe(202);
  });

  it("cleans messages but keeps line breaks", () => {
    expect(cleanMessage("  Hi\u0000 there \r\n\r\n\r\n\tbye  ")).toBe("Hi there\n\nbye");
  });
});

describe("admin + CORS", () => {
  it("rejects missing or wrong admin tokens", async () => {
    expect((await call("/admin/notes")).status).toBe(401);
    expect((await call("/admin/notes", { headers: { Authorization: "Bearer nope" } })).status).toBe(401);
    expect((await call("/admin/notes", { headers: ADMIN })).status).toBe(200);
  });

  it("only allows the portfolio origins", async () => {
    const ok = await call("/stats");
    expect(ok.headers.get("Access-Control-Allow-Origin")).toBe(ORIGIN);
    const bad = await call("/stats", { headers: { Origin: "https://evil.example" } });
    expect(bad.headers.get("Access-Control-Allow-Origin")).toBeNull();
    const pre = await call("/notes", { method: "OPTIONS" });
    expect(pre.status).toBe(204);
    expect(pre.headers.get("Access-Control-Allow-Methods")).toContain("POST");
  });
});
