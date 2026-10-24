# iama-api

Cloudflare Worker + D1 behind the portfolio's **visitor counter** and **guestbook**. The site itself stays a
static Kobweb export on GitHub Pages and calls this API (CORS-limited to the portfolio's origins).

| Endpoint | Purpose |
|---|---|
| `POST /visit`, `GET /stats` | Unique visitors: a salted daily hash, so each visitor counts once a day; no cookies, no raw IPs |
| `GET /notes`, `POST /notes` | Approved notes; new notes need a Cloudflare Turnstile token and land as *pending* |
| `/admin/notes…` | Approve / delete (Bearer `ADMIN_TOKEN`), used by the site's `/admin` page |

## First-time setup
```bash
./setup.sh   # guided: Cloudflare login, D1, schema, deploy, Turnstile, secrets, smoke test
```

## Day to day
```bash
npm run dev        # local API on :8787: applies the local schema, then starts (uses .dev.vars; copy .dev.vars.example)
npm run db:local   # apply migrations to the local D1
npm test           # Vitest inside the Workers runtime
npm run deploy     # publish changes to the Worker
```
Moderate notes at https://antimkhel.github.io/IAmA/admin (paste `ADMIN_TOKEN` once per browser).
