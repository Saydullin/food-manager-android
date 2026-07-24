package com.devsgroup.foodmanager.core.network.di

import android.util.Log
import com.devsgroup.foodmanager.core.network.NetworkConfig
import com.devsgroup.foodmanager.core.network.auth.AuthApi
import com.devsgroup.foodmanager.core.network.interceptor.AuthInterceptor
import com.devsgroup.foodmanager.core.network.interceptor.TokenRefreshAuthenticator
import com.devsgroup.foodmanager.core.network.profile.ProfileApi
import com.devsgroup.foodmanager.core.network.upload.UploadApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Marks the standalone client/Retrofit/[AuthApi] used only to call `auth/refresh`.
 * It carries neither [AuthInterceptor] nor [TokenRefreshAuthenticator], so refreshing
 * never re-enters the authenticator — using the main [AuthApi] here would form a DI
 * cycle (authenticator -> AuthApi -> Retrofit -> OkHttpClient -> authenticator).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RefreshApi

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = NetworkConfig.BASE_URL

    /**
     * [HttpLoggingInterceptor.Level.BODY] decodes the entire request body as text.
     * For multipart uploads (profile image) that means reading a multi-MB image as
     * UTF-8 and dumping it to Logcat, which stalls the request and floods the log
     * with garbled binary — so the *request* side of multipart calls is logged at
     * HEADERS level, but the (small, JSON) *response* body is still logged manually
     * so the returned `imageUrl` is visible for diagnosing upload issues.
     */
    private fun loggingInterceptor(): Interceptor {
        val headersOnly = HttpLoggingInterceptor().apply { level = Level.HEADERS }
        val fullBody = HttpLoggingInterceptor().apply { level = Level.BODY }
        return Interceptor { chain ->
            val request = chain.request()
            if (request.body?.contentType()?.type == "multipart") {
                val response = headersOnly.intercept(chain)
                val responseBody = runCatching { response.peekBody(4096).string() }.getOrNull()
                Log.d("OkHttp", "<-- response body for ${request.url}: $responseBody")
                response
            } else {
                fullBody.intercept(chain)
            }
        }
    }

    @Provides
    @Singleton
    fun providesJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun providesOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenRefreshAuthenticator: TokenRefreshAuthenticator,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenRefreshAuthenticator)
            .addInterceptor(loggingInterceptor())
            .build()

    @Provides
    @Singleton
    fun providesRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun providesAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun providesProfileApi(retrofit: Retrofit): ProfileApi = retrofit.create(ProfileApi::class.java)

    @Provides
    @Singleton
    fun providesUploadApi(retrofit: Retrofit): UploadApi = retrofit.create(UploadApi::class.java)

    @Provides
    @Singleton
    @RefreshApi
    fun providesRefreshOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor())
            .build()

    @Provides
    @Singleton
    @RefreshApi
    fun providesRefreshRetrofit(@RefreshApi client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    @RefreshApi
    fun providesRefreshAuthApi(@RefreshApi retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)
}
