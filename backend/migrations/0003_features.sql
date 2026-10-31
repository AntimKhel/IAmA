-- One quota table for every rate-limited feature (Ask, Fit check, voice), per anonymous daily visitor hash.
-- Rows older than yesterday are deleted by the daily cron. Replaces ask_usage.
CREATE TABLE IF NOT EXISTS usage (
  day   TEXT NOT NULL,
  kind  TEXT NOT NULL,
  hash  TEXT NOT NULL,
  count INTEGER NOT NULL DEFAULT 0,
  PRIMARY KEY (day, kind, hash)
);
DROP TABLE IF EXISTS ask_usage;

-- Visitor map: unique visitors per country (the two-letter code Cloudflare reports, nothing else).
CREATE TABLE IF NOT EXISTS countries (
  code     TEXT PRIMARY KEY,
  visitors INTEGER NOT NULL DEFAULT 0
);

-- The latest arrivals, for the map's pulses: country and time only. The cron keeps the newest 200.
CREATE TABLE IF NOT EXISTS arrivals (
  id      INTEGER PRIMARY KEY AUTOINCREMENT,
  country TEXT NOT NULL,
  at      INTEGER NOT NULL
);

-- Blog reactions: totals per post...
CREATE TABLE IF NOT EXISTS post_stats (
  slug  TEXT PRIMARY KEY,
  reads INTEGER NOT NULL DEFAULT 0,
  claps INTEGER NOT NULL DEFAULT 0
);

-- ...and, per anonymous daily visitor hash, whether today's read was counted and how many claps were given.
CREATE TABLE IF NOT EXISTS post_seen (
  day      TEXT NOT NULL,
  slug     TEXT NOT NULL,
  hash     TEXT NOT NULL,
  has_read INTEGER NOT NULL DEFAULT 0,
  claps    INTEGER NOT NULL DEFAULT 0,
  PRIMARY KEY (day, slug, hash)
);

-- All-time totals shown in the Stats app.
INSERT OR IGNORE INTO counters (name, value) VALUES ('ask_total', 0), ('fit_total', 0);
