package dev.ritwik.iama.components.devtools

import androidx.compose.runtime.*
import dev.ritwik.iama.util.Logcat
import dev.ritwik.iama.util.jsObject
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event

/**
 * Android's Developer options, for real: unlocked by tapping the build number seven times, and every toggle
 * actually does what it says on this site. Remembered in this browser.
 */
object DevOptions {
    private const val KEY = "rr-devopts"

    /** Android's animation scale choices (0 = "Animation off"). */
    val scales = listOf(0.0, 0.5, 1.0, 1.5, 2.0, 5.0, 10.0)

    var unlocked by mutableStateOf(false); private set
    var enabled by mutableStateOf(false); private set
    var layoutBounds by mutableStateOf(false); private set
    var showTaps by mutableStateOf(false); private set
    var pointerLocation by mutableStateOf(false); private set
    var gpuBars by mutableStateOf(false); private set
    var strictMode by mutableStateOf(false); private set
    var predictiveBack by mutableStateOf(true); private set
    var windowScale by mutableStateOf(1.0); private set
    var transitionScale by mutableStateOf(1.0); private set
    var animatorScale by mutableStateOf(1.0); private set

    init {
        runCatching {
            val saved = window.localStorage.getItem(KEY) ?: return@runCatching
            val o = JSON.parse<dynamic>(saved)
            unlocked = o.unlocked == true
            enabled = o.enabled == true
            layoutBounds = o.layoutBounds == true
            showTaps = o.showTaps == true
            pointerLocation = o.pointerLocation == true
            gpuBars = o.gpuBars == true
            strictMode = o.strictMode == true
            predictiveBack = o.predictiveBack != false
            windowScale = (o.windowScale as? Number)?.toDouble() ?: 1.0
            transitionScale = (o.transitionScale as? Number)?.toDouble() ?: 1.0
            animatorScale = (o.animatorScale as? Number)?.toDouble() ?: 1.0
        }
    }

    private fun save() {
        runCatching {
            window.localStorage.setItem(
                KEY,
                JSON.stringify(
                    jsObject(
                        "unlocked" to unlocked, "enabled" to enabled, "layoutBounds" to layoutBounds,
                        "showTaps" to showTaps, "pointerLocation" to pointerLocation, "gpuBars" to gpuBars,
                        "strictMode" to strictMode, "predictiveBack" to predictiveBack, "windowScale" to windowScale,
                        "transitionScale" to transitionScale, "animatorScale" to animatorScale,
                    )
                ),
            )
        }
    }

    private fun setting(name: String, value: Any) {
        save()
        Logcat.i("Settings", "Global.$name=$value")
    }

    /** Tapping the build number seven times. Returns false if already unlocked. */
    fun unlock(): Boolean {
        if (unlocked) return false
        unlocked = true
        enabled = true
        setting("development_settings_enabled", 1)
        return true
    }

    fun setEnabled(on: Boolean) { enabled = on; setting("development_settings_enabled", if (on) 1 else 0) }
    fun setLayoutBounds(on: Boolean) { layoutBounds = on; setting("debug_layout", if (on) 1 else 0) }
    fun setShowTaps(on: Boolean) { showTaps = on; setting("show_touches", if (on) 1 else 0) }
    fun setPointerLocation(on: Boolean) { pointerLocation = on; setting("pointer_location", if (on) 1 else 0) }
    fun setGpuBars(on: Boolean) { gpuBars = on; setting("debug_hwui_profile", if (on) "visual_bars" else "false") }
    fun setStrictMode(on: Boolean) { strictMode = on; setting("strict_mode_visual", if (on) 1 else 0) }
    fun setPredictiveBack(on: Boolean) { predictiveBack = on; setting("enable_back_animation", if (on) 1 else 0) }
    fun setWindowScale(x: Double) { windowScale = x; setting("window_animation_scale", x) }
    fun setTransitionScale(x: Double) { transitionScale = x; setting("transition_animation_scale", x) }
    fun setAnimatorScale(x: Double) { animatorScale = x; setting("animator_duration_scale", x) }

    // What's actually in effect: everything is off while the master switch is.
    val activeBounds get() = enabled && layoutBounds
    val activeTaps get() = enabled && showTaps
    val activePointer get() = enabled && pointerLocation
    val activeGpu get() = enabled && gpuBars
    val activeStrict get() = enabled && strictMode
    val backAnimations get() = !enabled || predictiveBack
    val effectiveWindowScale get() = if (enabled) windowScale else 1.0
    val effectiveTransitionScale get() = if (enabled) transitionScale else 1.0
    val effectiveAnimatorScale get() = if (enabled) animatorScale else 1.0
}

fun scaleLabel(x: Double): String = if (x == 0.0) "Animation off" else "Animation scale ${if (x % 1.0 == 0.0) x.toInt().toString() else x.toString()}x"

/**
 * Applies whatever Developer options are on. Lives in the page layout so it works on every page and over
 * every app window.
 */
@Composable
fun DevOverlays() {
    // Show layout bounds: an html class does it (see rr.css).
    val bounds = DevOptions.activeBounds
    LaunchedEffect(bounds) { document.documentElement?.classList?.toggle("dev-bounds", bounds) }

    AnimationScales(DevOptions.effectiveWindowScale, DevOptions.effectiveTransitionScale, DevOptions.effectiveAnimatorScale)
    if (DevOptions.activeTaps) ShowTaps()
    if (DevOptions.activePointer) PointerLocation()
    if (DevOptions.activeGpu) GpuBars()
    if (DevOptions.activeStrict) StrictModeFlash()
}

/**
 * Window, transition and animator duration scales, like Android's three: every CSS transition, CSS animation
 * and Web Animation on the page gets its playback rate set as it starts. App windows' own open/close
 * animations follow the window scale.
 */
@Composable
private fun AnimationScales(window: Double, transition: Double, animator: Double) {
    DisposableEffect(window, transition, animator) {
        if (window == 1.0 && transition == 1.0 && animator == 1.0) return@DisposableEffect onDispose { resetRates() }
        fun rateFor(anim: dynamic): Double {
            val target = anim.effect?.target as? HTMLElement
            val scale = when {
                target != null && (target.classList.contains("rr-appwin") || target.classList.contains("rr-appwin-splash")) -> window
                anim.transitionProperty != null -> transition // a CSSTransition; CSSAnimations have animationName
                else -> animator
            }
            return if (scale == 0.0) 1000.0 else 1.0 / scale
        }
        fun applyAll() {
            (document.asDynamic().getAnimations() as Array<dynamic>).forEach { anim -> anim.playbackRate = rateFor(anim) }
        }
        val onStart: (Event) -> Unit = { e ->
            val el = e.target.asDynamic()
            if (el?.getAnimations != null) (el.getAnimations() as Array<dynamic>).forEach { anim -> anim.playbackRate = rateFor(anim) }
        }
        val events = listOf("animationstart", "transitionrun", "transitionstart")
        events.forEach { document.addEventListener(it, onStart, true) }
        applyAll()
        onDispose {
            events.forEach { document.removeEventListener(it, onStart, true) }
            resetRates()
        }
    }
}

private fun resetRates() {
    runCatching { (document.asDynamic().getAnimations() as Array<dynamic>).forEach { it.playbackRate = 1.0 } }
}

/** Show taps: a soft dot under each pointer while it's down. */
@Composable
private fun ShowTaps() {
    DisposableEffect(Unit) {
        val layer = document.createElement("div") as HTMLElement
        layer.className = "dev-layer"
        document.body?.appendChild(layer)
        val dots = mutableMapOf<Int, HTMLElement>()
        fun place(dot: HTMLElement, e: dynamic) {
            dot.style.transform = "translate(${e.clientX}px, ${e.clientY}px)"
        }
        val down: (Event) -> Unit = { ev ->
            val e = ev.asDynamic()
            val dot = (document.createElement("div") as HTMLElement).apply { className = "dev-tap" }
            place(dot, e)
            layer.appendChild(dot)
            dots[e.pointerId as Int] = dot
        }
        val move: (Event) -> Unit = { ev -> val e = ev.asDynamic(); dots[e.pointerId as Int]?.let { place(it, e) } }
        val up: (Event) -> Unit = { ev ->
            val e = ev.asDynamic()
            dots.remove(e.pointerId as Int)?.let { dot ->
                dot.classList.add("out")
                window.setTimeout({ dot.remove() }, 300)
            }
        }
        window.addEventListener("pointerdown", down, true)
        window.addEventListener("pointermove", move, true)
        listOf("pointerup", "pointercancel").forEach { window.addEventListener(it, up, true) }
        onDispose {
            window.removeEventListener("pointerdown", down, true)
            window.removeEventListener("pointermove", move, true)
            listOf("pointerup", "pointercancel").forEach { window.removeEventListener(it, up, true) }
            layer.remove()
        }
    }
}

/** Pointer location: Android's top status line, crosshair and trail. */
@Composable
private fun PointerLocation() {
    DisposableEffect(Unit) {
        val layer = document.createElement("div") as HTMLElement
        layer.className = "dev-layer dev-pointer"
        layer.innerHTML = """<div class="bar"></div><div class="h"></div><div class="v"></div><canvas class="trail"></canvas>"""
        document.body?.appendChild(layer)
        val bar = layer.querySelector(".bar") as HTMLElement
        val h = layer.querySelector(".h") as HTMLElement
        val v = layer.querySelector(".v") as HTMLElement
        val canvas = layer.querySelector("canvas") as HTMLCanvasElement
        val dpr = window.devicePixelRatio
        fun size() {
            canvas.width = (window.innerWidth * dpr).toInt()
            canvas.height = (window.innerHeight * dpr).toInt()
        }
        size()
        val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
        var down = false
        var startX = 0.0; var startY = 0.0
        var lastX = 0.0; var lastY = 0.0; var lastT = 0.0
        var pointers = 0
        fun status(e: dynamic, xv: Double, yv: Double) {
            val x = e.clientX as Double; val y = e.clientY as Double
            fun f(n: Double, d: Int = 1) = n.asDynamic().toFixed(d) as String
            bar.textContent = "P: ${if (down) 1 else 0} / $pointers   X: ${f(x)}   Y: ${f(y)}   " +
                "dX: ${f(if (down) x - startX else 0.0)}   dY: ${f(if (down) y - startY else 0.0)}   " +
                "Xv: ${f(xv, 3)}   Yv: ${f(yv, 3)}   Prs: ${f((e.pressure as? Double) ?: 0.0, 2)}   " +
                "Size: ${f(((e.width as? Double) ?: 1.0) / 100.0, 2)}"
            h.style.transform = "translateY(${y}px)"
            v.style.transform = "translateX(${x}px)"
        }
        val onDown: (Event) -> Unit = { ev ->
            val e = ev.asDynamic()
            down = true; pointers = 1
            startX = e.clientX; startY = e.clientY; lastX = startX; lastY = startY; lastT = e.timeStamp
            ctx.clearRect(0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble())
            status(e, 0.0, 0.0)
        }
        val onMove: (Event) -> Unit = { ev ->
            val e = ev.asDynamic()
            val x = e.clientX as Double; val y = e.clientY as Double; val t = e.timeStamp as Double
            val dt = (t - lastT).coerceAtLeast(1.0)
            if (down) {
                ctx.strokeStyle = "rgba(47, 107, 255, 0.9)"
                ctx.lineWidth = 2 * dpr
                ctx.beginPath(); ctx.moveTo(lastX * dpr, lastY * dpr); ctx.lineTo(x * dpr, y * dpr); ctx.stroke()
                ctx.fillStyle = "rgba(255, 61, 127, 0.9)"
                ctx.fillRect(x * dpr - dpr, y * dpr - dpr, 2 * dpr, 2 * dpr)
            }
            status(e, (x - lastX) / dt, (y - lastY) / dt)
            lastX = x; lastY = y; lastT = t
        }
        val onUp: (Event) -> Unit = { ev -> down = false; status(ev.asDynamic(), 0.0, 0.0) }
        val onResize: (Event) -> Unit = { size() }
        window.addEventListener("pointerdown", onDown, true)
        window.addEventListener("pointermove", onMove, true)
        window.addEventListener("pointerup", onUp, true)
        window.addEventListener("resize", onResize)
        bar.textContent = "P: 0 / 0   X: -   Y: -   dX: -   dY: -   Xv: -   Yv: -   Prs: -   Size: -"
        onDispose {
            window.removeEventListener("pointerdown", onDown, true)
            window.removeEventListener("pointermove", onMove, true)
            window.removeEventListener("pointerup", onUp, true)
            window.removeEventListener("resize", onResize)
            layer.remove()
        }
    }
}

/** Profile HWUI rendering, "On screen as bars": one bar per frame, against the 16 ms line. */
@Composable
private fun GpuBars() {
    DisposableEffect(Unit) {
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.className = "dev-gpu"
        document.body?.appendChild(canvas)
        val dpr = window.devicePixelRatio
        val bars = 64
        val barW = 5.0
        val pxPerMs = 3.0
        canvas.width = (bars * barW * dpr).toInt()
        canvas.height = (120 * dpr).toInt()
        val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
        val frames = ArrayDeque<Double>()
        var last = 0.0
        var raf = 0
        var tick: (Double) -> Unit = {}
        tick = { now ->
            if (last > 0.0) {
                frames.addLast(now - last)
                if (frames.size > bars) frames.removeFirst()
            }
            last = now
            ctx.clearRect(0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble())
            frames.forEachIndexed { i, ms ->
                val hgt = (ms * pxPerMs).coerceAtMost(118.0)
                ctx.fillStyle = when {
                    ms <= 16.8 -> "rgba(61, 220, 132, 0.85)"
                    ms <= 33.4 -> "rgba(255, 196, 61, 0.9)"
                    else -> "rgba(255, 82, 82, 0.92)"
                }
                ctx.fillRect(i * barW * dpr, canvas.height - hgt * dpr, (barW - 1) * dpr, hgt * dpr)
            }
            ctx.fillStyle = "rgba(61, 220, 132, 1)"
            ctx.fillRect(0.0, canvas.height - 16.67 * pxPerMs * dpr, canvas.width.toDouble(), 1.5 * dpr)
            raf = window.requestAnimationFrame(tick)
        }
        raf = window.requestAnimationFrame(tick)
        onDispose {
            window.cancelAnimationFrame(raf)
            canvas.remove()
        }
    }
}

/** Strict mode: flash the screen's edges red whenever the main thread is blocked for a long task. */
@Composable
private fun StrictModeFlash() {
    DisposableEffect(Unit) {
        val flash = document.createElement("div") as HTMLElement
        flash.className = "dev-strict"
        document.body?.appendChild(flash)
        var active = true
        val hit: (dynamic) -> Unit = { entry ->
            if (active) {
                Logcat.w("StrictMode", "Long task on the main thread: ${(entry.duration as Double).toInt()}ms")
                flash.classList.remove("on")
                flash.offsetWidth // restart the animation
                flash.classList.add("on")
            }
        }
        // Prefer the browser's long-task reports; elsewhere, notice gaps between animation frames.
        val observer = Logcat.observe("longtask", hit)
        var raf = 0
        if (observer == null) {
            var last = 0.0
            var tick: (Double) -> Unit = {}
            tick = { now ->
                if (last > 0.0 && now - last > 100.0) hit(jsObject("duration" to now - last))
                last = now
                raf = window.requestAnimationFrame(tick)
            }
            raf = window.requestAnimationFrame(tick)
        }
        onDispose {
            active = false
            observer?.disconnect()
            if (raf != 0) window.cancelAnimationFrame(raf)
            flash.remove()
        }
    }
}
