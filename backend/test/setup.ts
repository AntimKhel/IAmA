import { beforeEach } from "vitest";
import { applyD1Migrations } from "cloudflare:test";
import { env } from "cloudflare:workers";

await applyD1Migrations(env.DB, env.TEST_MIGRATIONS);

// D1 storage persists across tests in a file, so start every test from an empty database.
beforeEach(async () => {
  await env.DB.batch([
    env.DB.prepare("DELETE FROM notes"),
    env.DB.prepare("DELETE FROM visits"),
    env.DB.prepare("DELETE FROM ask_usage"),
    env.DB.prepare("DELETE FROM counters WHERE name LIKE 'ask:%'"),
    env.DB.prepare("UPDATE counters SET value = 0"),
  ]);
});
