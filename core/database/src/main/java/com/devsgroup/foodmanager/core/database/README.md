# :core:database

Local **source of truth** for the offline-first architecture, backed by **Room 3**
(`androidx.room3`, KSP-only). The Room and Hilt dependencies are already wired in
`build.gradle.kts`; add the schema here:

- `entity/FoodEntity.kt` — `@Entity` tables (foods, swipes, likes).
- `dao/FoodDao.kt` — `@Dao` interfaces exposing `Flow`-returning queries.
- `FoodDatabase.kt` — `@Database(entities = [...], version = 1)`.
- `di/DatabaseModule.kt` — Hilt `@Module` providing the database + DAOs.

Then inject the DAOs into `:core:data` repository implementations.
