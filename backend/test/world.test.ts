import { describe, expect, it } from "vitest";
import { createScheduledController } from "cloudflare:test";
import { env } from "cloudflare:workers";
import worker from "../src/index";
import { countryOf } from "../src/world";
import { call } from "./helpers";

type World = {
  since: string;
  visitors: number;
  askAnswered: number;
  fitChecks: number;
  notes: number;
  countries: Array<{ code: string; visitors: number }>;
  recent: Array<{ code: string; at: number }>;
};

describe("visitor map", () => {
  it("counts each visitor's country once a day, with nothing but the code", async () => {
    await call("/visit", { method: "POST", country: "IN", ip: "198.51.100.1" });
    await call("/visit", { method: "POST", country: "IN", ip: "198.51.100.1" }); // same visitor again
    await call("/visit", { method: "POST", country: "IN", ip: "198.51.100.2" });
    await call("/visit", { method: "POST", country: "DE", ip: "198.51.100.3" });

    const world = await (await call("/stats/world", { country: "DE" })).json<World & { you: string | null }>();
    expect(world.you).toBe("DE");
    expect(world.visitors).toBe(3);
    expect(world.countries).toEqual([
      { code: "IN", visitors: 2 },
      { code: "DE", visitors: 1 },
    ]);
    expect(world.recent.map((r) => r.code)).toEqual(["DE", "IN", "IN"]);
    const columns = await env.DB.prepare("SELECT * FROM arrivals LIMIT 1").first<Record<string, unknown>>();
    expect(Object.keys(columns ?? {}).sort()).toEqual(["at", "country", "id"]);
  });

  it("ignores unknown and Tor countries", () => {
    const req = (c?: string) => new Request("https://api.test/visit", { headers: c ? { "CF-IPCountry": c } : {} });
    expect(countryOf(req("us"))).toBe("US");
    expect(countryOf(req("XX"))).toBeNull();
    expect(countryOf(req("T1"))).toBeNull();
    expect(countryOf(req("USA"))).toBeNull();
    expect(countryOf(req())).toBeNull();
  });

  it("reports the site's totals", async () => {
    await env.DB.batch([
      env.DB.prepare("UPDATE counters SET value = 42 WHERE name = 'ask_total'"),
      env.DB.prepare("UPDATE counters SET value = 5 WHERE name = 'fit_total'"),
      env.DB.prepare("INSERT INTO notes (name, message, created_at, status, ip_hash) VALUES ('A', 'hi', 1, 'approved', 'h'), ('B', 'yo', 2, 'pending', 'h')"),
    ]);
    const world = await (await call("/stats/world")).json<World>();
    expect(world).toMatchObject({ askAnswered: 42, fitChecks: 5, notes: 1, since: "2026-10-31" });
  });

  it("keeps only the newest 200 arrivals", async () => {
    const stmt = env.DB.prepare("INSERT INTO arrivals (country, at) VALUES ('FR', ?)");
    await env.DB.batch(Array.from({ length: 205 }, (_, i) => stmt.bind(i)));
    await worker.scheduled(createScheduledController(), env);
    const left = await env.DB.prepare("SELECT COUNT(*) AS n, MIN(at) AS oldest FROM arrivals").first<{ n: number; oldest: number }>();
    expect(left).toEqual({ n: 200, oldest: 5 });
  });
});
