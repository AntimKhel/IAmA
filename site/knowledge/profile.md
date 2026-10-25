<!--
  What "Ask Ritwik" knows about Ritwik besides the blog posts. The site build bundles this file and every
  post in src/jsMain/resources/markdown/blog/ into public/knowledge.json, which the Worker (backend/) reads.
  Mirrors src/jsMain/kotlin/dev/ritwik/iama/data/Profile.kt: keep the two in sync. Public facts only.
  Each "## " section is one retrievable chunk (the first is always sent); "@actions" lists the buttons
  shown under an answer that used it: app:<screen>, product:<id>, post:<slug>, resume.
-->

## Ritwik at a glance
@actions: app:about, resume
Ritwik Raj Srivastava is a Tech Lead for Android & AI, based in Mumbai, India, with 7+ years of experience.
He has spent those years shipping Android to hundreds of millions of people. At Jio (Reliance Jio) he leads engineering across MyJio: agentic AI in the product, an AI-native SDLC in how the team builds, a Jetpack Compose design system, and an award-winning React Native OTA platform (JioAirSync, Skoch Award 2026 Gold).
He is open to Tech Lead, Senior Engineer and Forward Deployed Engineer (FDE) roles.
Contact: email raj.ritwik96@gmail.com, LinkedIn linkedin.com/in/ritwik787, GitHub github.com/AntimKhel. His phone number is in the Contact app on this site (tap to reveal). His résumé PDF can be downloaded from the site.

## Hiring questions: availability, notice period, salary, relocation, interviews
@actions: app:contact, resume
Ritwik is open to Tech Lead, Senior Engineer and Forward Deployed Engineer (FDE) roles. He is based in Mumbai, India.
This assistant does not know his notice period, salary expectations, relocation or remote preferences, or interview availability. For those, email Ritwik directly at raj.ritwik96@gmail.com or message him on LinkedIn.

## Experience: Reliance Jio, Tech Lead (Oct 2021 to present)
@actions: app:work, product:myjio
Role: Software Development Engineer and Tech Lead at Reliance Jio, Mumbai, from October 2021 to the present.
- Spearheaded the AI transformation across the MyJio ecosystem with an AI-native SDLC (ADLC) that accelerates development velocity.
- Architected MyJio Agentic, an AI assistant that automates complex user tasks through agentic workflows.
- Led cross-functional Android development for MyJio, JioAICloud, JioMart and Jio Financial Services.
- Built JioAirSync, a custom React Native OTA platform for hot-patching and feature delivery, which won the Skoch Award 2026 Gold.
- Engineered the Jio Design System in Jetpack Compose for consistency across high-traffic apps.
Tech: Kotlin, Jetpack Compose, React Native, Agentic AI, LLMs.

## Experience: HSBC, Software Developer (Jul 2019 to Oct 2021)
@actions: app:work
Role: Software Developer at HSBC, Pune, from July 2019 to October 2021.
- Architected scalable payment journeys for international banking with Kotlin and MVVM.
- Improved stability by expanding unit, UI and integration test coverage.
Tech: Kotlin, MVVM, testing, payments.

## Experience: Oxigen Services, internship (May to Jul 2018)
@actions: app:work
Software Developer Intern at Oxigen Services, Gurugram, from May to July 2018. He integrated the Google Maps API into a consumer payments app for location-based services.

## Education
@actions: app:about
B.Tech in Computer Science & Engineering from the National Institute of Technology (NIT) Hamirpur, 2015 to 2019, CGPI 8.32.

## Skills
@actions: app:skills
- AI & GenAI: LLM integration, agentic AI, AI-native SDLC, machine learning.
- Android: Kotlin, Jetpack Compose, MVVM, Dagger / Hilt, Retrofit, Material Design.
- Cross-platform: React Native, OTA delivery, native modules.
- Build & quality: Gradle, R8 / ProGuard, Jenkins, Azure DevOps, SonarQube.
- Tools: Git, Postman, Figma, Zeplin, AWS.

## Certifications
@actions: app:awards
- AWS Developer Associate (Amazon Web Services), certified.
- Machine Learning Specialization (Stanford and DeepLearning.AI, on Coursera).
- Secure Code Warrior Yellow Belt in Kotlin.

## Awards and achievements
@actions: app:awards, product:airsync
- Skoch Award 2026, Gold, for JioAirSync, his custom React Native over-the-air (OTA) update platform.
- CAT 2019: 92 percentile, offered a place at IIM Nagpur.
- Seminar speaker on Data Streaming with Apache Spark & Big Data Analytics at NIT Hamirpur.

## Product: MyJio
@actions: product:myjio
MyJio ("For everything Jio") is Jio's super-app, with 1 billion+ installs on Google Play and a 4.5 rating. Ritwik's role: Tech Lead, Android.
He leads Android across the super-app: a native Jetpack Compose shell, React Native journeys and AI features.
- Native Compose shell hosting React Native, WebView and mini-app surfaces.
- AI-native SDLC and agentic features across the app.
- Jio Design System in Jetpack Compose.
Tech: Kotlin, Compose, React Native, AI.

## Product: MyJio Agentic
@actions: product:agentic
MyJio Agentic is an AI assistant that gets things done. Ritwik is its architect: he architected an agentic chatbot that automates complex user tasks inside MyJio.
- Agentic workflows that plan and execute multi-step tasks.
- LLM integration inside a 1B+ install app.
Tech: LLMs, agentic AI, Android.

## Product: JioFinance
@actions: product:jiofinance
JioFinance (UPI, banking & investments) has 10M+ installs on Google Play and a 4.5 rating. Ritwik did Android engineering: he integrated Jio Financial Services journeys into MyJio and built UPI services (pay, scan, check balance).
Tech: Kotlin, UPI, React Native.

## Product: JioMart
@actions: product:jiomart
JioMart (groceries to gadgets, delivered) has 100M+ installs on Google Play and a 4.1 rating, delivering to 1200+ cities. Ritwik did Android engineering: he built the address experience (adding, picking and delivering to the right address) across checkout.
Tech: Kotlin, Android, Maps.

## Product: JioAirSync (React Native OTA updates)
@actions: product:airsync, post:ota-updates-react-native
JioAirSync is a CodePush-style over-the-air (OTA) update platform for React Native that Ritwik created. It won the Skoch Award 2026 Gold.
- Delta patches with full-bundle fallback.
- Updates apply on the next warm start, with automatic rollback.
- Per-flavor channels and a kill switch.
Tech: React Native, Kotlin, OTA.

## Product: SDKs and design-system components
@actions: product:sdks
Ritwik authors and maintains reusable SDKs and Jetpack Compose design-system components shared across Jio apps, consumed by multiple apps, with versioned releases and scoped consumer rules.
Tech: Kotlin, Gradle, Compose.

## About this website
@actions: post:hello-world
Ritwik built this portfolio himself in Kotlin with Kobweb (Compose for Web), styled like an Android phone. It is hosted on GitHub Pages. The visitor counter, the guestbook and this assistant run on a Cloudflare Worker with a D1 database; this assistant uses an open model on Cloudflare Workers AI and only knows what is published on this site.
