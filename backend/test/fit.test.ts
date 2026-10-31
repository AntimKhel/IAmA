import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { env } from "cloudflare:workers";
import { dayKey } from "../src/index";
import { buildKnowledge, resetKnowledge, retrieveForJob } from "../src/ask";
import { extractJson, FIT_LIMITS, normalizeFit } from "../src/fit";
import { MINI_KNOWLEDGE, postJson, siteServes } from "./helpers";

const JD = `Senior Android Engineer
- 5+ years building Android apps in Kotlin and Jetpack Compose
- Experience with React Native and over-the-air updates
- Kubernetes and Go for backend services
- Lead a team of engineers`;

/** Workers AI stand-in returning [content] in the OpenAI-style shape. */
function fakeAi(content: string | (() => never)) {
  const calls: Array<{ model: string; inputs: any }> = [];
  return {
    calls,
    AI: {
      run: vi.fn(async (model: string, inputs: any) => {
        calls.push({ model, inputs });
        if (typeof content === "function") content();
        return { choices: [{ message: { content } }], usage: { prompt_tokens: 2000, completion_tokens: 300, neurons: 22 } };
      }),
    },
  };
}

const GOOD = JSON.stringify({
  role: "Senior Android Engineer",
  summary: "Strong Android and React Native background; no backend Go or Kubernetes on the site. Roughly 75% fit.",
  requirements: [
    { requirement: "Kotlin and Jetpack Compose", status: "match", evidence: "Skills list Kotlin and Jetpack Compose.", sources: [2] },
    { requirement: "React Native OTA updates", status: "Match", evidence: "Ritwik created JioAirSync.", sources: [4, 99] },
    { requirement: "Kubernetes and Go", status: "missing", evidence: "Nothing here", sources: [1] },
    { requirement: "Lead a team", status: "partial", evidence: "Led cross-functional Android development.", sources: [] },
  ],
});

beforeEach(() => {
  resetKnowledge();
  siteServes(MINI_KNOWLEDGE);
});
afterEach(() => vi.restoreAllMocks());

describe("fit check: parsing and grounding", () => {
  it("finds the JSON in a chatty reply", () => {
    expect(extractJson('Sure! ```json\n{"a": {"b": "}"}}\n``` hope that helps')).toEqual({ a: { b: "}" } });
    expect(extractJson("no json here")).toBeNull();
  });

  it("only keeps matches that point at the site, and turns sources into buttons", () => {
    const context = retrieveForJob(buildKnowledge(MINI_KNOWLEDGE), JD);
    const result = normalizeFit(JSON.parse(GOOD), context)!;
    const byReq = Object.fromEntries(result.requirements.map((r) => [r.requirement, r]));
    expect(byReq["Kotlin and Jetpack Compose"]).toMatchObject({ status: "match", actions: ["app:skills"] });
    expect(byReq["React Native OTA updates"]).toMatchObject({ status: "match", actions: ["product:airsync", "post:ota-updates-react-native"] });
    expect(byReq["Kubernetes and Go"]).toMatchObject({ status: "gap", evidence: "", actions: [] });
    // claimed partial with no source: not grounded, so it's a gap
    expect(byReq["Lead a team"]).toMatchObject({ status: "gap", evidence: "" });
    expect(result.counts).toEqual({ match: 2, partial: 0, gap: 2 });
    expect(result.summary).not.toMatch(/%/);
  });

  it("pulls in evidence for each part of a long description", () => {
    const titles = retrieveForJob(buildKnowledge(MINI_KNOWLEDGE), JD).map((c) => c.title);
    expect(titles[0]).toBe("Ritwik at a glance");
    expect(titles).toContain("Skills");
    expect(titles).toContain("Product: JioAirSync (React Native OTA updates)");
  });
});

describe("POST /fit", () => {
  it("returns requirements with evidence and buttons", async () => {
    const { AI, calls } = fakeAi(GOOD);
    const res = await postJson("/fit", { jd: JD }, { ai: AI });
    expect(res.status).toBe(200);
    const body = await res.json<any>();
    expect(body.role).toBe("Senior Android Engineer");
    expect(body.requirements).toHaveLength(4);
    expect(body.remaining).toBe(FIT_LIMITS.perVisitorPerDay - 1);
    const user = calls[0].inputs.messages[1].content as string;
    expect(user).toContain("<<<\nSenior Android Engineer");
    expect(user).toContain("[1] Ritwik at a glance");
    const total = await env.DB.prepare("SELECT value FROM counters WHERE name = 'fit_total'").first<{ value: number }>();
    expect(total?.value).toBe(1);
  });

  it("validates the description", async () => {
    expect((await postJson("/fit", { jd: "too short" })).status).toBe(400);
    expect((await postJson("/fit", { jd: "x".repeat(FIT_LIMITS.maxChars + 1) })).status).toBe(413);
    expect((await postJson("/fit", { jd: JD }, { origin: "https://evil.example" })).status).toBe(403);
  });

  it("allows three checks per visitor a day", async () => {
    const { AI } = fakeAi(GOOD);
    for (let i = 0; i < 3; i++) expect((await postJson("/fit", { jd: JD }, { ai: AI })).status).toBe(200);
    expect((await postJson("/fit", { jd: JD }, { ai: AI })).status).toBe(429);
  });

  it("stops at the site-wide cap", async () => {
    await env.DB.prepare("INSERT INTO counters (name, value) VALUES (?, ?)").bind(`fit:${dayKey(0)}`, FIT_LIMITS.perDay).run();
    expect((await postJson("/fit", { jd: JD }, { ai: fakeAi(GOOD).AI })).status).toBe(503);
  });

  it("explains when the model is unavailable or unreadable", async () => {
    const down = fakeAi(() => { throw new Error("daily free allocation exceeded"); });
    expect((await postJson("/fit", { jd: JD }, { ai: down.AI })).status).toBe(503);
    expect((await postJson("/fit", { jd: JD }, { ai: fakeAi("I think he's great!").AI, ip: "198.51.100.8" })).status).toBe(502);
  });
});
