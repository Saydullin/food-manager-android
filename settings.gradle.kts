pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Food Manager"

include(":app")

// Core — shared infrastructure, layered by responsibility.
include(":core:model")        // Pure Kotlin domain models (entities)
include(":core:common")       // Dispatchers, Result types, cross-cutting utilities
include(":core:domain")       // Repository contracts + use cases (framework-free)
include(":core:designsystem") // Material 3 Expressive theme, tokens, reusable UI primitives
include(":core:ui")           // Domain-aware shared composables (e.g. food cards)
include(":core:data")         // Repository implementations coordinating the sources below
include(":core:database")     // Room 3 — local source of truth
include(":core:network")      // Retrofit + kotlinx.serialization — remote source
include(":core:datastore")    // Preferences DataStore — user tastes/interests
include(":core:testing")      // Shared test doubles and utilities

// Features — each is self-contained and internally organized into
// domain / data / presentation packages (Clean Architecture within the module).
include(":feature:onboarding")      // Capture initial taste preferences
include(":feature:discover")        // The swipe deck ("Tinder for food")
include(":feature:recommendations") // Personalized "For You" feed
include(":feature:favorites")       // Liked / saved foods
include(":feature:details")         // Food detail screen (shared destination)
include(":feature:profile")         // Profile + taste-preference management
