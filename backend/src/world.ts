/**
 * Visitor map: which countries visitors come from. Only Cloudflare's two-letter country code is kept (no IPs,
 * no cities), counted once per visitor per day alongside the visitor counter.
 */
import type { Env } from "./env";
import { json } from "./util";

/** First day countries were counted (the map says "since …"). */
export const WORLD_SINCE = "2026-10-31";

const ARRIVALS_KEPT = 200;

/** ISO 3166-1 alpha-2 from the request, or null for unknown / Tor ("XX", "T1"). */
export function countryOf(request: Request): string | null {
  const cf = (request as unknown as { cf?: { country?: unknown } }).cf;
  const raw = (typeof cf?.country === "string" ? cf.country : request.headers.get("CF-IPCountry")) ?? "";
  const code = raw.trim().toUpperCase();
  if (!/^[A-Z]{2}$/.test(code) || code === "XX" || code === "T1") return null;
  return code;
}

/** Called for a visitor's first visit of the day. */
export async function recordArrival(env: Env, request: Request, now = Date.now()): Promise<void> {
  const country = countryOf(request);
  if (!country) return;
  await env.DB.batch([
    env.DB.prepare(
      "INSERT INTO countries (code, visitors) VALUES (?, 1) ON CONFLICT (code) DO UPDATE SET visitors = visitors + 1",
    ).bind(country),
    env.DB.prepare("INSERT INTO arrivals (country, at) VALUES (?, ?)").bind(country, now),
  ]);
}

/** GET /stats/world: totals, visitors per country, the latest arrivals, and the caller's own country. */
export async function worldStats(request: Request, env: Env): Promise<Response> {
  const [counters, countries, recent, notes] = await env.DB.batch([
    env.DB.prepare("SELECT name, value FROM counters WHERE name IN ('unique_visitors', 'ask_total', 'fit_total')"),
    env.DB.prepare("SELECT code, visitors FROM countries ORDER BY visitors DESC, code LIMIT 250"),
    env.DB.prepare("SELECT country, at FROM arrivals ORDER BY id DESC LIMIT 12"),
    env.DB.prepare("SELECT COUNT(*) AS n FROM notes WHERE status = 'approved'"),
  ]);
  const totals = Object.fromEntries((counters.results as Array<{ name: string; value: number }>).map((r) => [r.name, r.value]));
  return json(
    {
      since: WORLD_SINCE,
      you: countryOf(request),
      visitors: totals.unique_visitors ?? 0,
      askAnswered: totals.ask_total ?? 0,
      fitChecks: totals.fit_total ?? 0,
      notes: (notes.results[0] as { n: number } | undefined)?.n ?? 0,
      countries: countries.results as Array<{ code: string; visitors: number }>,
      recent: (recent.results as Array<{ country: string; at: number }>).map((r) => ({ code: r.country, at: r.at })),
    },
    200,
    { "Cache-Control": "private, max-age=60" }, // includes the caller's country
  );
}

export async function pruneArrivals(env: Env): Promise<void> {
  await env.DB.prepare(
    "DELETE FROM arrivals WHERE id <= (SELECT id FROM arrivals ORDER BY id DESC LIMIT 1 OFFSET ?)",
  )
    .bind(ARRIVALS_KEPT)
    .run();
}
