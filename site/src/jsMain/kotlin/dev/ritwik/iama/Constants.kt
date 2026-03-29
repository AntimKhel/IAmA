package dev.ritwik.iama

data class Experience(
    val company: String,
    val role: String,
    val period: String,
    val highlights: List<String>,
    val techStack: List<String>,
)

data class SkillCategory(
    val name: String,
    val skills: List<String>,
)

data class Certification(
    val name: String,
    val issuer: String,
    val year: String,
)

data class Education(
    val institution: String,
    val degree: String,
    val period: String,
    val score: String,
)

val experiences = listOf(
    Experience(
        company = "Reliance Jio",
        role = "Software Developer Engineer / Tech Lead",
        period = "October 2021 - Present",
        highlights = listOf(
            "Tech lead for MyJio application serving 100M+ users",
            "Designed and built the Jio Design System using Jetpack Compose",
            "Created OTA updates system for React Native (Codepush-style)",
            "Contributed to JioAICloud, JioMart, and JFS projects",
            "Developed Tirupati Balaji Android app for Government of Andhra Pradesh",
        ),
        techStack = listOf("Kotlin", "Jetpack Compose", "React Native", "MVVM"),
    ),
    Experience(
        company = "HSBC Holdings Plc",
        role = "Software Developer",
        period = "July 2019 - October 2021",
        highlights = listOf(
            "Built scalable Android applications for payment systems in Kotlin",
            "Enhanced testing coverage across UI, unit, integration, and snapshot testing",
        ),
        techStack = listOf("Kotlin", "Android", "Testing", "CI/CD"),
    ),
    Experience(
        company = "Oxigen Services Pvt. Ltd",
        role = "Software Developer Intern",
        period = "May 2018 - July 2018",
        highlights = listOf(
            "Worked on Google Maps integration for Future Pay app",
            "Bug fixes and quality assurance",
        ),
        techStack = listOf("Android", "Google Maps SDK"),
    ),
)

val skillCategories = listOf(
    SkillCategory("Languages", listOf("Kotlin", "Java", "JavaScript")),
    SkillCategory("Frameworks", listOf("Jetpack Compose", "React Native", "Retrofit")),
    SkillCategory("Architecture", listOf("MVVM", "Clean Architecture", "Dagger/Hilt")),
    SkillCategory("Tools", listOf("Git", "Jenkins CI", "SonarQube", "Postman", "AppDynamics")),
    SkillCategory("Design", listOf("Figma", "Adobe XD", "Material Design", "Zeplin")),
    SkillCategory("Cloud", listOf("AWS", "Azure DevOps")),
)

val certifications = listOf(
    Certification("AWS Developer Associate", "Amazon Web Services", "Certified"),
    Certification("Secure Code Warrior Yellow Belt", "Secure Code Warrior", "Kotlin"),
    Certification("Machine Learning", "Coursera (Stanford)", "Certified"),
)

val awards = listOf(
    "CAT 2019: 92 percentile, IIM Nagpur offer",
    "Apache Spark seminar speaker at NIT Hamirpur",
    "CBSE Physics distinction",
)

val educationList = listOf(
    Education(
        institution = "National Institute of Technology, Hamirpur",
        degree = "B.Tech, Computer Science & Engineering",
        period = "2015 - 2019",
        score = "CGPI: 8.32",
    ),
)
