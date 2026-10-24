package dev.ritwik.iama.components.blog

import androidx.compose.runtime.Composable
import dev.ritwik.iama.components.sections.reveal
import org.jetbrains.compose.web.dom.*

// Charts embedded in blog posts via Kobweb calls, e.g. `{{{ .components.blog.ObfuscationChart }}}`.
// Figures come from the R8 analyzer runs described in the R8 post.

private class Point(val label: String, val sub: String, val value: Double)

private val obfuscation = listOf(
    Point("Baseline", "R8 full mode already on", 24.64),
    Point("Our rules", "cleanup + AAR transform", 45.31),
    Point("Own OTA module", "fixed at the source", 67.68),
    Point("Vendor SDKs", "scoped consumer rules", 78.16),
    Point("Gson net", "50k fields → targeted", 80.78),
    Point("Final", "transform retired", 82.45),
)

@Composable
fun ObfuscationChart() {
    Div({ classes("chart", "chart-steps"); reveal("none") }) {
        Div({ classes("chart-head") }) {
            Div({ classes("chart-title") }) { Text("Obfuscation score, per milestone") }
            Div({ classes("chart-sub") }) { Text("Share of live classes, fields and methods R8 was allowed to rename") }
        }
        Div({ classes("steps") }) {
            obfuscation.forEachIndexed { i, p ->
                Div({ classes("step"); if (i == obfuscation.lastIndex) classes("final"); style { property("--v", p.value / 100); property("--i", i) } }) {
                    Div({ classes("step-val") }) { Text("${p.value}%") }
                    Div({ classes("step-track") }) { Div({ classes("step-bar") }) }
                    Div({ classes("step-label") }) { Text(p.label) }
                    Div({ classes("step-sub") }) { Text(p.sub) }
                }
            }
        }
    }
}

private class Offender(val rule: String, val source: String, val value: Double, val items: String)

private val offenders = listOf(
    Offender("-keep public class * { public protected *; }", "an engagement SDK", 48.70, "886,932 items"),
    Offender("-keep class com.<vendor>.** { *; }", "a TV SDK", 18.71, "340,660 items"),
    Offender("-keep class com.google.** { *; }", "our OTA module + an ads SDK", 17.17, "312,718 items"),
    Offender("-keep class * implements java.io.Serializable { *; }", "our OTA module", 14.71, "267,888 items"),
    Offender("-keep class * implements android.os.Parcelable { *; }", "the app itself", 5.20, "94,692 items"),
)

@Composable
fun BlastRadiusChart() {
    Div({ classes("chart", "chart-radius"); reveal("none") }) {
        Div({ classes("chart-head") }) {
            Div({ classes("chart-title") }) { Text("Blast radius of the worst keep rules (baseline)") }
            Div({ classes("chart-sub") }) { Text("Percent of the app each single rule pinned against obfuscation and optimization") }
        }
        offenders.forEachIndexed { i, o ->
            // Scale to 58% rather than 50% to leave headroom for the value label after the bar.
            Div({ classes("radius-row"); style { property("--v", o.value / 58); property("--i", i) } }) {
                Div({ classes("radius-text") }) {
                    Code { Text(o.rule) }
                    Span({ classes("radius-src") }) { Text("${o.source} · ${o.items}") }
                }
                Div({ classes("radius-track") }) {
                    Div({ classes("radius-bar") })
                    Span({ classes("radius-val") }) { Text("${o.value}%") }
                }
            }
        }
    }
}

private class SizePair(val label: String, val before: Double, val after: Double, val unit: String)

private val sizes = listOf(
    SizePair("APK", 131.06, 118.85, "MB"),
    SizePair("Dex (uncompressed)", 111.33, 71.16, "MB"),
    SizePair("Dex files", 11.0, 7.0, ""),
)

@Composable
fun SizeChart() {
    Div({ classes("chart", "chart-size"); reveal("none") }) {
        Div({ classes("chart-head") }) {
            Div({ classes("chart-title") }) { Text("Same code, obfuscation and optimization off vs on") }
            Div({ classes("chart-sub") }) { Text("Controlled pair of release builds; native libraries identical in both") }
        }
        Div({ classes("size-legend") }) {
            Span({ classes("before") }) { Text("-dontobfuscate -dontoptimize") }
            Span({ classes("after") }) { Text("full R8") }
        }
        sizes.forEachIndexed { i, s ->
            val max = s.before
            Div({ classes("size-row"); style { property("--i", i) } }) {
                Div({ classes("size-label") }) {
                    Text(s.label)
                    val delta = s.after - s.before
                    Span({ classes("size-delta") }) {
                        Text(if (s.unit.isEmpty()) "${delta.toInt()}" else "${(kotlin.math.round(delta * 10) / 10)} ${s.unit}")
                    }
                }
                Div({ classes("size-bars") }) {
                    Div({ classes("size-bar", "before"); style { property("--v", s.before / max) } }) {
                        Span { Text(fmt(s.before, s.unit)) }
                    }
                    Div({ classes("size-bar", "after"); style { property("--v", s.after / max) } }) {
                        Span { Text(fmt(s.after, s.unit)) }
                    }
                }
            }
        }
    }
}

private fun fmt(v: Double, unit: String) = if (unit.isEmpty()) v.toInt().toString() else "${kotlin.math.round(v * 10) / 10} $unit"
