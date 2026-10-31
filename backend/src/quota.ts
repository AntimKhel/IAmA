import type { Env } from "./env";
import { dayKey } from "./util";

export interface QuotaRule {
  perVisitorPerDay: number;
  perDay: number;
}

export type QuotaResult = { ok: true; remaining: number } | { ok: false; reason: "visitor" | "site" };

/**
 * Takes one unit of a daily quota for [kind] (e.g. "ask", "fit", "voice"): per anonymous visitor and for the
 * whole site. The site-wide caps keep Workers AI inside the free daily allocation.
 */
export async function takeQuota(env: Env, kind: string, visitor: string, rule: QuotaRule): Promise<QuotaResult> {
  const day = dayKey(0);
  const mine = await env.DB.prepare(
    "INSERT INTO usage (day, kind, hash, count) VALUES (?, ?, ?, 1) " +
      "ON CONFLICT (day, kind, hash) DO UPDATE SET count = count + 1 RETURNING count",
  )
    .bind(day, kind, visitor)
    .first<{ count: number }>();
  const used = mine?.count ?? 1;
  if (used > rule.perVisitorPerDay) return { ok: false, reason: "visitor" };

  const total = await env.DB.prepare(
    "INSERT INTO counters (name, value) VALUES (?, 1) ON CONFLICT (name) DO UPDATE SET value = value + 1 RETURNING value",
  )
    .bind(`${kind}:${day}`)
    .first<{ value: number }>();
  if ((total?.value ?? 1) > rule.perDay) return { ok: false, reason: "site" };
  return { ok: true, remaining: Math.max(0, rule.perVisitorPerDay - used) };
}

/** Daily cleanup: usage rows and the per-day site counters ("ask:2026-10-31") older than yesterday. */
export async function pruneQuotas(env: Env): Promise<void> {
  const yesterday = dayKey(-1);
  await env.DB.batch([
    env.DB.prepare("DELETE FROM usage WHERE day < ?").bind(yesterday),
    env.DB.prepare("DELETE FROM counters WHERE name LIKE '%:____-__-__' AND substr(name, -10) < ?").bind(yesterday),
  ]);
}
