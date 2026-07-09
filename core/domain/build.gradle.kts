plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Pure-Kotlin module: repository contracts + use cases, independent of any framework.
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
    api(project(":core:model"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.collections.immutable)
    // @Inject on use-case constructors (Hilt provides the implementations downstream).
    implementation(libs.javax.inject)
}
