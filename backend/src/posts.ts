/**
 * Blog reactions: read counts and claps per post. A read counts once per visitor per day; claps are capped
 * per visitor per day. Only slugs the site actually publishes (from knowledge.json) are accepted.
 */
import { loadKnowledge } from "./ask";
import type { Env } from "./env";
import { dayKey, isAllowedOrigin, json, readJson, visitorHash } from "./util";

export const CLAPS_PER_DAY = 10;

interface Totals {
  reads: number;
  claps: number;
}

/** GET /posts: { posts: { slug: { reads, claps } } } */
export async function listPostStats(env: Env): Promise<Response> {
  const { results } = await env.DB.prepare("SELECT slug, reads, claps FROM post_stats").all<{ slug: string } & Totals>();
  const posts = Object.fromEntries(results.map((r) => [r.slug, { reads: r.reads, claps: r.claps }]));
  return json({ posts }, 200, { "Cache-Control": "public, max-age=60" });
}

/** POST /posts/:slug/read and POST /posts/:slug/clap { count } */
export async function reactToPost(request: Request, env: Env, slug: string, action: "read" | "clap"): Promise<Response> {
  if (!isAllowedOrigin(request.headers.get("Origin"), env)) return json({ error: "Forbidden" }, 403);
  if (!(await isPublished(env, slug))) return json({ error: "Not found" }, 404);

  const day = dayKey(0);
  const visitor = await visitorHash(request, env, day);
  const seen = await env.DB.prepare(
    "INSERT INTO post_seen (day, slug, hash) VALUES (?, ?, ?) ON CONFLICT (day, slug, hash) DO UPDATE SET day = day " +
      "RETURNING has_read, claps",
  )
    .bind(day, slug, visitor)
    .first<{ has_read: number; claps: number }>();
  let myClaps = seen?.claps ?? 0;

  if (action === "read" && !seen?.has_read) {
    await env.DB.batch([
      env.DB.prepare("UPDATE post_seen SET has_read = 1 WHERE day = ? AND slug = ? AND hash = ?").bind(day, slug, visitor),
      env.DB.prepare(
        "INSERT INTO post_stats (slug, reads) VALUES (?, 1) ON CONFLICT (slug) DO UPDATE SET reads = reads + 1",
      ).bind(slug),
    ]);
  }
  if (action === "clap") {
    const body = await readJson<{ count?: unknown }>(request, 256);
    const wanted = Math.floor(Number(body?.count ?? 1));
    const add = Math.max(0, Math.min(Number.isFinite(wanted) ? wanted : 1, CLAPS_PER_DAY - myClaps, CLAPS_PER_DAY));
    if (add > 0) {
      await env.DB.batch([
        env.DB.prepare("UPDATE post_seen SET claps = claps + ? WHERE day = ? AND slug = ? AND hash = ?").bind(add, day, slug, visitor),
        env.DB.prepare(
          "INSERT INTO post_stats (slug, claps) VALUES (?, ?) ON CONFLICT (slug) DO UPDATE SET claps = claps + excluded.claps",
        ).bind(slug, add),
      ]);
      myClaps += add;
    }
  }

  const totals = await env.DB.prepare("SELECT reads, claps FROM post_stats WHERE slug = ?").bind(slug).first<Totals>();
  return json(
    { reads: totals?.reads ?? 0, claps: totals?.claps ?? 0, myClaps, maxClaps: CLAPS_PER_DAY },
    200,
    { "Cache-Control": "no-store" },
  );
}

async function isPublished(env: Env, slug: string): Promise<boolean> {
  if (!/^[a-z0-9-]{1,80}$/.test(slug)) return false;
  try {
    return (await loadKnowledge(env.KNOWLEDGE_URL)).slugs.has(slug);
  } catch {
    return false;
  }
}

export async function prunePostSeen(env: Env): Promise<void> {
  await env.DB.prepare("DELETE FROM post_seen WHERE day < ?").bind(dayKey(-1)).run();
}
