package com.devsgroup.foodmanager.core.data.repository

import com.devsgroup.foodmanager.core.common.di.Dispatcher
import com.devsgroup.foodmanager.core.common.di.FoodDispatcher
import com.devsgroup.foodmanager.core.data.mapper.toDomain
import com.devsgroup.foodmanager.core.datastore.AuthPreferencesDataSource
import com.devsgroup.foodmanager.core.datastore.ProfilePreferencesDataSource
import com.devsgroup.foodmanager.core.domain.repository.ProfileRepository
import com.devsgroup.foodmanager.core.model.ProfileException
import com.devsgroup.foodmanager.core.model.UserProfile
import com.devsgroup.foodmanager.core.network.auth.model.ErrorResponse
import com.devsgroup.foodmanager.core.network.profile.ProfileApi
import com.devsgroup.foodmanager.core.network.profile.model.EditProfileRequest
import com.devsgroup.foodmanager.core.network.profile.model.SetProfileImageRequest
import com.devsgroup.foodmanager.core.network.toReachableMediaUrl
import com.devsgroup.foodmanager.core.network.upload.UploadApi
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Backs the signed-in user's profile with the real `/users/me` endpoints ([ProfileApi]).
 * The server splits editing across two calls — `PATCH /users/me` for the text/age
 * fields and `PUT`/`DELETE /users/me/image` for the avatar — so [updateProfile] fans
 * a single domain edit out to whichever of those the changed fields require, then
 * caches the server's (authoritative) response for the process lifetime. `email` and
 * `cookingAttitude` aren't part of this vertical's wire schema (email is owned by the
 * auth email-verification flow; cookingAttitude is a purely client-side preference),
 * so both are carried forward from the caller/cache rather than sent to the server.
 */
@Singleton
class DefaultProfileRepository @Inject constructor(
    private val profileApi: ProfileApi,
    private val uploadApi: UploadApi,
    private val profilePrefs: ProfilePreferencesDataSource,
    private val authPrefs: AuthPreferencesDataSource,
    private val json: Json,
    @Dispatcher(FoodDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : ProfileRepository {

    private val cache = MutableStateFlow<UserProfile?>(null)

    override val emailVerified: Flow<Boolean> = profilePrefs.profileFlow.map { it?.emailVerified ?: false }

    override val isBanned: Flow<Boolean> = authPrefs.accountBannedFlow

    override fun invalidateCache() {
        cache.value = null
    }

    override fun profile(): Flow<UserProfile> = cache
        .onStart {
            if (cache.value != null) return@onStart
            val local = profilePrefs.currentProfile()
            cache.value = try {
                fetchProfile()
            } catch (e: ProfileException.Network) {
                // Offline with nothing fresh from the server — fall back to whatever
                // was persisted locally (e.g. right after registration) instead of
                // failing the whole screen.
                local ?: throw e
            }
        }
        .filterNotNull()

    override suspend fun refresh() {
        cache.value = fetchProfile()
    }

    override suspend fun updateProfile(profile: UserProfile) {
        val currentUsername = cache.value?.username?.removePrefix("@")
        val newUsername = profile.username.removePrefix("@")
        val updated = apiCall {
            var result = profileApi.editProfile(
                EditProfileRequest(
                    // Only sent when actually renaming — resending the unchanged
                    // handle risks a spurious 409 if the server's uniqueness check
                    // doesn't special-case "taken by yourself".
                    username = newUsername.takeIf { it.isNotBlank() && it != currentUsername },
                    name = profile.name,
                    age = profile.age,
                    status = profile.statusEmoji.orEmpty(),
                    description = profile.bio.orEmpty(),
                ),
            ).user.toDomain()

            val newAvatarUrl = profile.avatarUrl
            if (newAvatarUrl != result.avatarUrl) {
                result = if (newAvatarUrl.isNullOrBlank()) {
                    profileApi.removeProfileImage().user.toDomain()
                } else {
                    profileApi.setProfileImage(SetProfileImageRequest(newAvatarUrl)).user.toDomain()
                }
            }
            result
        }
        val merged = updated.copy(
            email = profile.email,
            emailVerified = profile.emailVerified,
            cookingAttitude = profile.cookingAttitude,
        )
        cache.value = merged
        profilePrefs.saveProfile(merged)
    }

    override suspend fun uploadAvatarImage(bytes: ByteArray, fileName: String, mimeType: String): String =
        apiCall {
            val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("image", fileName, requestBody)
            uploadApi.uploadImage(part).imageUrl.toReachableMediaUrl()
        }

    private suspend fun fetchProfile(): UserProfile = apiCall {
        val local = cache.value ?: profilePrefs.currentProfile()
        profileApi.myProfile().user.toDomain().copy(
            email = local?.email,
            emailVerified = local?.emailVerified ?: false,
            cookingAttitude = cache.value?.cookingAttitude,
        )
    }.also { profilePrefs.saveProfile(it) }

    /** Runs [block] on the IO dispatcher and normalizes failures to [ProfileException]. */
    private suspend fun <T> apiCall(block: suspend () -> T): T = withContext(ioDispatcher) {
        try {
            block()
        } catch (e: ProfileException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP error calling profile API", e)
            throw e.toProfileException()
        } catch (e: IOException) {
            Log.e(TAG, "Network error calling profile API", e)
            throw ProfileException.Network(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error calling profile API", e)
            throw ProfileException.Unknown(e.message, e)
        }
    }

    private companion object {
        const val TAG = "DefaultProfileRepository"
    }

    private fun HttpException.toProfileException(): ProfileException {
        val serverMessage = parseErrorMessage() ?: message()
        return when (code()) {
            400 -> ProfileException.InvalidRequest(serverMessage)
            401, 403 -> ProfileException.Unauthorized(serverMessage)
            404 -> ProfileException.NotFound(serverMessage)
            409 -> ProfileException.Conflict(serverMessage)
            else -> ProfileException.Unknown(serverMessage, this)
        }
    }

    /** Extracts `error.message` from the server's `{ "error": {...} }` envelope. */
    private fun HttpException.parseErrorMessage(): String? = runCatching {
        val body = response()?.errorBody()?.string()
        if (body.isNullOrBlank()) {
            null
        } else {
            json.decodeFromString(ErrorResponse.serializer(), body).error.message.takeIf { it.isNotBlank() }
        }
    }.getOrNull()
}
