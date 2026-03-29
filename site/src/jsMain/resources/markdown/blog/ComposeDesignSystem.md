---
title: "Building a Design System with Jetpack Compose"
---

# Building a Design System with Jetpack Compose

When we decided to unify the UI across MyJio, JioMart, and other Jio apps, we needed a design system that was
**composable, themeable, and scalable**. Jetpack Compose was the obvious choice.

## Why a Design System?

At Jio, we have multiple Android apps maintained by different teams. Without a shared design language:

- Each app had slightly different button styles, spacing, colors
- Designers had to specify every detail for every app
- Bug fixes in one component weren't shared across apps
- Onboarding new developers was slow

## Architecture

We structured the Jio Design System (JDS) into three layers:

### 1. Foundation Layer
The lowest level -- tokens that define the visual language:

```kotlin
object JdsTokens {
    object Colors {
        val primary = Color(0xFF0A1172)
        val secondary = Color(0xFF1B8A2E)
        // ... 60+ color tokens
    }
    object Spacing {
        val xs = 4.dp
        val sm = 8.dp
        val md = 16.dp
        val lg = 24.dp
        val xl = 32.dp
    }
    object Typography {
        val headlineLarge = TextStyle(
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 40.sp,
        )
    }
}
```

### 2. Component Layer
Reusable composables that consume tokens:

```kotlin
@Composable
fun JdsButton(
    text: String,
    onClick: () -> Unit,
    variant: ButtonVariant = ButtonVariant.Primary,
    size: ButtonSize = ButtonSize.Medium,
) {
    Button(
        onClick = onClick,
        colors = variant.toColors(),
        shape = RoundedCornerShape(size.cornerRadius),
        contentPadding = size.toPadding(),
    ) {
        Text(text, style = size.toTextStyle())
    }
}
```

### 3. Pattern Layer
Compositions of components that solve recurring UX patterns -- headers, empty states, list items, cards.

## Lessons Learned

**Start with tokens, not components.** We initially jumped straight to building buttons and cards. But without a
solid token foundation, we kept going back to change fundamentals. Tokens first, then components.

**Compose previews are your best friend.** We set up `@Preview` for every component variant -- light/dark mode,
all sizes, all states (enabled, disabled, loading, error). This caught visual regressions before they shipped.

**Snapshot testing catches what your eyes miss.** We use Paparazzi for snapshot tests. Every PR that touches a
component auto-generates before/after screenshots. Reviewers can spot unintended visual changes instantly.

**Version your design system.** We publish JDS as an internal Maven artifact. Apps depend on a specific version
and can upgrade at their own pace. Breaking changes go through a deprecation cycle.

## Results

After 6 months:
- **40% faster** feature development (developers grab existing components)
- **Zero** "why does this button look different in JioMart" bugs
- **3x faster** designer-to-developer handoff
- New developers productive in **days, not weeks**

The design system is now the foundation for all new Jio Android apps. If you're building multiple apps
at your company, I can't recommend this approach enough.
