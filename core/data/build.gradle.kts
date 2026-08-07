plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.devsgroup.foodmanager.core.data"
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
    testOptions {
        unitTests {
            // The repositories here log via android.util.Log, which throws "not mocked" in a
            // JVM unit test. Worse than the noise: a Log call inside a runCatching block gets
            // swallowed, so a test can pass while the path it claims to exercise actually
            // aborted early. Stubbing android.jar to return defaults makes logging a no-op.
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    // Implements the contracts declared in :core:domain by coordinating the data sources.
    api(project(":core:domain"))
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    implementation(project(":core:datastore"))

    implementation(libs.kotlinx.coroutines.android)

    // Auth error mapping: catch retrofit2.HttpException and decode the server's
    // JSON error envelope (both are `implementation` details of :core:network and
    // therefore not on this module's classpath transitively).
    implementation(libs.retrofit)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Outbox sync worker — durable retry of queued interactions once connectivity returns.
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(project(":core:testing"))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
}
