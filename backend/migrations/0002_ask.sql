-- "Ask Ritwik": questions per anonymous daily visitor hash (rows older than yesterday are deleted by the cron).
-- The site-wide daily total lives in counters as 'ask:YYYY-MM-DD'.
CREATE TABLE IF NOT EXISTS ask_usage (
  day   TEXT NOT NULL,
  hash  TEXT NOT NULL,
  count INTEGER NOT NULL DEFAULT 0,
  PRIMARY KEY (day, hash)
);
