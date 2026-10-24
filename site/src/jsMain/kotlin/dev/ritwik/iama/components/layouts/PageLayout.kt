package dev.ritwik.iama.components.layouts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.ColumnScope
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.core.PageContext
import com.varabyte.kobweb.core.data.getValue
import com.varabyte.kobweb.core.layout.Layout
import com.varabyte.kobweb.silk.style.CssStyle
import com.varabyte.kobweb.silk.style.breakpoint.Breakpoint
import com.varabyte.kobweb.silk.style.toAttrs
import kotlinx.browser.document
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.fr
import org.jetbrains.compose.web.css.vh
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import dev.ritwik.iama.components.sections.Footer
import dev.ritwik.iama.components.sections.NavHeader
import dev.ritwik.iama.components.phone.AppHost
import dev.ritwik.iama.components.phone.AppWindowHost
import dev.ritwik.iama.components.recruiter.ContactSheet
import dev.ritwik.iama.components.recruiter.Toast
import dev.ritwik.iama.components.widgets.VisitorStats

val PageContentStyle = CssStyle {
    base { Modifier.fillMaxSize().padding(leftRight = 0.cssRem, top = 0.cssRem) }
    Breakpoint.MD { Modifier }
}

class PageLayoutData(val title: String)

@Composable
@Layout
fun PageLayout(ctx: PageContext, content: @Composable ColumnScope.() -> Unit) {
    val data = ctx.data.getValue<PageLayoutData>()
    // Visitor counter: registers this visit once per session, then reads the total (hidden if the API is down).
    LaunchedEffect(Unit) { VisitorStats.load() }

    LaunchedEffect(data.title) {
        document.title = if (data.title == "Home") "Ritwik Raj Srivastava — Tech Lead · Android & AI"
        else "${data.title} — Ritwik Raj Srivastava"
    }

    Column(Modifier.fillMaxWidth().minHeight(100.vh)) {
        A("#content", { classes("skip-link") }) { Text("Skip to content") }
        NavHeader()
        Column(
            Modifier.fillMaxWidth().flexGrow(1),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Div(PageContentStyle.toAttrs { id("content"); attr("tabindex", "-1") }) {
                content()
            }
        }
        Footer(Modifier.fillMaxWidth())
    }
    AppWindowHost(AppHost.FULL)
    ContactSheet()
    Toast()
}
