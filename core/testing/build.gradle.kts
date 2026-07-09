plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.devsgroup.foodmanager.core.testing"
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
}

dependencies {
    // Shared test doubles, fixtures and coroutine helpers reused across modules.
    api(project(":core:model"))
    api(project(":core:domain"))

    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
    api(libs.mockk)
}
