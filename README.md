# IAmA: Ritwik Raj Srivastava's portfolio

**Live:** https://antimkhel.github.io/IAmA/

A portfolio that behaves like an Android phone, written entirely in Kotlin with
[Kobweb](https://github.com/varabyte/kobweb) (Compose for Web). The site is a static export on GitHub Pages; a small
Cloudflare backend adds a visitor counter, a moderated guestbook and **Ask Ritwik**, an AI assistant that answers
questions from what's published on the site.

## What's in it

- **Hero phone**: a 3D phone with working apps (Work, Products, Blog, About, Skills, Awards, Contact, Résumé,
  Guestbook, Search, Ask). On phones, the visitor's screen *is* the home screen.
- **Ask Ritwik**: chat with an AI that has read the site, from the phone dock, the desktop launcher, Search, or the
  "Ask AI" chip in the nav (past the hero it widens and types out example questions). Answers stream in, with
  buttons that open the relevant app, product or post.
- **Search** (⌘K / Ctrl+K): apps, products, posts, companies, skills and quick actions.
- **Blog**: markdown posts, discovered automatically, with syntax highlighting and charts.
- **Visitor counter and guestbook**: notes appear after approval at `/admin`.
- **Dev mode**: the whole site re-skinned as terminals (press `` ` ``, or tap the build number in About 7 times).
- Light and dark themes, reduced-motion support, keyboard focus management.

## Architecture

```
                      ┌──────────────────────────────────────────────┐
 Browser ───────────▶ │ GitHub Pages: static Kobweb export           │
   │                  │ (HTML, iama.js, CSS, images, knowledge.json) │
   │                  └──────────────────────────────────────────────┘
   │                                   ▲ knowledge.json (cached 5 min)
   │  fetch (CORS: site origins only)  │
   └──────────────▶ Cloudflare Worker "iama-api" (backend/)
                      ├─ D1 (SQLite): counters, daily visitor hashes, notes, Ask usage
                      ├─ Turnstile siteverify: spam check for guestbook notes
                      └─ Workers AI: GLM-4.7-Flash answers for Ask Ritwik
```

Everything dynamic runs on Cloudflare's free tier. The site keeps working if the API is down: the counter, guestbook
and Ask hide or show a friendly message instead of breaking the page.

## Repository layout

| Path | What it is |
|---|---|
| `site/src/jsMain/kotlin/dev/ritwik/iama/data/Profile.kt` | Single source of truth for everything the site says about Ritwik |
| `site/src/jsMain/kotlin/dev/ritwik/iama/components/` | `hero/` (phone scene), `phone/` + `phone/apps/` (launcher and apps), `sections/`, `blog/`, `guestbook/`, `ask/` (the nav's Ask AI chip), `devmode/` |
| `site/src/jsMain/resources/markdown/blog/` | Blog posts (front matter: `title`, `date`, `description`, `tags`, optional `draft: true`) |
| `site/src/jsMain/resources/public/` | CSS, images, `resume.pdf` |
| `site/knowledge/profile.md` | What Ask Ritwik knows besides the posts (mirrors `Profile.kt`) |
| `site/build.gradle.kts` | Head tags, the generated blog index, and the `askKnowledge` task that publishes `knowledge.json` |
| `site/src/jsMain/kotlin/dev/ritwik/iama/data/Backend.kt` | API URL and Turnstile site key (local vs production) |
| `backend/` | The Cloudflare Worker (TypeScript), D1 migrations, Vitest tests, `setup.sh`, [its own README](backend/README.md) |
| `.github/workflows/deploy.yml` | Exports and publishes the site on every push to `main` |

## Running locally

Prerequisites: JDK 17+, the [Kobweb CLI](https://github.com/varabyte/kobweb-cli), Node.js 20+, and (for Ask) a
Cloudflare login via `npx wrangler login`.

```bash
# Site: http://localhost:8080/IAmA/
cd site && kobweb run

# API: http://127.0.0.1:8787
cd backend
cp .dev.vars.example .dev.vars   # local-only values; real secrets live in Cloudflare
npm install
npm run dev                      # applies local D1 migrations, then starts wrangler dev
```

On `localhost` the site calls the local API and uses Cloudflare's always-pass Turnstile test key. Ask uses the real
Workers AI model even locally (it shares the account's daily free allocation) and reads `knowledge.json` from the
Kobweb dev server.

## Deploying

- **Site:** push to `main`. GitHub Actions runs `kobweb export --layout static` and publishes to GitHub Pages; asset
  URLs are cache-busted with the commit SHA.
- **Worker:** from `backend/`, run `npm run db:remote` when there are new migrations, then `npm run deploy`.
  First-time setup (Cloudflare login, D1, Turnstile, secrets) is a guided script: `./setup.sh`.

Secrets (`TURNSTILE_SECRET`, `ADMIN_TOKEN`, `HASH_SALT`) live only in Cloudflare and are never committed.

## Backend API

| Endpoint | Purpose |
|---|---|
| `POST /visit`, `GET /stats` | Unique visitors, counted once a day per visitor with a salted daily hash |
| `GET /notes`, `POST /notes` | Approved guestbook notes; new notes need a Turnstile token and land as pending |
| `GET /admin/notes`, `POST /admin/notes/:id/approve`, `DELETE /admin/notes/:id` | Moderation (Bearer `ADMIN_TOKEN`), used by `/admin` |
| `POST /ask` | Ask Ritwik: `{ question, history }` → answer streamed as text; suggested buttons in `X-Ask-Actions` |

## How Ask Ritwik works

1. **The site publishes its knowledge.** The `askKnowledge` Gradle task bundles `site/knowledge/profile.md` and every
   non-draft blog post into `/knowledge.json` at build time.
2. **The Worker fetches it** and caches it for five minutes (keeping a stale copy if the site is unreachable), then
   splits it into small chunks: one per profile section and one per post section.
3. **Retrieval** is plain BM25 with a short synonym list. Every question gets the core facts plus up to four of the
   best-matching chunks (about 4,500 characters at most), so the model only sees relevant, public text.
4. **The answer** comes from GLM-4.7-Flash on Workers AI (thinking off, temperature 0.3, at most 320 tokens) under
   strict rules: only facts from the site, no guessed numbers, salary or personal details, nothing about Jio beyond
   the site, and a fixed reply for anything not covered. It streams to the browser as it's generated.
5. **The buttons** under an answer come from the chunks that were retrieved, not from the model, so they always open
   something real.

**Adding knowledge:** publish a blog post and Ask knows it within minutes of the site deploy; nothing in `backend/`
changes. For profile facts, update `Profile.kt` and `site/knowledge/profile.md` together.

**Cost and limits:** the Workers Free plan includes 10,000 neurons a day, and past that requests fail rather than
bill. Answers measured 5–11 neurons each, so the Worker caps Ask at 20 questions per visitor and 800 site-wide a
day. `npx wrangler tail` shows one usage line (tokens and neurons) per answer.

**Changing the model:** edit `ASK_MODEL` in `backend/wrangler.toml`, or try one locally with
`npx wrangler dev --var ASK_MODEL:<model>`. Models that "think" need `chat_template_kwargs: { enable_thinking: false }`
(already sent), or they spend the token budget on hidden reasoning.

## Privacy

No cookies and no raw IP addresses. Visitors are counted with a salted daily hash that is deleted after a day.
Guestbook notes keep a salted IP hash only for rate limiting. Ask questions are not stored: the Worker keeps a
per-visitor daily count and logs token usage, not question text. Questions are processed by Cloudflare Workers AI.

## Tests

```bash
cd backend && npm test        # Vitest inside the Workers runtime: counter, guestbook, admin, Ask (retrieval, streaming, limits)
cd backend && npm run typecheck
```

## License

[GPL-3.0](LICENSE)
