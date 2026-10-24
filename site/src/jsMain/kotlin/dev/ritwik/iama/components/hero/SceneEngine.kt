package dev.ritwik.iama.components.hero

import dev.ritwik.iama.util.clamp01
import dev.ritwik.iama.util.css
import dev.ritwik.iama.util.prefersReducedMotion
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.asList
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Drives the hero scene without recomposing: one passive scroll/pointer listener pair and a single
 * requestAnimationFrame loop that writes CSS custom properties onto the scene element (see rr.css).
 */
class SceneEngine(private val scene: HTMLElement) {
    private val name = scene.querySelector(".rr-name") as? HTMLElement
    private val phone = scene.querySelector(".rr-phone") as? HTMLElement
    private val particles = (scene.querySelector(".rr-particles") as? HTMLCanvasElement)?.let { Particles(it) }
    private val reduced = prefersReducedMotion

    private var raf = 0
    private var disposed = false
    private val written = HashMap<String, String>()

    // pointer: target (t*) and smoothed (m*) in -1..1, raw px for particles / name gradient
    private var tx = 0.0
    private var ty = 0.0
    private var mx = 0.0
    private var my = 0.0
    private var px = -9999.0
    private var py = -9999.0
    private var lastPointerAt = -1e9

    private var phoneScale = 1.0
    private var fit = 1.0
    private var screenW = 278.0
    private var screenH = 608.0
    private var vw = 1.0
    private var vh = 1.0

    private val passive: dynamic = js("({passive: true})")

    private val onPointer: (Event) -> Unit = { e ->
        val m = e as MouseEvent
        tx = m.clientX / window.innerWidth.toDouble() * 2 - 1
        ty = m.clientY / window.innerHeight.toDouble() * 2 - 1
        px = m.clientX.toDouble()
        py = m.clientY.toDouble()
        lastPointerAt = window.performance.now()
        name?.let { n ->
            val r = n.getBoundingClientRect()
            n.style.setProperty("--cx", "${(px - r.left).css()}px")
            n.style.setProperty("--cy", "${(py - r.top).css()}px")
        }
    }

    private val onLeave: (Event) -> Unit = {
        px = -9999.0
        py = -9999.0
        tx = 0.0
        ty = 0.0
    }

    private val onResize: (Event) -> Unit = { measure() }

    init {
        window.addEventListener("pointermove", onPointer, passive)
        document.addEventListener("pointerleave", onLeave, passive)
        window.addEventListener("resize", onResize, passive)
        measure()
        // Glyph offsets depend on the web font, so re-measure once it has loaded.
        document.asDynamic().fonts?.ready?.then { if (!disposed) measureGlyphs() }
        raf = window.requestAnimationFrame(::frame)
    }

    fun dispose() {
        disposed = true
        window.cancelAnimationFrame(raf)
        window.removeEventListener("pointermove", onPointer, passive)
        document.removeEventListener("pointerleave", onLeave, passive)
        window.removeEventListener("resize", onResize, passive)
    }

    private fun measure() {
        val w = window.innerWidth.toDouble()
        val h = window.innerHeight.toDouble()
        vw = w
        vh = h
        val pw = (phone?.offsetWidth ?: 300).toDouble()
        val ph = (phone?.offsetHeight ?: 630).toDouble()
        // Rest scale (beside the copy) and centered "fit" scale: the phone is never larger than the viewport.
        phoneScale = min(1.0, (h - 130) / ph).coerceAtLeast(0.55)
        // Leave room for the nav above and the focus bar below.
        fit = min((h - 140) / ph, (w * 0.9) / pw).coerceIn(0.6, 1.3)
        screenW = (pw - 22) * fit
        screenH = (ph - 22) * fit
        val dx = if (w >= 1280) w * 0.22 else w * 0.245
        write("--ps", phoneScale.css())
        write("--fit", fit.css())
        write("--dx", "${dx.css()}px")
        written.remove("--clip")
        particles?.resize()
        measureGlyphs()
    }

    private fun measureGlyphs() {
        val n = name ?: return
        n.querySelectorAll(".rr-ch").asList().forEach { node ->
            val el = node as HTMLElement
            el.style.setProperty("--ox", "${el.offsetLeft}px")
            el.style.setProperty("--oy", "${el.offsetTop}px")
        }
    }

    private fun frame(now: Double) {
        if (disposed) return
        val rect = scene.getBoundingClientRect()
        val vh = window.innerHeight.toDouble()
        val visible = rect.bottom > 0 && rect.top < vh

        if (visible) {
            val travel = rect.height - vh
            val p = if (travel > 1) clamp01(-rect.top / travel) else 0.0

            // Act 1 (0–0.3): copy exits, phone glides to center and grows to fit the viewport.
            // Hold (0.3–0.5): the phone sits centered, fully visible and usable.
            // Act 2 (0.5–0.8): its screen expands into the full-screen launcher.
            val copy = clamp01(p / 0.22)
            val move = easeInOut(clamp01(p / 0.3))
            val e = easeInOut(clamp01((p - 0.5) / 0.3))

            write("--p", p.css())
            write("--move", move.css())
            write("--copy", copy.css())
            write("--e", e.css())
            val k = 1 - e
            write(
                "--clip",
                "inset(${((vh - screenH) / 2 * k).css()}px ${((vw - screenW) / 2 * k).css()}px round ${(42 * fit * k).css()}px)",
            )
            toggle("is-moving", copy > 0.5)
            toggle("is-expanding", e > 0.02)
            toggle("is-entered", e > 0.9)

            // Pointer smoothing; idle pointer lets the name gradient drift on its own.
            mx += (tx - mx) * 0.07
            my += (ty - my) * 0.07
            write("--mx", mx.css())
            write("--my", my.css())

            if (now - lastPointerAt > 2500 && !reduced) {
                name?.let { n ->
                    val r = n.getBoundingClientRect()
                    val t = now / 1000
                    n.style.setProperty("--cx", "${(r.width * (0.5 + 0.45 * sin(t * 0.6))).css()}px")
                    n.style.setProperty("--cy", "${(r.height * (0.5 + 0.4 * sin(t * 0.9 + 1))).css()}px")
                }
            }

            if (!reduced && document.asDynamic().hidden != true && e < 0.98) {
                particles?.step(px, py)
            }
        }

        raf = window.requestAnimationFrame(::frame)
    }

    private fun write(prop: String, value: String) {
        if (written[prop] != value) {
            written[prop] = value
            scene.style.setProperty(prop, value)
        }
    }

    private fun toggle(cls: String, on: Boolean) {
        if (scene.classList.contains(cls) != on) scene.classList.toggle(cls, on)
    }

    private fun easeInOut(t: Double) = if (t < 0.5) 4 * t * t * t else 1 - (-2 * t + 2).pow(3) / 2
}

/** A cursor-reactive constellation drawn on a canvas. Cheap: ~100 points, O(n²) links. */
private class Particles(private val canvas: HTMLCanvasElement) {
    private val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
    private val palette = arrayOf("61,220,132", "0,191,165", "124,77,255", "47,107,255")
    private var w = 0.0
    private var h = 0.0
    private var n = 0
    private var xs = DoubleArray(0)
    private var ys = DoubleArray(0)
    private var vxs = DoubleArray(0)
    private var vys = DoubleArray(0)
    private var rs = DoubleArray(0)
    private var cs = IntArray(0)

    fun resize() {
        val dpr = min(window.devicePixelRatio, 2.0)
        w = canvas.clientWidth.toDouble()
        h = canvas.clientHeight.toDouble()
        if (w <= 0 || h <= 0) return
        canvas.width = (w * dpr).toInt()
        canvas.height = (h * dpr).toInt()
        ctx.setTransform(dpr, 0.0, 0.0, dpr, 0.0, 0.0)
        n = (w * h / 15000).toInt().coerceIn(28, 110)
        xs = DoubleArray(n) { Random.nextDouble() * w }
        ys = DoubleArray(n) { Random.nextDouble() * h }
        vxs = DoubleArray(n) { (Random.nextDouble() - 0.5) * 0.35 }
        vys = DoubleArray(n) { (Random.nextDouble() - 0.5) * 0.35 }
        rs = DoubleArray(n) { 0.8 + Random.nextDouble() * 1.8 }
        cs = IntArray(n) { Random.nextInt(palette.size) }
    }

    fun step(mx: Double, my: Double) {
        if (n == 0) resize()
        ctx.clearRect(0.0, 0.0, w, h)
        val link = 125.0
        val reach = 170.0

        for (i in 0 until n) {
            var x = xs[i] + vxs[i]
            var y = ys[i] + vys[i]
            val dx = x - mx
            val dy = y - my
            val d2 = dx * dx + dy * dy
            if (d2 < reach * reach && d2 > 1) {
                val d = sqrt(d2)
                val f = (1 - d / reach) * 1.6
                x += dx / d * f
                y += dy / d * f
            }
            if (x < -10) x = w + 10 else if (x > w + 10) x = -10.0
            if (y < -10) y = h + 10 else if (y > h + 10) y = -10.0
            xs[i] = x
            ys[i] = y
        }

        ctx.lineWidth = 0.7
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val dx = xs[i] - xs[j]
                val dy = ys[i] - ys[j]
                val d2 = dx * dx + dy * dy
                if (d2 < link * link) {
                    val a = (1 - sqrt(d2) / link) * 0.32
                    ctx.strokeStyle = "rgba(${palette[cs[i]]},$a)"
                    ctx.beginPath()
                    ctx.moveTo(xs[i], ys[i])
                    ctx.lineTo(xs[j], ys[j])
                    ctx.stroke()
                }
            }
            // threads to the cursor
            val dx = xs[i] - mx
            val dy = ys[i] - my
            val d2 = dx * dx + dy * dy
            if (d2 < reach * reach) {
                val a = (1 - sqrt(d2) / reach) * 0.55
                ctx.strokeStyle = "rgba(${palette[cs[i]]},$a)"
                ctx.beginPath()
                ctx.moveTo(xs[i], ys[i])
                ctx.lineTo(mx, my)
                ctx.stroke()
            }
        }

        for (i in 0 until n) {
            ctx.fillStyle = "rgba(${palette[cs[i]]},0.85)"
            ctx.beginPath()
            ctx.arc(xs[i], ys[i], rs[i], 0.0, 2 * PI)
            ctx.fill()
        }
    }
}
