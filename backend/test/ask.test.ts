import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { createScheduledController } from "cloudflare:test";
import { env } from "cloudflare:workers";
import worker, { dayKey } from "../src/index";
import { ASK_LIMITS, buildKnowledge, loadKnowledge, resetKnowledge, retrieve, sseToText, type KnowledgeFile } from "../src/ask";
// Fixtures: the site's real knowledge files, shaped like the knowledge.json its build publishes.
import profile from "../../site/knowledge/profile.md";
import r8 from "../../site/src/jsMain/resources/markdown/blog/R8Story.md";
import ota from "../../site/src/jsMain/resources/markdown/blog/OtaUpdatesReactNative.md";

const ORIGIN = "https://antimkhel.github.io";
const FILE: KnowledgeFile = {
  profile,
  posts: [
    { slug: "r8-story", markdown: r8 },
    { slug: "ota-updates-react-native", markdown: ota },
  ],
};
const KB = buildKnowledge(FILE);

/** Serves knowledge.json like the site does; counts how often it was fetched. */
function siteServes(file: KnowledgeFile | null) {
  return vi.spyOn(globalThis, "fetch").mockImplementation(async (input) => {
    expect(String(input instanceof Request ? input.url : input)).toBe("https://site.test/knowledge.json");
    return file ? Response.json(file) : new Response("down", { status: 502 });
  });
}

beforeEach(() => {
  resetKnowledge();
  siteServes(FILE);
});
afterEach(() => vi.restoreAllMocks());

/** A Workers AI stand-in that streams [parts] as server-sent events and records what it was asked. */
function fakeAi(parts: string[] = ["Ritwik ", "leads ", "Android."], shape: "classic" | "openai" = "classic") {
  const calls: Array<{ model: string; inputs: any }> = [];
  const AI = {
    run: vi.fn(async (model: string, inputs: any) => {
      calls.push({ model, inputs });
      const enc = new TextEncoder();
      return new ReadableStream({
        start(c) {
          for (const p of parts) {
            const payload = shape === "classic" ? { response: p } : { choices: [{ delta: { content: p } }] };
            c.enqueue(enc.encode(`data: ${JSON.stringify(payload)}\n\n`));
          }
          c.enqueue(enc.encode("data: [DONE]\n\n"));
          c.close();
        },
      });
    }),
  };
  return { AI, calls };
}

function askReq(body: unknown, opts: { origin?: string | null; ip?: string } = {}) {
  const headers = new Headers({ "Content-Type": "application/json", "CF-Connecting-IP": opts.ip ?? "203.0.113.9", "User-Agent": "vitest" });
  if (opts.origin !== null) headers.set("Origin", opts.origin ?? ORIGIN);
  return new Request("https://api.test/ask", { method: "POST", headers, body: JSON.stringify(body) });
}

const ask = (body: unknown, ai = fakeAi().AI, opts: { origin?: string | null; ip?: string } = {}) =>
  worker.fetch(askReq(body, opts), { ...env, AI: ai as unknown as Ai, ASK_MODEL: "@cf/test/model" });

async function pipe(chunks: string[]): Promise<string> {
  const enc = new TextEncoder();
  const src = new ReadableStream({
    start(c) {
      chunks.forEach((x) => c.enqueue(enc.encode(x)));
      c.close();
    },
  });
  return new Response(src.pipeThrough(sseToText())).text();
}

describe("knowledge", () => {
  it("builds chunks from the profile and every post it is given", () => {
    const { chunks } = KB;
    expect(chunks[0]).toMatchObject({ title: "Ritwik at a glance", core: true });
    for (const slug of ["r8-story", "ota-updates-react-native"]) {
      expect(chunks.some((c) => c.actions.includes(`post:${slug}`))).toBe(true);
    }
    expect(chunks.every((c) => !c.text.includes("```"))).toBe(true);
  });

  it("picks up a new post with no code changes", () => {
    const post = "---\ntitle: \"Shipping Wear OS tiles\"\n---\n\nIntro paragraph about tiles on watches.\n\n## Why tiles\n\nGlanceable Wear OS surfaces for the watch face.";
    const kb = buildKnowledge({ ...FILE, posts: [...FILE.posts, { slug: "wear-os-tiles", markdown: post }] });
    expect(retrieve(kb, "Has he worked on Wear OS tiles?").actions).toContain("post:wear-os-tiles");
  });

  it("caches the site's knowledge and keeps a stale copy if the site goes down", async () => {
    vi.restoreAllMocks();
    const fetchSpy = siteServes(FILE);
    const t0 = 1_000_000;
    await loadKnowledge("https://site.test/knowledge.json", t0);
    await loadKnowledge("https://site.test/knowledge.json", t0 + 1000);
    expect(fetchSpy).toHaveBeenCalledTimes(1);

    vi.restoreAllMocks();
    siteServes(null);
    const kb = await loadKnowledge("https://site.test/knowledge.json", t0 + ASK_LIMITS.knowledgeTtlMs + 1);
    expect(kb.chunks.length).toBe(KB.chunks.length);
  });

  it.each([
    ["Tell me about the R8 obfuscation work", "post:r8-story"],
    ["Has he led teams?", "app:work"],
    ["What awards has he won?", "app:awards"],
    ["How does his React Native OTA platform work?", "product:airsync"],
    ["Which skills does he have?", "app:skills"],
    ["What did he do at HSBC?", "app:work"],
    ["What is his salary expectation and notice period?", "app:contact"],
  ])("routes %j to %s", (q, action) => {
    expect(retrieve(KB, q).actions).toContain(action);
  });

  it("always includes the core facts and stays within budget", () => {
    const { context } = retrieve(KB, "compose design system tokens");
    expect(context[0].core).toBe(true);
    expect(context.slice(1).reduce((n, c) => n + c.text.length, 0)).toBeLessThanOrEqual(ASK_LIMITS.contextChars);
    expect(context.length).toBeLessThanOrEqual(5);
  });

  it("offers Contact for questions it has nothing on", () => {
    expect(retrieve(KB, "qwerty zxcvb").actions).toEqual(["app:contact"]);
  });
});

describe("streaming", () => {
  it("turns classic and OpenAI-style events into text", async () => {
    expect(await pipe(['data: {"response":"Hel"}\n\n', 'data: {"response":"lo"}\n\ndata: [DONE]\n\n'])).toBe("Hello");
    expect(await pipe(['data: {"choices":[{"delta":{"content":"Hi"}}]}\n\n', "data: [DONE]\n\n"])).toBe("Hi");
  });

  it("drops reasoning blocks, even when tags arrive split", async () => {
    const ev = (t: string) => `data: ${JSON.stringify({ response: t })}\n\n`;
    expect(await pipe([ev("<thi"), ev("nk>plan</th"), ev("ink>\n\nAnswer")])).toBe("Answer");
  });

  it("reports the final usage once the answer ends", async () => {
    const seen: unknown[] = [];
    const enc = new TextEncoder();
    const ev = (o: unknown) => enc.encode(`data: ${JSON.stringify(o)}\n\n`);
    const src = new ReadableStream({
      start(c) {
        c.enqueue(ev({ choices: [{ delta: { content: "Hi" } }], usage: { prompt_tokens: 0, completion_tokens: 1, neurons: 0.1 } }));
        c.enqueue(ev({ choices: [{ delta: {} }], usage: { prompt_tokens: 800, completion_tokens: 20, neurons: 5.3 } }));
        c.close();
      },
    });
    expect(await new Response(src.pipeThrough(sseToText((u) => seen.push(u)))).text()).toBe("Hi");
    expect(seen).toEqual([{ prompt_tokens: 800, completion_tokens: 20, neurons: 5.3 }]);
  });

  it("copes with events split across network chunks", async () => {
    expect(await pipe(['data: {"respo', 'nse":"ok"}\n', "\n"])).toBe("ok");
  });
});

describe("POST /ask", () => {
  it("streams an answer with buttons and the visitor's remaining questions", async () => {
    const { AI, calls } = fakeAi();
    const res = await ask({ question: "Tell me about the R8 work" }, AI);
    expect(res.status).toBe(200);
    expect(await res.text()).toBe("Ritwik leads Android.");
    expect(res.headers.get("X-Ask-Actions")).toContain("post:r8-story");
    expect(res.headers.get("X-Ask-Remaining")).toBe(String(ASK_LIMITS.perVisitorPerDay - 1));
    expect(res.headers.get("Access-Control-Expose-Headers")).toContain("X-Ask-Actions");
    expect(calls[0].model).toBe("@cf/test/model");
    const last = calls[0].inputs.messages.at(-1).content as string;
    expect(last).toContain("Visitor's question: Tell me about the R8 work");
    expect(last).toContain("Ritwik at a glance");
  });

  it("passes the last turns of the conversation, trimmed", async () => {
    const { AI, calls } = fakeAi();
    const history = [1, 2, 3].map((i) => ({ q: `question ${i}`, a: "x".repeat(2000) }));
    await (await ask({ question: "and at HSBC?", history }, AI)).text();
    const roles = calls[0].inputs.messages.map((m: any) => m.role);
    expect(roles).toEqual(["system", "user", "assistant", "user", "assistant", "user"]);
    expect(calls[0].inputs.messages[1].content).toBe("question 2");
    expect(calls[0].inputs.messages[2].content.length).toBe(ASK_LIMITS.historyAnswer);
  });

  it("only answers the site's own origin", async () => {
    expect((await ask({ question: "hi there" }, undefined, { origin: "https://evil.example" })).status).toBe(403);
    expect((await ask({ question: "hi there" }, undefined, { origin: null })).status).toBe(403);
  });

  it("validates the question", async () => {
    expect((await ask({ question: "" })).status).toBe(400);
    expect((await ask({ question: "x".repeat(ASK_LIMITS.question + 1) })).status).toBe(400);
  });

  it("limits questions per visitor per day", async () => {
    let last = 0;
    for (let i = 0; i <= ASK_LIMITS.perVisitorPerDay; i++) {
      const res = await ask({ question: "What does he do?" });
      await res.text();
      last = res.status;
    }
    expect(last).toBe(429);
    // someone else can still ask
    expect((await ask({ question: "What does he do?" }, undefined, { ip: "198.51.100.4" })).status).toBe(200);
  });

  it("stops at the site-wide daily cap", async () => {
    await env.DB.prepare("INSERT INTO counters (name, value) VALUES (?, ?)").bind(`ask:${dayKey(0)}`, ASK_LIMITS.perDay).run();
    const res = await ask({ question: "What does he do?" });
    expect(res.status).toBe(503);
  });

  it("explains when the site's knowledge can't be reached", async () => {
    vi.restoreAllMocks();
    siteServes(null);
    const res = await ask({ question: "What does he do?" });
    expect(res.status).toBe(503);
  });

  it("explains when Workers AI is unavailable", async () => {
    const AI = { run: vi.fn(async () => { throw new Error("daily free allocation exceeded"); }) };
    const res = await ask({ question: "What does he do?" }, AI as never);
    expect(res.status).toBe(503);
    expect(((await res.json()) as { error: string }).error).toMatch(/resting/);
  });

  it("keeps only today's and yesterday's usage rows", async () => {
    await env.DB.batch([
      env.DB.prepare("INSERT INTO usage (day, kind, hash, count) VALUES (?, 'ask', 'a', 1), (?, 'ask', 'b', 1)").bind(dayKey(-3), dayKey(0)),
      env.DB.prepare("INSERT INTO counters (name, value) VALUES (?, 5), (?, 5)").bind(`ask:${dayKey(-3)}`, `ask:${dayKey(0)}`),
    ]);
    await worker.scheduled(createScheduledController(), env);
    const days = await env.DB.prepare("SELECT day FROM usage").all<{ day: string }>();
    expect(days.results.map((r) => r.day)).toEqual([dayKey(0)]);
    const names = await env.DB.prepare("SELECT name FROM counters WHERE name LIKE 'ask:%'").all<{ name: string }>();
    expect(names.results.map((r) => r.name)).toEqual([`ask:${dayKey(0)}`]);
  });
});
