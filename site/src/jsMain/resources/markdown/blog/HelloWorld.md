---
title: "Hello World — Why I Built This Site in Kotlin"
date: "March 29, 2026"
description: "An Android developer's notes on building a portfolio with Compose for Web and Kobweb: what maps cleanly from Android, what doesn't, and the gotchas that cost me an evening each."
tags: Kotlin, Kobweb, Web
---

> I build Android apps for a living, so I built this site the way I'd build an app: **Kotlin, composable functions and state** — via Compose HTML and the Kobweb framework. Most of what I know from Jetpack Compose carried over. The rest is below, gotchas included.

## Why not React or Next.js?

They're great, and they power most of the web. But I already think in composables, modifiers and state, Kotlin is the language I'm fastest in, and I wanted a side project that sharpened skills I use every day rather than a new stack to maintain.

[Kobweb](https://kobweb.varabyte.com) sits on top of Compose HTML and adds the things a real site needs: file-based routing, a component and theming layer called Silk, markdown pages, and static export.

## What maps from Android — and what doesn't

| Jetpack Compose (Android) | Kobweb (web) |
|---|---|
| `@Composable` functions | The same — Compose HTML renders to the DOM |
| `remember`, `mutableStateOf` | Identical |
| `Modifier` chains | Kobweb `Modifier` chains, compiled to CSS |
| `dp` | roughly CSS `px` (and `sp` ≈ `rem`) |
| `LazyColumn` | A regular column — **no virtualisation** unless you build it |
| `NavHost` and routes | File-based routing with `@Page` |
| `MaterialTheme` | Silk theme, color modes and `CssStyle` |

The surprise wasn't how much carried over; it was how quickly I wanted to drop down to plain CSS for anything animated. Modifiers are perfect for layout. For choreography, a stylesheet driven by CSS variables is clearer.

## Gotchas that cost me an evening each

**GitHub Pages lives under a sub-path.** This site is served from `/IAmA/`, not `/`. Kobweb handles that with a `basePath` in `.kobweb/conf.yaml`, and most helpers (`Image`, Silk `Link`) prefix it for you. The exception: links to *files*. A `Link` to `/resume.pdf` is treated as a route, so I use a plain anchor with `BasePath.prependTo(...)` and a `download` attribute instead.

**Posts should index themselves.** I didn't want to edit a Kotlin list every time I wrote a post, so the build script reads each markdown file's front matter and generates the blog index — title, date, tags, reading time, and even a reference to the post's composable so it can render inside other components:

```kotlin
// site/build.gradle.kts (simplified excerpt)
kobweb {
    markdown {
        process.set { entries ->
            val posts = entries.filter { it.filePath.startsWith("blog/") }
            generateKotlin("dev/ritwik/iama/blog/BlogEntries.kt", buildBlogIndex(posts))
        }
    }
}
```

**The head is static.** Fonts, meta tags and stylesheets added at runtime end up duplicated in a static export. Anything that belongs in `<head>` goes in `kobweb { app { index { head.add { … } } } }` at build time.

## Shipping it

`kobweb export --layout static` produces plain HTML, CSS and JS; a GitHub Actions workflow builds and deploys it to GitHub Pages on every push to `main`. No server, no bill.

## Update — October 2026: the redesign

The site you're reading is version two. It boots like a phone, the phone dives into its own screen as you scroll, and the icons open real apps — including this blog. A few notes from building it:

- **Animation without recomposition.** A single `requestAnimationFrame` loop in Kotlin writes scroll and pointer progress into CSS custom properties; the stylesheet does the rest. Compose only re-renders when real state changes.
- **Markdown anywhere.** Because the generated blog index holds each post's composable, the Blog app inside the phone renders the actual post, not a summary.
- **Light and dark mode** follow the OS until you toggle them.
- There's a dev mode. Press the backtick key on the home page.

## More from the blog

- [From 24% to 82%: Untangling R8 Keep Rules in a 500M-Install App](/blog/r8-story)
- [Rolling Your Own React Native OTA](/blog/ota-updates-react-native)
- [One Theme Token, Two Runtimes](/blog/compose-design-system)
