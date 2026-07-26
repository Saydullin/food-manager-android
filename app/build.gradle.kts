import com.android.build.api.variant.impl.VariantOutputImpl

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val appVersionName = "1.0-beta.2"

android {
    namespace = "com.devsgroup.foodmanager"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.devsgroup.foodmanager"
        minSdk = 29
        targetSdk = 37
        versionName = appVersionName
        versionCode = versionCode(versionName)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

// Rename every built APK to FoodManager_<versionName>.apk.
// AGP 9 removed the legacy `applicationVariants` API, so this hooks the new
// variant API instead. Debug/release land in their own outputs/apk/<type>/ folder.
androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            (output as? VariantOutputImpl)?.outputFileName?.set("FoodManager_$appVersionName.apk")
        }
    }
}

fun versionCode(versionName: String?): Int {
    if (versionName.isNullOrBlank()) return 1

    val numbers = Regex("""\d+""")
        .findAll(versionName)
        .map { it.value.toLong() }
        .toList()

    if (numbers.isEmpty()) return 1

    val major = numbers.getOrElse(0) { 0 }
    val minor = numbers.getOrElse(1) { 0 }
    val patch = numbers.getOrElse(2) { 0 }
    val build = numbers.getOrElse(3) { 0 }
    val extra = numbers.drop(4).sum()

    val result = major * 1_000_000_000 +
            minor * 1_000_000 +
            patch * 10_000 +
            build * 100 +
            extra

    return result
        .coerceAtMost(2_100_000_000L)
        .toInt()
        .coerceAtLeast(1)
}

dependencies {
    // Feature modules — the app is the composition root that assembles them.
    implementation(project(":feature:auth"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:discover"))
    implementation(project(":feature:recommendations"))
    implementation(project(":feature:favorites"))
    implementation(project(":feature:details"))
    implementation(project(":feature:profile"))

    // Core modules needed at the composition root (theme + DI wiring for data).
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    // :core:data is wired at runtime so Hilt can bind repository implementations.
    implementation(project(":core:data"))

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.bundles.lifecycle.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Core / lifecycle
    implementation(libs.androidx.core.ktx)
    // AppCompatDelegate.setApplicationLocales — per-app language switching without AppCompatActivity.
    implementation(libs.androidx.appcompat)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // WorkManager — Configuration.Provider/HiltWorkerFactory wiring for the outbox sync worker.
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)

    // Debug tooling
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
