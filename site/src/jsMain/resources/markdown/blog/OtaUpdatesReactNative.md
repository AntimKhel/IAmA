---
title: "OTA Updates for React Native Without CodePush"
date: "March 20, 2026"
description: "We built our own over-the-air update system for React Native at Jio. Here's the architecture and why we chose this over CodePush."
tags: React Native, OTA, Architecture
---

# OTA Updates for React Native Without CodePush

At Jio, we needed a way to push JavaScript bundle updates to our React Native modules without going through
the full Play Store review cycle. Microsoft's CodePush was the obvious choice -- but we decided to build our own.
Here's why and how.

## Why Not CodePush?

CodePush is great for small teams, but at our scale:

- **Control**: We needed full control over rollout percentages, targeting, and rollback
- **Infrastructure**: We already had CDN and analytics infrastructure
- **Compliance**: Storing bundles on Microsoft's servers raised compliance concerns
- **Customization**: We needed features CodePush didn't support (A/B testing bundles, device-specific targeting)

## The Architecture

Our OTA system has four components:

### 1. Bundle Server
A simple API that serves bundle metadata:

```
GET /api/bundles/latest?app=myjio&platform=android&version=5.2.0

Response:
{
  "bundleId": "b-2026-03-15-001",
  "url": "https://cdn.jio.com/bundles/myjio/b-2026-03-15-001.zip",
  "hash": "sha256:abc123...",
  "minAppVersion": "5.0.0",
  "rolloutPercentage": 25,
  "mandatory": false
}
```

### 2. Android Native Bridge
A Kotlin module that handles downloading, verifying, and installing bundles:

```kotlin
class OtaBundleManager(
    private val context: Context,
    private val api: OtaApi,
) {
    suspend fun checkForUpdate(): OtaUpdate? {
        val current = getCurrentBundleVersion()
        val latest = api.getLatestBundle(
            app = BuildConfig.APP_ID,
            platform = "android",
            version = BuildConfig.VERSION_NAME,
        )
        return if (latest.bundleId != current && isInRollout(latest)) {
            latest
        } else null
    }

    suspend fun downloadAndApply(update: OtaUpdate) {
        val file = downloadBundle(update.url)
        verifyHash(file, update.hash)
        installBundle(file)
        // React Native will pick up the new bundle on next reload
    }
}
```

### 3. Dashboard
An internal web app where our release team can:
- Upload new bundles
- Set rollout percentages (start at 5%, ramp to 100%)
- Monitor crash rates per bundle version
- Instantly rollback if something goes wrong

### 4. Analytics Pipeline
Every bundle load reports success/failure metrics. If the crash rate for a new bundle exceeds 2%,
the system auto-rollbacks to the previous stable version.

## Key Design Decisions

**Differential updates**: Instead of downloading the full bundle every time (2-5 MB), we compute
binary diffs between versions. Most updates are 50-200 KB.

**Integrity verification**: Every bundle is signed with our private key. The native side verifies
the signature before installing. This prevents tampering even if the CDN is compromised.

**Graceful fallback**: If the new bundle crashes on startup, the native side detects this and
automatically falls back to the embedded bundle that shipped with the APK.

## Results

- **Time to production**: From 3-5 days (Play Store review) to **< 30 minutes**
- **Update adoption**: 95% of users on latest JS bundle within 24 hours
- **Reliability**: Zero incidents in 18 months of operation
- **Bundle size**: Average update is 120 KB (vs 3.5 MB full bundle)

## Should You Build Your Own?

Honestly, for most teams: **no**. Use CodePush or Expo Updates. They're battle-tested and free.

Build your own only if you need:
- Custom rollout logic beyond what CodePush offers
- Full control over infrastructure for compliance reasons
- Integration with existing internal tooling
- Scale that exceeds CodePush's free tier significantly

The maintenance cost is real -- we have one engineer spending ~20% of their time on this system.
But for our use case at Jio's scale, the control and reliability are worth it.
