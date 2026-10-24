---
title: "From 24% to 82%: Untangling R8 Keep Rules in a 500M-Install App"
date: "October 24, 2026"
description: "R8 full mode was on, yet three quarters of MyJio shipped un-obfuscated. How we measured keep-rule blast radius, fixed rules we didn't own, and survived the crashes along the way."
tags: Android, R8, Performance, Gradle
---

> R8 was configured "correctly", yet only about **25%** of MyJio's code was being obfuscated or optimized. The culprit wasn't our setup — it was a handful of over-broad keep rules shipped *inside dependencies*. We built a small analyzer around R8's keep-radius dump, removed or neutralised the worst rules (including with a Gradle transform for AARs we didn't control), and took obfuscation from **24.6% to 82.5%** — about **40 MB less dex** in a controlled build. This work is rolling out now; here's how it went, crashes included.

## The puzzle

MyJio is a big hybrid Android app: a native Jetpack Compose shell, React Native journeys, WebViews, and dozens of SDKs. On paper, our release build had every switch flipped:

- `minifyEnabled true` and `shrinkResources true`
- `proguard-android-optimize.txt`
- R8 **full mode** (`android.enableR8.fullMode=true`)
- `-repackageclasses ''` and `-allowaccessmodification`

So why did `mapping.txt` still show huge swathes of classes keeping their original names? And why did decompiled release builds read almost like source?

"R8 is on" turned out to be a very different statement from "R8 is allowed to do its job".

## Measuring what R8 actually keeps

Guessing from `mapping.txt` gets you nowhere in an app this size. What we needed was the answer to one question: **for every keep rule, how much of the app does it pin, and what does it stop R8 from doing?**

Newer versions of R8 can dump exactly that — a *keep radius* (or blast radius) report. We pinned a newer R8 only for analysis builds, so normal release builds kept the R8 bundled with AGP:

```kotlin
// Root build script (simplified): a newer R8 only when -Pr8Analyze is passed.
// It has to sit ahead of the Android Gradle Plugin on the buildscript classpath.
buildscript {
    dependencies {
        if (providers.gradleProperty("r8Analyze").isPresent) {
            classpath("com.android.tools:r8:9.4.14")
        }
    }
}
```

Then one analysis build writes the report:

```bash
./gradlew :app:assembleRelease -Pr8Analyze --no-configuration-cache \
  -Pandroid.experimental.r8.runInSeparateProcess=false \
  -Dcom.android.tools.r8.dumpkeepradiustodirectory=build/r8/radius \
  -Dcom.android.tools.r8.dumpkeepradiushtmltodirectory=build/r8/html
```

That `runInSeparateProcess=false` line cost us an afternoon. By default AGP may run R8 in a separate worker JVM, which doesn't inherit your `-D` system properties — so on CI the report was *silently never written*.

The dump is a protobuf. A small Python script turns it into JSON and computes a score for each of the three things R8 does:

> **score = 100 − (items kept by rules that forbid it) ÷ (live classes + fields + methods)**

It also ranks rules by blast radius, lists rules that are fully subsumed by others, and diffs against the previous run — so every change to a `.pro` file got a number attached.

The first run was sobering: **24.64%** obfuscation, **24.60%** optimization, **24.65%** shrinking, and **521** redundant rules.

## The villains

Sort rules by blast radius and the story writes itself. Five rules did almost all the damage, and four of them didn't come from our `proguard-rules.pro` at all:

{{{ .components.blog.BlastRadiusChart }}}

The key insight: **consumer rules shipped inside an AAR are merged into your app's configuration, and there is no "un-keep".** One SDK's `-keep public class *` quietly switched off obfuscation for every public member in the entire app — ours included. Look at the merged result yourself with `-printconfiguration`; it's humbling.

Yes, our own OTA module was on the list twice. Nobody gets to feel superior here.

## Fixing the rules we owned

We worked in small, measurable stages — one analyzer run per change:

1. **Delete dead files.** Three `.pro` files weren't wired into any build.
2. **Remove keeps that libraries already ship.** Glide, OkHttp, Gson, Coroutines, Room, Lottie, Retrofit and Firebase all bring their own consumer rules. Ours were duplicates at best and broader at worst.
3. **Remove keeps for libraries that were no longer in the dependency graph.**
4. **Narrow `{ *; }` to what reflection actually needs.** Most model keeps only need fields: `-keepclassmembers class … { <fields>; }`.
5. **Drop the blanket Parcelable keep.** `proguard-android-optimize.txt` already keeps the `CREATOR` field, which is all Android needs.
6. **Fix our own OTA module at the source.** Its consumer rules went from pinning roughly 580k members to **17 scoped rules with no broad keeps**. That single release took the score from 45% to 68%.

The trickiest one was a "Gson safety net" — a package-wide rule that pinned about **50,000 fields** in 10,000 classes just in case something was deserialised by reflection. We replaced it with a conditional rule plus a short explicit list:

```proguard
# Any class that has a @SerializedName field keeps all its fields and its no-arg constructor
-if class * { @com.google.gson.annotations.SerializedName <fields>; }
-keepclassmembers class <1> {
    <fields>;
    <init>();
}
```

That rule's footprint dropped from 4.42% of the app to 0.89%.

## Fixing the rules we didn't own

The right fix for a vendor's rule is for the vendor to fix it, so we sent each SDK owner a short, specific ask: *this rule, this blast radius, here's a scoped replacement*. That takes time.

In the meantime we wrote a **Gradle artifact transform** that rewrites the `proguard.txt` inside each AAR before R8 ever sees it:

```kotlin
// Simplified — the real one has more guard rails and a kill switch (-PstripBroadConsumerRules=false)
abstract class StripBroadKeeps : TransformAction<TransformParameters.None> {
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val aar = inputArtifact.get().asFile
        val out = outputs.file(aar.nameWithoutExtension + "-stripped.aar")
        ZipFile(aar).use { zip ->
            ZipOutputStream(out.outputStream()).use { zos ->
                for (entry in zip.entries()) {
                    zos.putNextEntry(ZipEntry(entry.name))
                    val bytes = zip.getInputStream(entry).readBytes()
                    zos.write(if (entry.name == "proguard.txt") strip(bytes.decodeToString()).encodeToByteArray() else bytes)
                    zos.closeEntry()
                }
            }
        }
    }
}
```

The `strip` function is deliberately conservative. It only removes bare wildcard keeps (`class *`, `**`) with broad bodies, and exact umbrella globs like `com.google.**`. It **never** touches rules scoped by `extends`, `implements` or an `@annotation`, never touches `native` methods, and leaves a `# [stripped broad consumer keep]` marker so the merged configuration stays auditable.

As SDKs shipped scoped rules, we retired the transform. What remains are a couple of overrides: AGP lets you ignore a specific dependency's consumer rules and supply your own vetted copy instead.

```kotlin
android {
    buildTypes {
        release {
            optimization {
                keepRules {
                    ignoreFrom("com.example:some-sdk") // its rules are re-added, minus the bad lines, in proguard-sdk-overrides.pro
                }
            }
        }
    }
}
```

## War stories

Removing keep rules is easy. Removing them *without breaking a 500M-install app* is the actual job. Every one of these shipped to an internal build first; every one was diagnosed from a release build with `mapping.txt` open in the other window.

### 1. A JNI crash at startup

**Symptom:** `NoSuchMethodError` the moment React Native started. **Cause:** native code looks up some Java methods *by name*; React Native protects them with `@DoNotStrip`-scoped keep rules — and the first version of our transform matched one of those rules as "broad" and dropped it. **Fix:** the transform now never touches annotation-scoped rules.

### 2. A home-screen crash inside a navigation library

**Symptom:** an NPE in `react-native-screens`. **Cause:** a view's `requestLayout()` runs *during the superclass constructor*, before a Kotlin non-null `val` is assigned. R8 full mode trusted the type system, decided the field could never be null, and removed the null check. **Fix:** targeted keep rules for that library's classes.

### 3. Non-null fields that were null

**Symptom:** screens that loaded forever. **Cause:** Gson creates objects with no no-arg constructor via `Unsafe.allocateInstance`, which **skips Kotlin default values** — so "non-null" fields arrived as null, and an exception handler swallowed the crash. **Fix:** keep `<init>()` alongside the fields (that's why it's in the rule above).

### 4. Icons that vanished

**Symptom:** fallback placeholders instead of icons. **Cause:** the optimized resource shrinker removed drawables that were only ever referenced by name through `Resources.getIdentifier()`. **Fix:** a `res/raw/keep.xml` with `tools:keep="@drawable/ic_ds_*"`, which survives even when CI overrides Gradle properties.

### 5. A spinner that never stopped

**Symptom:** one booking flow hung forever. **Cause:** Guava's `TypeToken` reads generic signatures at runtime; once the blanket `com.google.**` keep was gone, the signature it needed was optimised away. Confirmed with `dexdump -a`. **Fix:** a keep rooted at Guava's type-capture class instead of all of `com.google`.

### 6. A payment screen that crashed only in release

**Symptom:** a crash deep inside an XML security library on a payments screen. **Cause:** an attribution SDK shipped a bare `-adaptresourcefilecontents`, so R8 rewrote class names *inside Java resources* — 544 of 876 of them, including an XML config another library parsed. **Fix:** ignore that SDK's consumer rules and re-add the safe ones.

### 7. Widgets missing after an upgrade

**Symptom:** after an in-place upgrade, some home widgets were blank. **Cause:** R8 renamed the fields of a JSON payload sent from native code to React Native — `{"a": {…}}` instead of `{"account": {…}}`. Payloads typed as `value: Any` are invisible to any static scan for Gson models. **Fix:** `@Keep` on every class that crosses the native ↔ JS bridge.

### 8. R8 itself crashed

**Symptom:** the release build died with a `NullPointerException` *inside R8's dex builder*. **Fix path:** dump R8's exact inputs (`-Dcom.android.tools.r8.dumpinputtodirectory`), replay them through the R8 API, catch the NPE under `jdb`, bisect down to a single method — then work around it by calling a getter once instead of twice. Compilers are software too.

## Results

{{{ .components.blog.ObfuscationChart }}}

- **Obfuscation 24.64% → 82.45%** (optimization 81.96%, shrinking 82.52%)
- **Redundant rules 521 → 136**
- In a controlled pair of builds — same code, only obfuscation and optimization toggled — **APK −12 MB and dex −40 MB**, from 11 dex files down to 7

{{{ .components.blog.SizeChart }}}

One more data point that surprised people: with identical rules, upgrading from the R8 bundled with AGP 8.12 to R8 9.4 saved only **4.7% of dex** and changed obfuscation not at all. **Your keep rules matter far more than your R8 version.**

## A checklist

**If you ship an app:**

- Measure before you tune. A keep-radius report turns arguments into numbers.
- Read your *merged* configuration (`-printconfiguration`). That's what R8 actually runs.
- Hunt for `class *` and `**` with `{ *; }` bodies, in your rules *and* your dependencies'.
- Prefer `-keepclassmembers`, `<fields>`, and `-if` rules over blanket keeps.
- Put `@Keep` on anything that crosses a reflection or JSON bridge.
- Run the analyzer in CI, opt-in, and publish the report as a build artifact.

**If you ship a library:**

- Never ship `-keep class * …` or package-wide `{ *; }` in consumer rules.
- Scope rules to your own packages and to what you actually reflect on.
- Never ship `-dontobfuscate`, an unfiltered `-adaptresourcefilecontents`, or `-assumenosideeffects` for `android.util.Log` — one SDK we used silently stripped logging from the *entire host app*.

## What I'd do differently

Treat consumer rules as part of dependency review, the same way we treat licenses and permissions. A one-line keep rule in someone else's AAR cost us more app size than any feature we shipped that year — and it took a measurement tool, not a hunch, to see it.
