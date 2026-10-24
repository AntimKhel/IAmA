package dev.ritwik.iama.util

import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event

/**
 * One shared scroll/rAF loop for section effects. Registered elements receive:
 *  --sp   0..1 as the element travels from entering the bottom of the viewport to leaving the top
 *  --pin  0..1 across the element's own height minus one viewport (for sticky / pinned scenes)
 */
object ScrollFx {
    private val elements = mutableListOf<HTMLElement>()
    private var raf = 0
    private var listening = false
    private val passive: dynamic = js("({passive: true})")

    private val onScroll: (Event) -> Unit = { schedule() }

    fun register(el: HTMLElement) {
        elements += el
        if (!listening) {
            listening = true
            window.addEventListener("scroll", onScroll, passive)
            window.addEventListener("resize", onScroll, passive)
        }
        schedule()
    }

    fun unregister(el: HTMLElement) {
        elements -= el
        if (elements.isEmpty() && listening) {
            listening = false
            window.removeEventListener("scroll", onScroll, passive)
            window.removeEventListener("resize", onScroll, passive)
        }
    }

    private fun schedule() {
        if (raf != 0) return
        raf = window.requestAnimationFrame {
            raf = 0
            val vh = window.innerHeight.toDouble()
            elements.forEach { el ->
                val r = el.getBoundingClientRect()
                if (r.bottom < -vh || r.top > vh * 2) return@forEach
                val sp = clamp01((vh - r.top) / (vh + r.height))
                val travel = r.height - vh
                val pin = if (travel > 1) clamp01(-r.top / travel) else sp
                el.style.setProperty("--sp", sp.css())
                el.style.setProperty("--pin", pin.css())
            }
        }
    }
}

/**
 * Adds `is-in` to elements once they scroll into view (see `.rv` in sections.css).
 * A single IntersectionObserver serves the whole page.
 */
object Reveal {
    private var observer: dynamic = null

    private fun observer(): dynamic {
        if (observer == null) {
            val callback: (dynamic, dynamic) -> Unit = { entries, obs ->
                val arr = entries.unsafeCast<Array<dynamic>>()
                for (entry in arr) {
                    if (entry.isIntersecting == true) {
                        (entry.target as HTMLElement).classList.add("is-in")
                        obs.unobserve(entry.target)
                    }
                }
            }
            val options: dynamic = js("({rootMargin: '0px 0px -12% 0px', threshold: 0.08})")
            @Suppress("UNUSED_VARIABLE") val cb = callback
            observer = js("new IntersectionObserver(cb, options)")
        }
        return observer
    }

    fun observe(el: HTMLElement) {
        if (prefersReducedMotion) {
            el.classList.add("is-in")
            return
        }
        observer().observe(el)
    }

    fun unobserve(el: HTMLElement) {
        observer?.unobserve(el)
    }
}
