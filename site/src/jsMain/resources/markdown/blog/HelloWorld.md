---
title: "Hello World - Why I Built This Site with Kotlin"
---

# Hello World - Why I Built This Site with Kotlin

As an Android developer who's been writing Kotlin for 6+ years, the idea of building a website always felt like
stepping into foreign territory. HTML, CSS, JavaScript -- a whole different ecosystem to learn. But then I discovered
**Kobweb**, a framework that lets you build websites using **Compose for Web**, and everything clicked.

## Why Not Just Use React or Next.js?

Great question. React and Next.js are fantastic, and they power most of the modern web. But here's the thing:

- I already think in **Composables**, **Modifiers**, and **state management**
- Kotlin is my primary language, and I love its type safety
- I wanted to build something without learning an entirely new paradigm

## What is Kobweb?

Kobweb is like **Next.js for Kotlin**. It gives you:

- **File-based routing** -- just like Next.js, your file structure defines your URLs
- **Silk UI** -- a component library with `Row`, `Column`, `Box`, and `Modifier` (sound familiar?)
- **Markdown support** -- this very blog post is written in Markdown and auto-converted to a page
- **Static export** -- deploy to GitHub Pages, Netlify, or any static host

## The Android Developer Advantage

If you know Jetpack Compose, you already know 80% of what you need:

| Android Compose | Kobweb (Web) |
|---|---|
| `dp` | `cssRem` |
| `LazyColumn` | Regular `Column` |
| `MaterialTheme` | `SilkTheme` + `ColorMode` |
| `NavController` | File-based routing |
| `remember { mutableStateOf() }` | Same! |

## What's Next?

I'll be writing more about:

- Building design systems in Jetpack Compose
- My experience leading the MyJio app development
- Tips for Android developers exploring web technologies
- OTA updates for React Native apps

Stay tuned, and feel free to reach out on [LinkedIn](https://linkedin.com/in/ritwik-srivastava)!
