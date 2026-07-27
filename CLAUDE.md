# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Food Manager — a "Tinder for food" Android app (Kotlin, Jetpack Compose). Users swipe through dishes to build a taste profile and get personalized recommendations. Device-key authentication (no passwords).

## Commands

```bash
./gradlew assembleDebug              # build debug APK
./gradlew assembleRelease            # build release APK (see app/build.gradle.kts for APK renaming logic)
./gradlew test                       # unit tests, all modules
./gradlew :feature:auth:test         # unit tests, single module
./gradlew connectedCheck             # instrumented tests (needs device/emulator)
```

There is no lint/detekt/ktlint config and no CI workflow in this repo — `./gradlew test` and a targeted `compileKotlin`/`assembleDebug` are the only verification available. When implementing against `:core:network`/`:core:model`/`:core:domain`/`:core:data`, compile just the touched modules for fast feedback, e.g.:

```bash
./gradlew :core:network:compileDebugKotlin :core:model:compileKotlin :core:domain:compileKotlin :core:data:compileDebugKotlin
```

Fall back to `./gradlew :app:assembleDebug` when a change touches Hilt `@Binds`/`@Provides` — per-module compiles don't catch DI graph errors.

## Architecture

Clean Architecture across a multi-module Gradle project. Dependencies point inward only: features depend on `core`, never the reverse; lower-level core modules never depend on higher-level ones. There is **no `buildSrc`/convention-plugin setup** — every module's `build.gradle.kts` applies `alias(libs.plugins.*)` directly from `gradle/libs.versions.toml`, with the boilerplate repeated per module.

```
app/                         composition root: single NavHost, single Scaffold, Hilt entry point

core/
├── model                    pure Kotlin domain models + typed exceptions (no Android/serialization imports)
├── common                   dispatchers, Result types, cross-cutting utilities
├── domain                   repository contracts + use cases (framework-free — no Android, no :core:common either)
├── designsystem             Material 3 Expressive theme, tokens, shared top-bar plumbing
├── ui                       domain-aware shared composables (e.g. food cards)
├── data                     repository implementations coordinating network/database/datastore
├── database                 Room 3 (androidx.room3, KSP-only) — local source of truth
├── network                  Retrofit 3 + kotlinx.serialization — remote source
├── datastore                Preferences DataStore — session/tokens, taste profile
└── testing                  shared test doubles

feature/
├── auth                     registration + login (device-key auth) — the one fully backend-wired vertical
├── onboarding, discover, recommendations, favorites, details, profile
```

Each feature module owns its own `navigation/` (a `<feature>Screens()` graph-builder extension) and internal `presentation`/`domain`/`data` packages. Features never depend on each other — only `:app` depends on all of them, composing their graph builders into the single `FoodNavHost` (`app/src/main/java/com/devsgroup/foodmanager/navigation/FoodNavHost.kt`).

### Implementation status — read before assuming a layer is "real"

Most of the architecture is scaffolded but not backend-wired yet. Before building on top of a repository or use case, check what's actually there:

- **Auth vertical is the one complete, backend-integrated example** — `AuthApi` (`:core:network`), `DefaultAuthRepository`/`AndroidKeystoreDeviceKeyRepository` (`:core:data`), `AuthPreferencesDataSource` (`:core:datastore`), `AuthException` (`:core:model`). Use it as the template for wiring a new vertical end-to-end.
- **Everything else in `:core:data` is placeholder or in-memory**: `DefaultFoodRepository`, `DefaultRecipeRepository`, `DefaultGroupRepository`, `DefaultSettingsRepository`, `DefaultTasteProfileRepository` return `flowOf(emptyList())` or hold state in a `MutableStateFlow` with no persistence. `DefaultProfileRepository` is partial (reads username from datastore, but edits aren't persisted).
- **Use cases are the exception, not the rule**: only `GetRecommendedFoodsUseCase` and `RecordSwipeUseCase` exist (`core/domain/.../usecase/`). Every other feature's ViewModel injects repository interfaces directly — don't add a use case for a single passthrough call; follow the auth vertical's pattern of calling the repository contract straight from the ViewModel unless there's real domain logic to encapsulate.
- `core/database`, `core/network`, `core/datastore` module READMEs describe the intended shape for pieces not yet built there — treat them as a checklist, not documentation of existing code.

### Shared top app bar — don't add a Scaffold per screen

There is exactly one `Scaffold`/`FoodTopAppBar`, owned by `FoodAppContent` in `app/src/main/java/com/devsgroup/foodmanager/ui/FoodApp.kt`. Top-level destinations get their title auto-derived; any pushed/child screen (edit profile, food detail, etc.) calls `ReportFoodTopBar(config)` (`core/designsystem/.../component/TopBarState.kt`) to set title/back-icon into the persistent bar via a `CompositionLocal`. Never wrap a feature screen in its own `Scaffold`/`TopAppBar` — it would create a second bar or fight the shared one.

### Backend integration

New/changed API endpoints are implemented against the Postman collection at `E:\food-manager-server\postman\food-manager.postman_collection.json` (external to this repo — the backend team edits it independently). Use the `api-integrator` subagent for this; it only touches the network→model→domain→data layers for the specific endpoint(s) named, follows the auth vertical's conventions exactly (bearer auth via `AuthInterceptor`/`AuthTokenProvider` is automatic — never add manual `Authorization` headers), and stops at the ViewModel/Compose boundary.

### Design system

Bold/dark theme, Nunito display font (bundled as a single variable-weight TTF in `core/designsystem/src/main/res/font/`, instanced per weight via `FontVariation.Settings` in `Type.kt` — no runtime/Google Play Services download), Material 3 Expressive (alpha — `material3` version is pinned in `gradle/libs.versions.toml` above what the Compose BOM resolves), `dynamicColor` off, accent colors map to their own container colors per design-system token conventions.

## Build & tooling notes

- AGP 9 with built-in Kotlin (2.4.0) — no separate `kotlin-android` plugin version needed alongside AGP; `compileSdk`/`targetSdk` 37, `minSdk` 29, JVM target 11.
- Room 3 uses the new `androidx.room3` coordinates and is **KSP-only** (no kapt anywhere in the project).
- `material3` is version-pinned separately from the Compose BOM in `gradle/libs.versions.toml` to get Expressive; don't let a BOM bump silently downgrade it.
- `app/build.gradle.kts` renames every built APK to `FoodManager_<versionName>.apk` via the AGP 9 variant API (`androidComponents.onVariants`) — the legacy `applicationVariants` API was removed in AGP 9.
