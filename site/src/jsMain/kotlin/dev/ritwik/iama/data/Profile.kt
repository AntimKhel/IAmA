package dev.ritwik.iama.data

// ---------------------------------------------------------------------------
// Single source of truth for everything the site says about Ritwik.
// Sourced from the 2026 résumé (resources/public/resume.pdf).
// ---------------------------------------------------------------------------

object Profile {
    const val name = "Ritwik Raj Srivastava"
    const val firstName = "Ritwik"
    const val shortName = "Ritwik Raj"
    const val lastName = "Srivastava"
    const val headline = "Tech Lead · Android & AI"
    const val location = "Mumbai, India"
    const val yearsOfExperience = "7+"
    const val availability = "Open to Tech Lead · Senior · FDE roles"

    const val summary =
        "7+ years shipping Android to hundreds of millions of people. At Jio I lead engineering " +
            "across MyJio — agentic AI in the product, an AI-native SDLC in how we build it, a Compose " +
            "design system, and an award-winning React Native OTA platform."

    // Contact — email is shown, phone is assembled at runtime from parts so simple scrapers miss it.
    const val email = "raj.ritwik96@gmail.com"
    val phoneParts = listOf("+91", "70042", "31285")
    const val linkedIn = "https://www.linkedin.com/in/ritwik787/"
    const val github = "https://github.com/AntimKhel"
    const val resumePath = "/resume.pdf"
    const val resumeFileName = "Ritwik-Raj-Srivastava-Resume.pdf"

    /** Completes the sentence "I'm your next …" in the hero. */
    val roles = listOf("Tech Lead", "AI-native Engineer", "Forward Deployed Engineer", "Senior Android Engineer")
}

data class Stat(val value: String, val label: String, val icon: String)

val heroStats = listOf(
    Stat("1B+", "installs on MyJio", "fa-brands fa-google-play"),
    Stat("7+ yrs", "building Android at scale", "fa-brands fa-android"),
    Stat("Skoch Gold", "2026 · for JioAirSync", "fa-solid fa-trophy"),
)

/** Cards floating around the phone (the phone itself already shows the award). */
val floaterStats = listOf(
    heroStats[0],
    heroStats[1],
    Stat("AI-native", "SDLC across MyJio", "fa-solid fa-wand-magic-sparkles"),
)

data class Experience(
    val company: String,
    val role: String,
    val period: String,
    val location: String,
    val highlights: List<String>,
    val techStack: List<String>,
)

val experiences = listOf(
    Experience(
        company = "Reliance Jio",
        role = "Software Development Engineer · Tech Lead",
        period = "Oct 2021 — Present",
        location = "Mumbai",
        highlights = listOf(
            "Spearheaded AI transformation across the MyJio ecosystem with an AI-native SDLC (ADLC) that accelerates development velocity",
            "Architected MyJio Agentic — an AI assistant that automates complex user tasks through agentic workflows",
            "Led cross-functional Android development for MyJio, JioAICloud, JioMart and Jio Financial Services",
            "Built JioAirSync, a custom React Native OTA platform for hot-patching and feature delivery — Skoch Award 2026 Gold",
            "Engineered the Jio Design System in Jetpack Compose for consistency across high-traffic apps",
        ),
        techStack = listOf("Kotlin", "Jetpack Compose", "React Native", "Agentic AI", "LLMs"),
    ),
    Experience(
        company = "HSBC",
        role = "Software Developer",
        period = "Jul 2019 — Oct 2021",
        location = "Pune",
        highlights = listOf(
            "Architected scalable payment journeys for international banking with Kotlin and MVVM",
            "Improved stability by expanding unit, UI and integration test coverage",
        ),
        techStack = listOf("Kotlin", "MVVM", "Testing", "Payments"),
    ),
    Experience(
        company = "Oxigen Services",
        role = "Software Developer Intern",
        period = "May 2018 — Jul 2018",
        location = "Gurugram",
        highlights = listOf("Integrated the Google Maps API into a consumer payments app for location-based services"),
        techStack = listOf("Android", "Google Maps"),
    ),
)

data class SkillCategory(val name: String, val skills: List<String>)

val skillCategories = listOf(
    SkillCategory("AI & GenAI", listOf("LLM integration", "Agentic AI", "AI-native SDLC", "Machine Learning")),
    SkillCategory("Android", listOf("Kotlin", "Jetpack Compose", "MVVM", "Dagger / Hilt", "Retrofit", "Material Design")),
    SkillCategory("Cross-platform", listOf("React Native", "OTA delivery", "Native modules")),
    SkillCategory("Build & Quality", listOf("Gradle", "R8 / ProGuard", "Jenkins", "Azure DevOps", "SonarQube")),
    SkillCategory("Tools", listOf("Git", "Postman", "Figma", "Zeplin", "AWS")),
)

data class Certification(val name: String, val issuer: String, val note: String)

val certifications = listOf(
    Certification("AWS Developer Associate", "Amazon Web Services", "Certified"),
    Certification("Machine Learning Specialization", "Stanford · DeepLearning.AI", "Coursera"),
    Certification("Secure Code Warrior — Yellow Belt", "Secure Code Warrior", "Kotlin"),
)

val awards = listOf(
    "Skoch Award 2026 — Gold, for JioAirSync (custom React Native OTA)",
    "CAT 2019: 92 percentile, offered a place at IIM Nagpur",
    "Seminar speaker: Data Streaming with Apache Spark & Big Data Analytics, NIT Hamirpur",
)

data class Education(val institution: String, val degree: String, val period: String, val score: String)

val educationList = listOf(
    Education("National Institute of Technology, Hamirpur", "B.Tech, Computer Science & Engineering", "2015 — 2019", "CGPI 8.32"),
)

// ---------------------------------------------------------------------------
// Products — public facts only (Play Store listings, résumé). No internal details.
// ---------------------------------------------------------------------------

/** Pill on a product card: an award (gold trophy) or a milestone in the product's own colours. */
data class Badge(val text: String, val icon: String = "fa-solid fa-trophy", val tone: String = "gold")

data class Product(
    val id: String,
    val name: String,
    val tagline: String,
    val role: String,
    val myWork: String,      // one line: what I built on it
    val highlights: List<String>,
    val tech: List<String>,
    val accent: String,
    val accent2: String,
    val icon: String,        // font-awesome classes, used when there is no store icon
    val iconImage: String? = null,
    val url: String? = null,
    val installs: String? = null,
    val rating: String? = null,
    val badge: Badge? = null,
    val screenshots: List<String> = emptyList(),
)

private fun shots(id: String, vararg n: Int) = n.map { "/images/products/$id/$it.webp" }

val products = listOf(
    Product(
        id = "myjio",
        name = "MyJio",
        tagline = "For everything Jio",
        role = "Tech Lead · Android",
        myWork = "Lead Android across the super-app: native Compose shell, React Native journeys, AI features",
        highlights = listOf(
            "Native Compose shell hosting React Native, WebView and mini-app surfaces",
            "AI-native SDLC and agentic features across the app",
            "Jio Design System in Jetpack Compose",
        ),
        tech = listOf("Kotlin", "Compose", "React Native", "AI"),
        accent = "#2f6bff", accent2 = "#7c4dff",
        icon = "fa-solid fa-mobile-screen-button",
        iconImage = "/images/products/myjio/icon.webp",
        url = "https://play.google.com/store/apps/details?id=com.jio.myjio",
        installs = "1B+", rating = "4.5",
        badge = Badge("1 Billion installs", icon = "fa-brands fa-google-play", tone = "navy"),
        screenshots = shots("myjio", 1, 2, 4, 7, 3, 5, 6),
    ),
    Product(
        id = "agentic",
        name = "MyJio Agentic",
        tagline = "An AI assistant that gets things done",
        role = "Architect",
        myWork = "Architected an agentic chatbot that automates complex user tasks inside MyJio",
        highlights = listOf(
            "Agentic workflows that plan and execute multi-step tasks",
            "LLM integration inside a 1B+ install app",
        ),
        tech = listOf("LLMs", "Agentic AI", "Android"),
        accent = "#7c4dff", accent2 = "#ff3d7f",
        icon = "fa-solid fa-wand-magic-sparkles",
    ),
    Product(
        id = "jiofinance",
        name = "JioFinance",
        tagline = "UPI, banking & investments",
        role = "Android engineering",
        myWork = "Integrated Jio Financial Services into MyJio and built UPI services",
        highlights = listOf(
            "JFS journeys embedded inside MyJio",
            "UPI payments: pay, scan, check balance",
        ),
        tech = listOf("Kotlin", "UPI", "React Native"),
        accent = "#c9a26b", accent2 = "#1b2a8a",
        icon = "fa-solid fa-wallet",
        iconImage = "/images/products/jiofinance/icon.webp",
        url = "https://play.google.com/store/apps/details?id=in.jfs.jiofinance",
        installs = "10M+", rating = "4.5",
        screenshots = shots("jiofinance", 7, 1, 4, 8, 3, 5),
    ),
    Product(
        id = "jiomart",
        name = "JioMart",
        tagline = "Groceries to gadgets, delivered",
        role = "Android engineering",
        myWork = "Built the address experience — add, pick and deliver to the right door",
        highlights = listOf(
            "Address capture and selection across checkout",
            "Delivery to 1200+ cities",
        ),
        tech = listOf("Kotlin", "Android", "Maps"),
        accent = "#00a0e3", accent2 = "#e2231a",
        icon = "fa-solid fa-cart-shopping",
        iconImage = "/images/products/jiomart/icon.webp",
        url = "https://play.google.com/store/apps/details?id=com.jpl.jiomart",
        installs = "100M+", rating = "4.1",
        screenshots = shots("jiomart", 2, 7, 1, 3, 8),
    ),
    Product(
        id = "airsync",
        name = "JioAirSync",
        tagline = "Over-the-air updates for React Native",
        role = "Creator",
        myWork = "Built a CodePush-style OTA platform for hot-patching React Native at scale",
        highlights = listOf(
            "Delta patches with full-bundle fallback",
            "Apply on next warm start, automatic rollback",
            "Per-flavor channels and a kill switch",
        ),
        tech = listOf("React Native", "Kotlin", "OTA"),
        accent = "#ff8a3d", accent2 = "#ff3d7f",
        icon = "fa-solid fa-cloud-arrow-down",
        badge = Badge("Skoch Award 2026 · Gold"),
    ),
    Product(
        id = "sdks",
        name = "SDKs",
        tagline = "Embeddable modules & design-system wrappers",
        role = "Author & maintainer",
        myWork = "Reusable SDKs and Compose design-system components shared across apps",
        highlights = listOf(
            "Consumed across multiple Jio apps",
            "Versioned releases with scoped consumer rules",
        ),
        tech = listOf("Kotlin", "Gradle", "Compose"),
        accent = "#3ddc84", accent2 = "#00bfa5",
        icon = "fa-solid fa-cubes",
    ),
)

// ---------------------------------------------------------------------------
// Upcoming work — Agentic MyJio. DRAFT phases, to be confirmed by Ritwik; generic on purpose.
// ---------------------------------------------------------------------------

data class RoadmapPhase(
    val number: Int,
    val name: String,
    val horizon: String,
    val summary: String,
    val items: List<String>,
    val icon: String,
    val accent: String,
)

const val roadmapIntro =
    "MyJio Agentic is the start of an assistant that doesn't just answer — it gets things done. " +
        "Here's where it's heading."

val agenticRoadmap = listOf(
    RoadmapPhase(
        1, "Assist", "Now",
        "A conversational layer inside MyJio that understands plans, usage and bills, and routes you to the right journey.",
        listOf("Natural-language help", "Deep links into native & RN journeys", "Grounded answers on account data"),
        "fa-solid fa-comments", "#3ddc84",
    ),
    RoadmapPhase(
        2, "Act", "Next",
        "Agents that complete tasks end to end — recharge, pay a bill, change a plan, raise a request — with confirmations and guardrails.",
        listOf("Tool-calling over app capabilities", "Human-in-the-loop confirmations", "Safe, auditable actions"),
        "fa-solid fa-bolt", "#00bfa5",
    ),
    RoadmapPhase(
        3, "Anticipate", "Later",
        "Proactive, personalised help: spotting a plan that fits better, warning before data runs out, planning multi-step tasks.",
        listOf("Proactive nudges", "Multi-step planning", "Personal context & memory"),
        "fa-solid fa-brain", "#2f6bff",
    ),
    RoadmapPhase(
        4, "Everywhere", "Exploring",
        "Voice-first and multilingual, working across the Jio ecosystem, with on-device models where privacy and speed matter.",
        listOf("Voice & Indian languages", "Cross-app agents", "On-device inference"),
        "fa-solid fa-globe", "#7c4dff",
    ),
)
