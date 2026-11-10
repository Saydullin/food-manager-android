import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// Which backend the app talks to. Regular builds use the shared server; the *Local build
// types (selected in Build Variants) use a backend running on this computer. Its address
// is per-developer, so it lives in the git-ignored local.properties:
//   localApi.host=192.168.0.4   # the computer's LAN IP, for a phone on the same Wi-Fi
//   localApi.port=3000          # optional
// Without it, 10.0.2.2 — how the Android emulator reaches its host machine.
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val remoteBaseUrl = "http://5.188.20.138:3000/api/"
val localBaseUrl = "http://" + localProperties.getProperty("localApi.host", "10.0.2.2") +
    ":" + localProperties.getProperty("localApi.port", "3000") + "/api/"

android {
    namespace = "com.devsgroup.foodmanager.core.network"
    compileSdk {
        version = release(37)
    }
    defaultConfig {
        minSdk = 29
        buildConfigField("String", "BASE_URL", "\"$remoteBaseUrl\"")
    }
    buildFeatures {
        buildConfig = true
    }
    // Only this module needs its own *Local build types (they change BASE_URL). Every other
    // module is resolved through the app's matchingFallbacks to its debug/release variant.
    buildTypes {
        create("debugLocal") {
            initWith(getByName("debug"))
            buildConfigField("String", "BASE_URL", "\"$localBaseUrl\"")
            matchingFallbacks += listOf("debug")
        }
        create("releaseLocal") {
            initWith(getByName("release"))
            buildConfigField("String", "BASE_URL", "\"$localBaseUrl\"")
            matchingFallbacks += listOf("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":core:common"))

    // Retrofit 3 + OkHttp 5 + first-party kotlinx.serialization converter.
    implementation(libs.bundles.network)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
