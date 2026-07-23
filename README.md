# Food Manager

**[Русский](README.ru.md) | English**

A food discovery app — build a taste profile from the dishes you like, then get personalized food recommendations.

## Features

- **Discover deck** — browse dishes one at a time and mark what you like
- **Personalized recommendations** — a "For You" feed built from your taste profile
- **Favorites** — save and revisit dishes you liked
- **Onboarding** — capture initial taste preferences for new users
- **Device-key authentication** — frictionless login without passwords
- **Profile management** — view and edit your taste preferences

## Tech stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose, Material 3 Expressive (alpha) |
| DI | Hilt |
| Async | Kotlin Coroutines & Flow |
| Local persistence | Room 3, Preferences DataStore |
| Networking | Retrofit, OkHttp, kotlinx.serialization |
| Image loading | Coil 3 |
| Navigation | Navigation Compose |
| Testing | JUnit, Turbine, MockK, Espresso |

Build tooling: AGP 9 (built-in Kotlin 2.4), compileSdk/targetSdk 37, minSdk 29, JVM 11.

## Architecture

The app follows **Clean Architecture** across a multi-module Gradle project. Dependencies only point inward/downward — features depend on core, never the reverse, and lower-level core modules never depend on higher-level ones.

```
app                        — composition root, assembles features + navigation

core/
├── model                  — pure Kotlin domain models (entities)
├── common                 — dispatchers, Result types, cross-cutting utilities
├── domain                 — repository contracts + use cases (framework-free)
├── designsystem           — Material 3 Expressive theme, tokens, reusable UI primitives
├── ui                     — domain-aware shared composables (e.g. food cards)
├── data                   — repository implementations coordinating the sources below
├── database               — Room 3, local source of truth
├── network                — Retrofit + kotlinx.serialization, remote source
├── datastore              — Preferences DataStore, user tastes/interests
└── testing                — shared test doubles and utilities

feature/
├── auth                   — registration + login (device-key auth)
├── onboarding              — capture initial taste preferences
├── discover                — the discover deck
├── recommendations         — personalized "For You" feed
├── favorites                — liked / saved foods
├── details                 — food detail screen (shared destination)
└── profile                  — profile + taste-preference management
```

Each feature module is internally organized into its own `domain` / `data` / `presentation` packages, mirroring the same Clean Architecture layering at a smaller scale.

## Getting started

### Requirements

- Android Studio (with AGP 9 / Kotlin 2.4 support)
- JDK 11+
- Android SDK with API 37

### Build & run

```bash
git clone https://github.com/Saydullin/food-manager-android.git
cd food-manager-android
./gradlew assembleDebug
```

Or open the project in Android Studio and run the `app` configuration on a device/emulator with API 29+.

### Useful Gradle tasks

```bash
./gradlew test              # unit tests across all modules
./gradlew connectedCheck    # instrumented tests
./gradlew assembleRelease   # release APK
```
