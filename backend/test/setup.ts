import { beforeEach } from "vitest";
import { applyD1Migrations } from "cloudflare:test";
import { env } from "cloudflare:workers";

await applyD1Migrations(env.DB, env.TEST_MIGRATIONS);

// D1 storage persists across tests in a file, so start every test from an empty database.
beforeEach(async () => {
  await env.DB.batch([
    env.DB.prepare("DELETE FROM notes"),
    env.DB.prepare("DELETE FROM visits"),
    env.DB.prepare("DELETE FROM usage"),
    env.DB.prepare("DELETE FROM counters WHERE name LIKE '%:____-__-__'"),
    env.DB.prepare("DELETE FROM countries"),
    env.DB.prepare("DELETE FROM arrivals"),
    env.DB.prepare("DELETE FROM post_stats"),
    env.DB.prepare("DELETE FROM post_seen"),
    env.DB.prepare("UPDATE counters SET value = 0"),
  ]);
});
