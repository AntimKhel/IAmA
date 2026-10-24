---
title: "One Theme Token, Two Runtimes: Design-System Parity Across Compose and React Native"
date: "March 25, 2026"
description: "In a hybrid app, users can't tell — and shouldn't be able to tell — which screens are Jetpack Compose and which are React Native. How a tiny theme contract keeps both in visual lockstep."
tags: Jetpack Compose, React Native, Design System
---

> In a hybrid app, the design system has to work in **two runtimes at once**. What made that tractable for us wasn't a big shared component library — it was a **tiny theme contract** with identical defaults on both sides, pushed from the native host into React Native on every mount, plus one strict rule about how components get wrapped.

## The problem with two runtimes

MyJio renders some screens in Jetpack Compose and others in React Native, chosen per screen by how often that journey changes. Users move between them dozens of times a session. If a button is 2dp rounder, a brand colour a shade off, or dark mode lags a frame behind on one side, the app feels stitched together.

So "the design system" can't just be a component library. It's a *contract* that two very different UI toolkits honour at the same time.

## A contract small enough to keep

The temptation is to share everything: spacing scales, typography ramps, every colour role. In practice, the more you share across runtimes, the more there is to drift. We kept the cross-runtime part deliberately tiny — a four-part theme token:

```kotlin
@Immutable
data class ThemeToken(
    val primary: String,   // brand palette name, e.g. "indigo"
    val secondary: String,
    val accent: String,
    val mode: Mode,        // LIGHT or DARK
)

val DefaultToken = ThemeToken(primary = "indigo", secondary = "slate", accent = "teal", mode = Mode.LIGHT)
```

Everything else — spacing, type, elevation, the actual colour values — is *derived* from that token by each runtime's design-system layer. The rule is that both sides ship **identical defaults**, so a screen rendered before any theme arrives still matches its neighbour.

## Compose: a theme, not a global object

On the Compose side the token lives in a `CompositionLocal`, provided once by an app-level theme wrapper. Static `object`s full of colours look simpler, but they break the moment you need dark mode, a server-driven theme, or a preview in a different palette.

```kotlin
val LocalThemeToken = staticCompositionLocalOf { DefaultToken }

@Composable
fun AppTheme(token: ThemeToken = DefaultToken, content: @Composable () -> Unit) {
    val colors = remember(token) { paletteFor(token) }   // derived, never hand-maintained per screen
    CompositionLocalProvider(LocalThemeToken provides token) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}
```

## React Native: the host pushes the token in

React Native screens are guests inside the native shell, so they don't decide the theme — they **receive** it. The host passes the current token in the initial props every time it mounts an RN surface:

```kotlin
// Simplified: the native host seeds every React Native mount with the live theme.
val initialProps = Bundle().apply {
    putBundle("theme", themeToken.toBundle())
    putString("session", sessionHandle)
}
```

On the JS side a `ThemeProvider` reads that prop and feeds the same derivation logic. Because the RN store is deliberately ephemeral and re-seeded on each mount, there's no stale theme to invalidate: change the theme natively, and the next RN screen simply mounts with the new one.

## Server-driven themes, derived dark mode

Brand campaigns and festivals change the palette several times a year, so the catalogue of themes is **served by the backend and cached to disk**, with the defaults as the offline fallback.

We don't ship hand-made dark variants. Each runtime derives dark mode from the light palette with the same rules — shift lightness, cap saturation, keep contrast ratios — which means a new campaign theme gets a correct dark mode for free, on both sides, with no extra design work.

## The wrapping rule

The most valuable thing we did wasn't technical; it was a rule everyone could remember:

1. **Use the design-system component** if one exists.
2. Otherwise, **use the existing wrapper** for it.
3. Otherwise, **write a new wrapper** — and only then.

Never wrap the underlying engine again "just for this screen". Every one-off wrapper is a future inconsistency, and in a hybrid app it's twice as expensive because the RN side needs a matching one.

## Migrating without a big bang

Design systems get major versions. Ours had two living side by side on the classpath for a while: new screens used v2, old screens stayed on v1, and adapters translated tokens between them. It's untidy, but it let us migrate screen by screen inside normal releases instead of freezing feature work for a rewrite.

## What I'd tell a team starting today

- **Make the shared contract tiny.** Share decisions, not implementations.
- **Identical defaults on both runtimes**, so the first frame always matches.
- **Theme lives in composition, not globals**, in both Compose and React.
- **Derive dark mode; don't hand-draw it.**
- **Write the wrapping rule down** and review against it — it prevents more drift than any linter.
