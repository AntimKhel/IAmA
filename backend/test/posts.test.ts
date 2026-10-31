import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { createScheduledController } from "cloudflare:test";
import { env } from "cloudflare:workers";
import worker, { dayKey } from "../src/index";
import { resetKnowledge } from "../src/ask";
import { call, MINI_KNOWLEDGE, postJson, siteServes } from "./helpers";

type Reaction = { reads: number; claps: number; myClaps: number; maxClaps: number };

beforeEach(() => {
  resetKnowledge();
  siteServes(MINI_KNOWLEDGE);
});
afterEach(() => vi.restoreAllMocks());

describe("blog reactions", () => {
  it("counts a read once per visitor per day", async () => {
    await call("/posts/r8-story/read", { method: "POST" });
    await call("/posts/r8-story/read", { method: "POST" });
    const other = await (await call("/posts/r8-story/read", { method: "POST", ip: "198.51.100.9" })).json<Reaction>();
    expect(other).toMatchObject({ reads: 2, claps: 0, myClaps: 0, maxClaps: 10 });
  });

  it("caps claps per visitor per day", async () => {
    const first = await (await postJson("/posts/r8-story/clap", { count: 4 })).json<Reaction>();
    expect(first).toMatchObject({ claps: 4, myClaps: 4 });
    const second = await (await postJson("/posts/r8-story/clap", { count: 8 })).json<Reaction>();
    expect(second).toMatchObject({ claps: 10, myClaps: 10 });
    const third = await (await postJson("/posts/r8-story/clap", { count: 3 })).json<Reaction>();
    expect(third).toMatchObject({ claps: 10, myClaps: 10 });
    const someoneElse = await (await postJson("/posts/r8-story/clap", { count: 99 }, { ip: "198.51.100.4" })).json<Reaction>();
    expect(someoneElse).toMatchObject({ claps: 20, myClaps: 10 });
  });

  it("lists every post's totals", async () => {
    await call("/posts/r8-story/read", { method: "POST" });
    await postJson("/posts/r8-story/clap", { count: 2 });
    expect(await (await call("/posts")).json()).toEqual({ posts: { "r8-story": { reads: 1, claps: 2 } } });
  });

  it("only accepts posts the site publishes, from the site", async () => {
    expect((await call("/posts/not-a-post/read", { method: "POST" })).status).toBe(404);
    expect((await call("/posts/r8-story/read", { method: "POST", origin: "https://evil.example" })).status).toBe(403);
    expect((await call("/posts/R8_STORY/read", { method: "POST" })).status).toBe(404);
  });

  it("forgets who reacted after a day", async () => {
    await env.DB.prepare("INSERT INTO post_seen (day, slug, hash, has_read, claps) VALUES (?, 'r8-story', 'old', 1, 3), (?, 'r8-story', 'new', 1, 1)")
      .bind(dayKey(-3), dayKey(0))
      .run();
    await worker.scheduled(createScheduledController(), env);
    const rows = await env.DB.prepare("SELECT hash FROM post_seen").all<{ hash: string }>();
    expect(rows.results.map((r) => r.hash)).toEqual(["new"]);
  });
});
