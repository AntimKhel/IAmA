# iama-api

Cloudflare Worker + D1 behind the portfolio's **visitor counter**, **guestbook** and **Ask Ritwik** (AI answers on Workers AI). The site itself stays a
static Kobweb export on GitHub Pages and calls this API (CORS-limited to the portfolio's origins).

| Endpoint | Purpose |
|---|---|
| `POST /visit`, `GET /stats` | Unique visitors: a salted daily hash, so each visitor counts once a day; no cookies, no raw IPs |
| `GET /notes`, `POST /notes` | Approved notes; new notes need a Cloudflare Turnstile token and land as *pending* |
| `/admin/notes…` | Approve / delete (Bearer `ADMIN_TOKEN`), used by the site's `/admin` page |
| `POST /ask` | Ask Ritwik: `{ question, history }` → answer streamed as text; suggested buttons in `X-Ask-Actions` |

## Ask Ritwik
- **What it knows** is whatever the site publishes at `/knowledge.json`: `site/knowledge/profile.md` plus every
  blog post, bundled by the site build (`askKnowledge` task). New posts need no change or redeploy here; the
  Worker refetches the file every few minutes.
- **Model**: `ASK_MODEL` in `wrangler.toml` (GLM-4.7-Flash, thinking off). Retrieval is BM25 over small chunks, so
  each answer costs about 5–11 neurons.
- **Cost**: the Workers Free plan includes 10,000 neurons a day; past that, requests error instead of billing.
  Limits: 20 questions per visitor a day, 800 site-wide (`ASK_LIMITS` in `src/ask.ts`).
- **Watch it**: `npx wrangler tail` shows one `{"ask":"usage", …, "neurons": …}` line per answer.

## First-time setup
```bash
./setup.sh   # guided: Cloudflare login, D1, schema, deploy, Turnstile, secrets, smoke test
```

## Day to day
```bash
npm run dev        # local API on :8787: applies the local schema, then starts (uses .dev.vars; copy .dev.vars.example)
                   # Ask uses real Workers AI even locally, and reads knowledge.json from the Kobweb dev server
npm run db:local   # apply migrations to the local D1
npm test           # Vitest inside the Workers runtime
npm run deploy     # publish changes to the Worker
```
Moderate notes at https://antimkhel.github.io/IAmA/admin (paste `ADMIN_TOKEN` once per browser).
