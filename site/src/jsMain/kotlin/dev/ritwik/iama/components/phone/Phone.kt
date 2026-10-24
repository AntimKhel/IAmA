package dev.ritwik.iama.components.phone

import androidx.compose.runtime.*
import dev.ritwik.iama.data.Profile
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.dom.B
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.I
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import kotlin.js.Date

class Clock(val time: String, val date: String, val shortDate: String)

/** The visitor's local time, refreshed every 20s. */
@Composable
fun rememberClock(): Clock {
    fun now(): Clock {
        val d = Date()
        val opts: dynamic = js("({hour: '2-digit', minute: '2-digit', hour12: false})")
        val dateOpts: dynamic = js("({weekday: 'long', month: 'long', day: 'numeric'})")
        val shortOpts: dynamic = js("({weekday: 'short', month: 'short', day: 'numeric'})")
        return Clock(
            d.asDynamic().toLocaleTimeString(js("undefined"), opts) as String,
            d.asDynamic().toLocaleDateString(js("undefined"), dateOpts) as String,
            d.asDynamic().toLocaleDateString(js("undefined"), shortOpts) as String,
        )
    }
    var clock by remember { mutableStateOf(now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(20_000)
            clock = now()
        }
    }
    return clock
}

@Composable
fun StatusBar(time: String) {
    Div({ classes("rr-status") }) {
        Span { Text(time) }
        Div({ classes("icons") }) {
            I({ classes("fa-solid", "fa-signal") })
            I({ classes("fa-solid", "fa-wifi") })
            I({ classes("fa-solid", "fa-battery-full") })
        }
    }
}

@Composable
private fun LockScreen(clock: Clock) {
    Div({ classes("rr-lock"); attr("aria-hidden", "true") }) {
        Div({ classes("rr-wall") })
        Div({ classes("rr-lock-inner"); style { property("position", "relative"); property("width", "100%") } }) {
            Div({ classes("rr-lock-time") }) { Text(clock.time) }
            Div({ classes("rr-lock-date") }) { Text(clock.date) }
            Div({ classes("rr-notif") }) {
                Div({ classes("ic") }) { I({ classes("fa-brands", "fa-linkedin-in") }) }
                Div {
                    B { Text("LinkedIn · now") }
                    Text("${Profile.firstName} is open to Tech Lead roles. Tap to connect.")
                }
            }
        }
        Div({ classes("rr-swipe") }) {
            I({ classes("fa-solid", "fa-chevron-up") })
            Span { Text("Swipe up to open") }
        }
    }
}

@Composable
fun HomeScreen(clock: Clock) {
    Div({ classes("rr-home") }) {
        Div({ classes("rr-glance"); style { property("--i", 0) } }) {
            Div({ classes("d") }) { Text(clock.shortDate) }
            Div({ classes("t") }) { Text("Hi, I'm ${Profile.firstName}. Tap around.") }
        }
        Div({ classes("rr-widget-pill"); style { property("--i", 1) } }) {
            Div({ classes("ring") })
            Div {
                Div({ classes("big") }) { Text("82%") }
                Text("obfuscated after R8 cleanup")
            }
        }
        Div({ classes("rr-grid") }) {
            Apps.grid.forEachIndexed { i, app -> AppIcon(app, i + 2) }
        }
        Dock(index = Apps.grid.size + 2)
    }
}

/** The Pixel-style device: frame, punch-hole, lock screen that unlocks into the launcher. */
@Composable
fun PhoneFrame() {
    val clock = rememberClock()
    Div({ classes("rr-phone") }) {
        Div({ classes("rr-btn-side", "power") })
        Div({ classes("rr-btn-side", "vol") })
        Div({ classes("rr-screen") }) {
            Div({ classes("rr-screen-on") }) {
                Div({ classes("rr-wall") })
                StatusBar(clock.time)
                HomeScreen(clock)
                LockScreen(clock)
                Div({ classes("rr-gesture") })
            }
            Div({ classes("rr-punch") })
            Div({ classes("rr-glare") })
        }
    }
}
