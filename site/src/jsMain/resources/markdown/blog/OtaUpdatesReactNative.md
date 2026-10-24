---
title: "Rolling Your Own React Native OTA: Delta Patches, Warm-Start Apply and Rollback"
date: "March 20, 2026"
description: "The design behind JioAirSync, the over-the-air update system I built for React Native at MyJio scale: own the bundle path, patch deltas, apply on the next warm start, and roll back automatically."
tags: React Native, OTA, Architecture
---

> Over-the-air updates for React Native come down to one hook — **owning where the JS bundle is loaded from** — plus a lot of discipline around it: download deltas with a full-bundle fallback, verify before you trust, **apply only on the next warm start**, and **roll back automatically** when a new bundle fails to come up. This is the design behind JioAirSync, which won a Skoch Gold award in 2026.

## Why not just use a hosted service?

Hosted OTA services are the right answer for most teams. We built our own because, at our scale, we needed things a hosted service couldn't give us:

- **Control over rollout:** targeting, staged percentages and instant pauses decided by *our* backend.
- **Our own delivery and data path:** bundles served from infrastructure we already ran and audited.
- **Release UX we could template:** silent updates for fixes, prompts for features, a hard stop for critical issues.
- **Many teams, many cadences:** journeys owned by different teams needed to ship independently.

If you're starting today, look at the current landscape first. Microsoft retired Visual Studio App Center in March 2025, and the ecosystem has moved — self-hostable CodePush-compatible servers and Expo Updates are both worth evaluating before you write a line of code.

## The one hook you need: own the bundle path

React Native lets the host app decide where the JavaScript bundle comes from. Everything else in an OTA system hangs off that decision. In the host, delegate it to an updater that falls back to the bundle shipped inside the APK:

```kotlin
// Simplified. The updater returns null when there is no verified, active OTA bundle.
override fun getJSBundleFile(): String? =
    bundleUpdater.activeBundlePath() // null → React Native loads the bundle embedded in the APK
```

That fallback is your safety net. If anything about the downloaded bundle is wrong, returning `null` gets the user back to a known-good build.

## Download: delta first, full bundle as fallback

JS bundles are big; most releases change a small part of them. So the client asks the update service for a **binary delta** against the bundle it's running, applies it with `bspatch`, and falls back to downloading the full bundle if anything goes wrong:

```kotlin
suspend fun fetch(update: UpdateInfo): File {
    val staged = runCatching { applyDelta(update) }   // bspatch against the current bundle
        .getOrElse { downloadFullBundle(update) }      // any failure → full zip
    check(sha256(staged) == update.sha256) { "Bundle hash mismatch, discarding" }
    unzipInto(staging = staged, dir = stagingDirFor(update.label))
    return staged
}
```

Two rules that saved us repeatedly:

1. **Never write into the active bundle directory.** Stage everything; switch only by flipping a pointer.
2. **Verify before you trust.** A hash check catches corruption. For tampering, I'd also recommend **signing bundles** and verifying the signature on device — TLS with certificate pinning protects the connection, not the artifact.

## Apply on the next warm start — never mid-session

Swapping JavaScript under a running user session is how you get half-old, half-new UI and state that no longer matches its code. So a downloaded bundle is marked **pending**, and only becomes active the next time the React Native host is (re)created.

It's slower than "update now", and it's worth it: users never see the app change under their thumb.

## Roll back automatically

A new bundle that crashes on start is the scariest OTA failure, because every launch loads the same broken bundle and crashes again — and the user can't reach the code that would fix it. The fix is a tiny state machine with a failure counter:

```text
DOWNLOADED ──(next warm start)──▶ PENDING ──(JS signals "ready")──▶ ACTIVE
                                     │
                                     └──(crash, or no "ready" before a timeout)──▶ failures += 1
                                                                                  │
                                              failures ≥ 2 ──▶ ROLLED_BACK (embedded bundle)
```

The JS side calls a native "I'm alive" method once the first screen renders. If that never happens — twice — the updater discards the bundle and goes back to the one inside the APK. No server round-trip needed; the device protects itself.

## Channels and a kill switch

- **One channel per build flavor.** Dev, QA and production builds never see each other's bundles.
- **The server decides targeting.** Rollout percentages and cohorts live in the backend, not in `if` statements on the client — so you can pause a rollout without shipping anything.
- **A build-time kill switch.** One flag disables OTA entirely for a build. You hope you never need it.

## Update UX is configuration, not code

Not every update deserves the same treatment, so the server sends a *template* along with the update:

| Template | When we use it |
|---|---|
| Silent | Bug fixes — apply on next warm start, say nothing |
| Toast | Small features — a gentle "updated" note |
| Modal | Bigger changes worth explaining |
| Force update | Critical fixes — a blocking screen with retry |

## Scaling to many teams: module federation

The last piece is organisational. When several teams own different journeys, a single bundle means a single release train. **Module federation** (via Re.Pack) lets a team ship its own remote bundle on its own schedule, loaded by the host at runtime — with the same verification, staging and rollback rules applied to each.

## Build or buy?

Build your own only if you can say yes to most of these:

- You need rollout control or data residency a hosted service can't provide.
- You can staff it like a product: monitoring, on-call, and a backend that owns targeting.
- You'll implement **staging, verification and automatic rollback** on day one — not "later".

Otherwise, use a maintained solution and spend the time on your app. An OTA system is a deployment pipeline that runs on a hundred million devices you can't SSH into; treat it with that level of respect.
