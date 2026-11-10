package com.devsgroup.foodmanager.core.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * The host/port the app talks to the backend on — see `NetworkModule.providesRetrofit`.
 * Set per build type in this module's build.gradle.kts: the shared server for debug/release,
 * this computer (from local.properties) for debugLocal/releaseLocal.
 */
object NetworkConfig {
    const val BASE_URL: String = BuildConfig.BASE_URL
}

private val LOOPBACK_HOSTS = setOf("localhost", "127.0.0.1", "10.0.2.2")

/**
 * The dev backend builds absolute media URLs (avatar uploads, etc.) from its own
 * `localhost`, which only resolves on the server itself — not on the device calling
 * it. Rewrite any such loopback host to the actual API host so images the server
 * just told us about are reachable from the phone.
 */
fun String.toReachableMediaUrl(): String {
    val url = toHttpUrlOrNull() ?: return this
    if (url.host !in LOOPBACK_HOSTS) return this
    val apiHost = NetworkConfig.BASE_URL.toHttpUrlOrNull() ?: return this
    return url.newBuilder().scheme(apiHost.scheme).host(apiHost.host).port(apiHost.port).build().toString()
}
