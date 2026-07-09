# :core:datastore

Persists lightweight user state — primarily the **taste profile / interests** that
power personalization — using **Preferences DataStore**. Dependencies are wired in
`build.gradle.kts`; add:

- `TasteProfilePreferences.kt` — a wrapper over `DataStore<Preferences>` exposing
  `Flow`s for favorite cuisines, dietary tags, and the onboarding-complete flag.
- `di/DataStoreModule.kt` — Hilt `@Module` providing the `DataStore` instance.

Then back `DefaultTasteProfileRepository` in `:core:data` with this source.
