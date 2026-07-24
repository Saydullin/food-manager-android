package com.devsgroup.foodmanager.core.network.upload

import com.devsgroup.foodmanager.core.network.upload.model.UploadImageResponse
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Retrofit surface for the Food Manager uploads backend (relative to the `/api/`
 * base URL). This endpoint is protected; the bearer token is attached automatically
 * by the auth interceptor whenever a session exists, so no @Header param here.
 */
interface UploadApi {

    @Multipart
    @POST("uploads/image")
    suspend fun uploadImage(@Part image: MultipartBody.Part): UploadImageResponse
}
