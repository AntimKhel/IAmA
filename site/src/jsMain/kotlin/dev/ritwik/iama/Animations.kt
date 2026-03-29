package dev.ritwik.iama

import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.silk.style.animation.Keyframes
import org.jetbrains.compose.web.css.deg
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px

val FadeInUp = Keyframes {
    from { Modifier.opacity(0).translateY(30.px) }
    to { Modifier.opacity(1).translateY(0.px) }
}

val FadeInLeft = Keyframes {
    from { Modifier.opacity(0).translateX((-40).px) }
    to { Modifier.opacity(1).translateX(0.px) }
}

val FadeInRight = Keyframes {
    from { Modifier.opacity(0).translateX(40.px) }
    to { Modifier.opacity(1).translateX(0.px) }
}

val PopIn = Keyframes {
    from { Modifier.opacity(0).scale(0.85) }
    to { Modifier.opacity(1).scale(1.0) }
}

val HeroFadeIn = Keyframes {
    from { Modifier.opacity(0).translateY(20.px) }
    to { Modifier.opacity(1).translateY(0.px) }
}

// Floating blob animations - Josh Comeau inspired
val BlobFloat1 = Keyframes {
    from { Modifier.translate(0.percent, 0.percent) }
    25.percent { Modifier.translate(5.percent, (-10).percent) }
    50.percent { Modifier.translate((-5).percent, 5.percent) }
    75.percent { Modifier.translate(10.percent, (-5).percent) }
    to { Modifier.translate(0.percent, 0.percent) }
}

val BlobFloat2 = Keyframes {
    from { Modifier.translate(0.percent, 0.percent) }
    25.percent { Modifier.translate((-8).percent, 6.percent) }
    50.percent { Modifier.translate(4.percent, (-8).percent) }
    75.percent { Modifier.translate((-6).percent, 3.percent) }
    to { Modifier.translate(0.percent, 0.percent) }
}

val BlobFloat3 = Keyframes {
    from { Modifier.translate(0.percent, 0.percent).rotate(0.deg) }
    33.percent { Modifier.translate(7.percent, (-5).percent).rotate(5.deg) }
    66.percent { Modifier.translate((-3).percent, 8.percent).rotate((-3).deg) }
    to { Modifier.translate(0.percent, 0.percent).rotate(0.deg) }
}

val Pulse = Keyframes {
    from { Modifier.scale(1.0).opacity(0.6) }
    50.percent { Modifier.scale(1.05).opacity(0.8) }
    to { Modifier.scale(1.0).opacity(0.6) }
}

val GlowPulse = Keyframes {
    from { Modifier.opacity(0.4) }
    50.percent { Modifier.opacity(0.7) }
    to { Modifier.opacity(0.4) }
}
