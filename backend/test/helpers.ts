import { vi } from "vitest";
import { env } from "cloudflare:workers";
import worker from "../src/index";
import type { KnowledgeFile } from "../src/ask";

export const ORIGIN = "https://antimkhel.github.io";

export interface CallOptions {
  method?: string;
  body?: BodyInit | null;
  headers?: Record<string, string>;
  ip?: string;
  ua?: string;
  origin?: string | null;
  country?: string;
  ai?: unknown;
}

/** Calls the Worker directly, as a visitor from [ip] on the site's origin (overridable). */
export function call(path: string, o: CallOptions = {}) {
  const headers = new Headers(o.headers);
  headers.set("CF-Connecting-IP", o.ip ?? "203.0.113.7");
  headers.set("User-Agent", o.ua ?? "vitest");
  if (o.origin !== null) headers.set("Origin", o.origin ?? ORIGIN);
  if (o.country) headers.set("CF-IPCountry", o.country);
  const request = new Request(`https://api.test${path}`, { method: o.method ?? "GET", headers, body: o.body ?? null });
  const testEnv = o.ai ? { ...env, AI: o.ai as Ai } : env;
  return worker.fetch(request, testEnv);
}

export const postJson = (path: string, body: unknown, o: CallOptions = {}) =>
  call(path, { ...o, method: "POST", body: JSON.stringify(body), headers: { "Content-Type": "application/json", ...o.headers } });

/** A small knowledge.json, shaped like the one the site build publishes. */
export const MINI_KNOWLEDGE: KnowledgeFile = {
  profile: [
    "## Ritwik at a glance",
    "@actions: app:about, resume",
    "Ritwik Raj Srivastava is a Tech Lead for Android & AI in Mumbai with 7+ years of experience.",
    "",
    "## Skills",
    "@actions: app:skills",
    "- Android: Kotlin, Jetpack Compose, MVVM.",
    "- Cross-platform: React Native, OTA delivery.",
    "",
    "## Experience: Reliance Jio, Tech Lead (Oct 2021 to present)",
    "@actions: app:work, product:myjio",
    "Led cross-functional Android development for MyJio. Architected MyJio Agentic, an AI assistant.",
    "",
    "## Product: JioAirSync (React Native OTA updates)",
    "@actions: product:airsync, post:ota-updates-react-native",
    "JioAirSync is a CodePush-style over-the-air update platform for React Native that Ritwik created.",
  ].join("\n"),
  posts: [
    {
      slug: "r8-story",
      markdown: '---\ntitle: "R8 story"\n---\n\nHow obfuscation went from 24% to 82% by untangling keep rules in dependencies.\n',
    },
  ],
};

/** Serves [file] as the site's knowledge.json (and anything else via [other]). */
export function siteServes(file: KnowledgeFile | null, other?: (url: string) => Response | undefined) {
  return vi.spyOn(globalThis, "fetch").mockImplementation(async (input) => {
    const url = String(input instanceof Request ? input.url : input);
    const extra = other?.(url);
    if (extra) return extra;
    if (url.endsWith("/knowledge.json")) return file ? Response.json(file) : new Response("down", { status: 502 });
    throw new Error(`unexpected fetch ${url}`);
  });
}
