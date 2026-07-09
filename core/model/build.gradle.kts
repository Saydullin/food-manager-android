plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Pure-Kotlin module: domain models must stay free of Android and framework types.
// Target JVM 11 so the output is consumable by the Android modules.
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.collections.immutable)
}
