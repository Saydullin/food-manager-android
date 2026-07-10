plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.devsgroup.foodmanager.core.designsystem"
    compileSdk {
        version = release(37)
    }
    defaultConfig {
        minSdk = 29
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Exposed with `api` so every consumer inherits the Compose + Material 3 Expressive surface.
    implementation(platform(libs.androidx.compose.bom))
    api(libs.bundles.compose)
    // Unbounded is delivered via the Google Fonts downloadable-fonts provider.
    api(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
