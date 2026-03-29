package dev.ritwik.iama.components.widgets

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.silk.style.animation.Keyframes
import com.varabyte.kobweb.silk.style.animation.toAnimation
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div

@Composable
fun ScrollReveal(
    keyframes: Keyframes = dev.ritwik.iama.FadeInUp,
    duration: CSSSizeValue<CSSUnit.ms> = 600.ms,
    delay: CSSSizeValue<CSSUnit.ms> = 0.ms,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var isVisible by remember { mutableStateOf(false) }

    val animModifier = if (isVisible) {
        Modifier.animation(
            keyframes.toAnimation(
                duration = duration,
                timingFunction = AnimationTimingFunction.EaseOut,
                delay = delay,
                fillMode = AnimationFillMode.Both,
            )
        )
    } else {
        Modifier.opacity(0)
    }

    Div(
        modifier.then(animModifier).toAttrs {
            ref { element ->
                val options = js("({threshold: 0.15})")
                val callback: (dynamic, dynamic) -> Unit = { entries, _ ->
                    val arr = entries as Array<dynamic>
                    for (entry in arr) {
                        if (entry.isIntersecting as Boolean) {
                            isVisible = true
                        }
                    }
                }
                @Suppress("UNUSED_VARIABLE")
                val cb = callback
                val observer = js("new IntersectionObserver(cb, options)")
                observer.observe(element)
                onDispose {
                    observer.disconnect()
                }
            }
        }
    ) {
        content()
    }
}
