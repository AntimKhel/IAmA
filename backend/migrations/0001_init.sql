-- Running totals (only `unique_visitors` for now).
CREATE TABLE IF NOT EXISTS counters (
  name  TEXT PRIMARY KEY,
  value INTEGER NOT NULL
);
INSERT OR IGNORE INTO counters (name, value) VALUES ('unique_visitors', 0);

-- One row per anonymous visitor per day; used only to count each visitor once a day.
-- `hash` = SHA-256(ip + user-agent + day + secret salt). Rows older than a day are deleted by the cron.
CREATE TABLE IF NOT EXISTS visits (
  day  TEXT NOT NULL,
  hash TEXT NOT NULL,
  PRIMARY KEY (day, hash)
);

-- Guestbook. Notes are public only once approved.
CREATE TABLE IF NOT EXISTS notes (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  name       TEXT    NOT NULL,
  message    TEXT    NOT NULL,
  created_at INTEGER NOT NULL,
  status     TEXT    NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'approved')),
  ip_hash    TEXT    NOT NULL
);
CREATE INDEX IF NOT EXISTS notes_status_created ON notes (status, created_at);
CREATE INDEX IF NOT EXISTS notes_iphash_created ON notes (ip_hash, created_at);
