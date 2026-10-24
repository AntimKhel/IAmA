package dev.ritwik.iama.data

// ---------------------------------------------------------------------------
// Single source of truth for everything the site says about Ritwik.
// Values marked TODO are placeholders until real details are provided.
// ---------------------------------------------------------------------------

object Profile {
    const val name = "Ritwik Raj Srivastava"
    const val firstName = "Ritwik"
    const val shortName = "Ritwik Raj"
    const val lastName = "Srivastava"
    const val headline = "Tech Lead · Android"
    const val location = "Mumbai, India" // TODO: confirm
    const val yearsOfExperience = "6+"
    const val availability = "Open to Tech Lead / Staff Android roles"

    const val summary =
        "I lead Android engineering for MyJio, one of India's most-installed apps. " +
            "I build at the seam between Jetpack Compose and React Native — design-system parity, " +
            "OTA delivery, deeplink routing, build performance and the kind of R8 archaeology " +
            "that shaves tens of megabytes off a release."

    // Contact — email is shown, phone is assembled at runtime from parts so simple scrapers miss it.
    const val email = "TODO@example.com" // TODO: personal email
    val phoneParts = listOf("+91", "00000", "00000") // TODO: real number, split in parts
    const val linkedIn = "https://www.linkedin.com/in/ritwik787/"
    const val github = "https://github.com/AntimKhel" // TODO: confirm handle
    const val resumePath = "/resume.pdf" // TODO: drop the PDF into resources/public/resume.pdf
    const val resumeFileName = "Ritwik-Raj-Srivastava-Resume.pdf"

    val roles = listOf("Tech Lead · Android", "Jetpack Compose", "React Native", "Build & R8 performance", "Design systems")
}

data class Stat(val value: String, val label: String, val icon: String)

val heroStats = listOf(
    Stat("500M+", "installs on apps I ship", "fa-google-play"),
    Stat("6+ yrs", "building Android at scale", "fa-android"),
    Stat("82%", "obfuscation after R8 cleanup", "fa-shield-halved"),
)

data class Experience(
    val company: String,
    val role: String,
    val period: String,
    val highlights: List<String>,
    val techStack: List<String>,
)

val experiences = listOf(
    Experience(
        company = "Reliance Jio",
        role = "Software Development Engineer → Tech Lead",
        period = "Oct 2021 — Present",
        highlights = listOf(
            "Tech lead for the MyJio Android app — a native Compose shell hosting React Native, WebView and mini-app surfaces",
            "Drove design-system parity across Jetpack Compose and React Native with a shared theme-token contract",
            "Built AirSync, an in-house over-the-air update platform for React Native bundles",
            "Untangled R8 keep rules: obfuscation from 24% to 82%, ~40 MB less dex in a controlled build",
            "Shipped across JioFinance, JioMart and multiple embeddable SDKs",
        ),
        techStack = listOf("Kotlin", "Jetpack Compose", "React Native", "Gradle", "R8"),
    ),
    Experience(
        company = "HSBC",
        role = "Software Developer",
        period = "Jul 2019 — Oct 2021",
        highlights = listOf(
            "Built scalable Android features for payments in Kotlin",
            "Raised test coverage across UI, unit, integration and snapshot tests",
        ),
        techStack = listOf("Kotlin", "Android", "Testing", "CI/CD"),
    ),
    Experience(
        company = "Oxigen Services",
        role = "Software Developer Intern",
        period = "May 2018 — Jul 2018",
        highlights = listOf(
            "Integrated Google Maps into a consumer payments app",
            "Bug fixing and quality assurance",
        ),
        techStack = listOf("Android", "Google Maps SDK"),
    ),
)

data class SkillCategory(val name: String, val skills: List<String>)

val skillCategories = listOf(
    SkillCategory("Languages", listOf("Kotlin", "Java", "JavaScript", "TypeScript")),
    SkillCategory("Android", listOf("Jetpack Compose", "Coroutines & Flow", "Hilt", "Room", "WorkManager")),
    SkillCategory("Cross-platform", listOf("React Native (New Arch)", "TurboModules", "Hermes", "Re.Pack")),
    SkillCategory("Build & Perf", listOf("Gradle", "R8 / ProGuard", "Baseline Profiles", "Perfetto")),
    SkillCategory("Architecture", listOf("MVVM", "Clean Architecture", "Modularization", "Design Systems")),
    SkillCategory("Tooling", listOf("Git", "CI/CD", "SonarQube", "Figma", "AWS")),
)

data class Certification(val name: String, val issuer: String, val note: String)

val certifications = listOf(
    Certification("AWS Developer Associate", "Amazon Web Services", "Certified"),
    Certification("Secure Code Warrior — Yellow Belt", "Secure Code Warrior", "Kotlin"),
    Certification("Machine Learning", "Coursera (Stanford)", "Certified"),
)

val awards = listOf(
    "CAT 2019: 92 percentile, IIM Nagpur offer",
    "Apache Spark seminar speaker at NIT Hamirpur",
    "CBSE Physics distinction",
)

data class Education(val institution: String, val degree: String, val period: String, val score: String)

val educationList = listOf(
    Education("National Institute of Technology, Hamirpur", "B.Tech, Computer Science & Engineering", "2015 — 2019", "CGPI 8.32"),
)

// ---------------------------------------------------------------------------
// Products — public facts only (Play Store listings). No internal details.
// ---------------------------------------------------------------------------

data class Product(
    val id: String,
    val name: String,
    val tagline: String,
    val role: String,
    val highlights: List<String>,
    val tech: List<String>,
    val accent: String,      // CSS color
    val accent2: String,     // CSS color
    val icon: String,        // font-awesome class used when no image
    val url: String? = null, // public link
    val screenshots: List<String> = emptyList(),
)

val products = listOf(
    Product(
        id = "myjio",
        name = "MyJio",
        tagline = "India's telecom super-app",
        role = "Tech Lead · Android",
        highlights = listOf(
            "Native Compose shell hosting React Native, WebView and mini-app surfaces",
            "One deeplink router for every way into the app",
            "R8 cleanup: 24% → 82% obfuscation",
        ),
        tech = listOf("Compose", "React Native", "Kotlin", "R8"),
        accent = "#2F6BFF", accent2 = "#7C4DFF",
        icon = "fa-mobile-screen-button",
        url = "https://play.google.com/store/apps/details?id=com.jio.myjio",
        screenshots = listOf("/images/myjio-1.webp", "/images/myjio-2.webp", "/images/myjio-3.webp", "/images/myjio-4.webp", "/images/myjio-5.webp"),
    ),
    Product(
        id = "jiofinance",
        name = "JioFinance",
        tagline = "Payments, banking & investments",
        role = "Android engineering",
        highlights = listOf(
            "Finance journeys inside a hybrid native + RN app",
            "Secure SDK integration and release hardening",
        ),
        tech = listOf("Kotlin", "React Native", "Security"),
        accent = "#00BFA5", accent2 = "#2F6BFF",
        icon = "fa-wallet",
        url = "https://play.google.com/store/apps/details?id=in.jfs.jiofinance",
    ),
    Product(
        id = "jiomart",
        name = "JioMart",
        tagline = "Groceries to electronics, delivered",
        role = "Android engineering",
        highlights = listOf(
            "Commerce flows at national scale",
            "Shared components and design-system adoption",
        ),
        tech = listOf("Kotlin", "Compose", "Commerce"),
        accent = "#00A0E3", accent2 = "#3DDC84",
        icon = "fa-cart-shopping",
        url = "https://play.google.com/store/apps/details?id=com.jpl.jiomart",
    ),
    Product(
        id = "airsync",
        name = "AirSync",
        tagline = "Over-the-air updates for React Native",
        role = "Creator",
        highlights = listOf(
            "Delta patches with full-bundle fallback",
            "Apply on next warm start, automatic rollback",
            "Per-flavor channels and a kill switch",
        ),
        tech = listOf("React Native", "Kotlin", "OTA"),
        accent = "#FF8A3D", accent2 = "#FF3D7F",
        icon = "fa-cloud-arrow-down",
    ),
    Product(
        id = "sdks",
        name = "SDKs",
        tagline = "Embeddable modules & design-system wrappers",
        role = "Author & maintainer",
        highlights = listOf(
            "Reusable SDKs consumed across multiple apps",
            "Scoped consumer ProGuard rules, versioned releases",
        ),
        tech = listOf("Kotlin", "Gradle", "Compose"),
        accent = "#3DDC84", accent2 = "#00BFA5",
        icon = "fa-cubes",
    ),
)
