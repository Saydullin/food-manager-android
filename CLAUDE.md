# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Food Manager — a "Tinder for food" Android app (Kotlin, Jetpack Compose). Users swipe through dishes to build a taste profile and get personalized recommendations. Device-key authentication (no passwords). `README.md` / `README.ru.md` are the English/Russian user-facing docs.

## Commands

```bash
./gradlew assembleDebug              # build debug APK
./gradlew assembleRelease            # build release APK (see app/build.gradle.kts for APK renaming logic)
./gradlew test                       # unit tests, all modules
./gradlew :feature:auth:test         # unit tests, single module
./gradlew :core:data:testDebugUnitTest --tests "*DefaultProfileRepositoryOnboardingTest"   # single test class
./gradlew connectedCheck             # instrumented tests (needs device/emulator)
```

There is no lint/detekt/ktlint config and no CI workflow in this repo — `./gradlew test` and a targeted `compileKotlin`/`assembleDebug` are the only verification available. When implementing against `:core:network`/`:core:model`/`:core:domain`/`:core:data`, compile just the touched modules for fast feedback, e.g.:

```bash
./gradlew :core:network:compileDebugKotlin :core:model:compileKotlin :core:domain:compileKotlin :core:data:compileDebugKotlin
```

Fall back to `./gradlew :app:assembleDebug` when a change touches Hilt `@Binds`/`@Provides`/`@AssistedInject` workers — per-module compiles don't catch DI graph errors.

## Architecture

Clean Architecture across a multi-module Gradle project. Dependencies point inward only: features depend on `core`, never the reverse; lower-level core modules never depend on higher-level ones. There is **no `buildSrc`/convention-plugin setup** — every module's `build.gradle.kts` applies `alias(libs.plugins.*)` directly from `gradle/libs.versions.toml`, with the boilerplate repeated per module.

```
app/                         composition root: single NavHost, single Scaffold, Hilt entry point

core/
├── model                    pure Kotlin domain models + typed exceptions (no Android/serialization imports)
├── common                   dispatchers (@Dispatcher(FoodDispatcher.IO)), Result types, AccountBanNotifier
├── domain                   repository contracts + use cases (framework-free — no Android, no :core:common either)
├── designsystem             Material 3 Expressive theme, tokens, shared top-bar plumbing
├── ui                       domain-aware shared composables (e.g. food cards)
├── data                     repository implementations, mappers, WorkManager sync workers
├── database                 Room 3 (androidx.room3, KSP-only) — local source of truth
├── network                  Retrofit 3 + kotlinx.serialization — remote source, one package per API vertical
├── datastore                Preferences DataStore — auth tokens, profile, settings
└── testing                  shared test doubles

feature/
├── auth, onboarding, dinner, discover, recommendations, favorites, details, profile
```

`feature/dinner` is the app's home since the pivot away from swipe-first: `WhatToEatRoute` ("Что едим?" — cook / order / eat out; only cooking is built) is the start destination, and `CookDinnerRoute` is a single-destination wizard (eaters → kitchen equipment, asked once → ingredient source → optional pantry → dishes that suit *everyone*, from the server).

Each feature module owns its own `navigation/` (a `<feature>Screens()` graph-builder extension) and internal `presentation`/`domain`/`data` packages. Features never depend on each other — only `:app` depends on all of them, composing their graph builders into the single `FoodNavHost` (`app/src/main/java/com/devsgroup/foodmanager/navigation/FoodNavHost.kt`).

### Implementation status — check before assuming a layer is "real"

Backend-wired verticals (each has an `*Api` in `:core:network/<vertical>/`, mappers in `:core:data/mapper/`, and a `Default*Repository`):

- **Auth** — `AuthApi`, `DefaultAuthRepository`, `AndroidKeystoreDeviceKeyRepository`, `AuthPreferencesDataSource`, `AuthException`. The original template for wiring a vertical end-to-end.
- **Food feed** — `FoodApi` + `DefaultFoodRepository`. Offline-first: Room (`FoodDao`, `FeedMetaDao`) is the single source of truth for the feed; the network only fetches the next page. Swipes/interactions are written to an **outbox** (`OutboxDao`) and replayed by `OutboxSyncWorker` (scheduled via `OutboxScheduler`), oldest first; network errors retry, unrecoverable entries are dropped.
- **Profile** — `ProfileApi` + `UploadApi` + `DefaultProfileRepository`. One domain edit fans out to `PATCH /users/me` and/or `PUT|DELETE /users/me/image`. Profile/preferences are persisted in `ProfilePreferencesDataSource` and pushed via `ProfileSyncWorker`. Also owns onboarding completion (`onboardingComplete`/`completeOnboarding`) and email verification.
- **Settings** — `SettingsApi` + `DefaultSettingsRepository` + `SettingsPreferencesDataSource`.
- **Cook dinner** — `HouseholdApi` + `DinnerApi`. `DefaultHouseholdRepository` (Room `eaters`), `DefaultKitchenRepository` and `DefaultPantryRepository` (DataStore) are local-first: write locally, push at once, otherwise queue for `HouseholdSyncWorker` (rows/keys carry a pending flag; eater deletes are tombstones). The self eater ("Я", `Eater.SELF_ID`) is **not** stored — it's synthesized from `UserProfile` (`sex`, `medicalRestrictions`, taste lists) and saved via `ProfileRepository.updateDinnerSelf`. `DefaultDinnerRepository` calls `GET /dinner/suggestions` online-only — all matching rules (allergies, diets, doctor's restrictions, equipment, pantry) live **on the server**; don't re-implement them client-side. Suggested dishes are upserted into the Room food cache (keeping feed slot/like) because the details screen reads Room only. All of it is cleared on logout.

Still placeholder / in-memory: `DefaultRecipeRepository` (`flowOf(emptyList())`), `DefaultGroupRepository`, `DefaultTasteProfileRepository` (`MutableStateFlow`, no persistence).

Cross-cutting network behavior lives in `:core:network/interceptor/`: `AuthInterceptor` (bearer token), `TokenRefreshAuthenticator` (refresh on 401), and `AccountBanInterceptor` (detects the `ACCOUNT_BANNED` error envelope on *any* response and signals `AccountBanNotifier`, surfaced as `ProfileRepository.isBanned`). Don't re-implement auth headers or ban detection per repository.

Use cases are the exception, not the rule: only `GetRecommendedFoodsUseCase`, `LoadNextFeedPageUseCase`, `RecordSwipeUseCase` and `SuggestDinnerUseCase` exist (`core/domain/.../usecase/`). Other ViewModels inject repository interfaces directly — don't add a use case for a single passthrough call.

The `core/database`, `core/network`, `core/datastore` READMEs (inside `src/main/java/...`) predate the real implementations — treat them as historical intent, not documentation.

### Shared top app bar — don't add a Scaffold per screen

There is exactly one `Scaffold`/`FoodTopAppBar`, owned by `FoodAppContent` in `app/src/main/java/com/devsgroup/foodmanager/ui/FoodApp.kt`. Top-level destinations get their title auto-derived; any pushed/child screen (edit profile, food detail, etc.) calls `ReportFoodTopBar(config)` (`core/designsystem/.../component/TopBarState.kt`) to set title/back-icon into the persistent bar via a `CompositionLocal`. Never wrap a feature screen in its own `Scaffold`/`TopAppBar` — it would create a second bar or fight the shared one.

### Backend integration

New/changed API endpoints are implemented against the Postman collection at `C:\Users\predator\food-manager-server\postman\food-manager.postman_collection.json` (external to this repo — the backend team edits it independently; `API_DOCS.md` and `docs/tasks/` there are the prose companions). Use the `api-integrator` subagent (`.claude/agents/`) for this; it only touches the network→model→domain→data layers for the specific endpoint(s) named, follows the existing verticals' conventions, and stops at the ViewModel/Compose boundary. For explanatory "why is it structured this way" questions, the read-only `architecture-mentor` subagent exists.

### Design system

Bold/dark theme, Nunito display font (bundled as a single variable-weight TTF in `core/designsystem/src/main/res/font/`, instanced per weight via `FontVariation.Settings` in `Type.kt` — no runtime/Google Play Services download), Material 3 Expressive (alpha), `dynamicColor` off, accent colors map to their own container colors per design-system token conventions. `design-templates/` and `design_handoff_profile_screen/` at the repo root hold design references.

## Build & tooling notes

- AGP 9 with built-in Kotlin (2.4.0) — no separate `kotlin-android` plugin needed; `compileSdk`/`targetSdk` 37, `minSdk` 29, JVM target 11.
- Room 3 uses the new `androidx.room3` coordinates and is **KSP-only** (no kapt anywhere in the project).
- `material3` is version-pinned separately from the Compose BOM in `gradle/libs.versions.toml` to get Expressive; don't let a BOM bump silently downgrade it.
- `app/build.gradle.kts` renames every built APK to `FoodManager_<versionName>.apk` (`…-local.apk` for the *Local types) via the AGP 9 variant API (`androidComponents.onVariants`) — the legacy `applicationVariants` API was removed in AGP 9.
- Build types: `debug`/`release` talk to the shared server; **`debugLocal`/`releaseLocal`** talk to a backend on the developer's machine at `localApi.host`/`localApi.port` from the git-ignored `local.properties` (default `10.0.2.2:3000`, the emulator's view of the host). Only `:core:network` declares the *Local types (they set `BuildConfig.BASE_URL`, read via `NetworkConfig.BASE_URL`); `:app` and `:core:network` use `matchingFallbacks` for every other module — a new module needs nothing. *Local builds install side by side as `com.devsgroup.foodmanager.local` ("Food Manager (local)") and allow cleartext HTTP to any host via `app/src/local/res`.
