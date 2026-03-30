package dev.ritwik.iama

import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.silk.style.animation.Keyframes
import org.jetbrains.compose.web.css.px

// Scroll-reveal entrance animations (used by ScrollReveal.kt)
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

// Hero entrance animation (used by HeroSection CssStyles)
val HeroFadeIn = Keyframes {
    from { Modifier.opacity(0).translateY(20.px) }
    to { Modifier.opacity(1).translateY(0.px) }
}

// Note: Blob, pulse, cursor, bot-peek, scroll-bounce animations are defined
// as raw CSS @keyframes in AppEntry.kt because they use infinite iteration
// which isn't directly supported by Kobweb's Keyframes.toAnimation() API.
