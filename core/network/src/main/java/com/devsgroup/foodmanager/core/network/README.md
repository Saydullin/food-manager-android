# :core:network

Remote source for the offline-first architecture, using **Retrofit 3 + OkHttp 5**
with the first-party **kotlinx.serialization** converter. Dependencies and the
serialization plugin are already wired in `build.gradle.kts`; add:

- `dto/FoodDto.kt` — `@Serializable` transport models (kept separate from `:core:model`).
- `FoodApi.kt` — Retrofit service interface.
- `di/NetworkModule.kt` — Hilt `@Module` providing `Json`, `OkHttpClient`, and
  `Retrofit` (via `json.asConverterFactory("application/json".toMediaType())`).

Then inject `FoodApi` into `:core:data` and map DTOs → domain models.
