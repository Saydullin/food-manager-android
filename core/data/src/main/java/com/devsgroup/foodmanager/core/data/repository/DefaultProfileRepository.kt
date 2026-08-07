package com.devsgroup.foodmanager.core.data.repository

import com.devsgroup.foodmanager.core.common.di.Dispatcher
import com.devsgroup.foodmanager.core.common.di.FoodDispatcher
import com.devsgroup.foodmanager.core.data.mapper.toDomain
import com.devsgroup.foodmanager.core.data.worker.ProfileSyncScheduler
import com.devsgroup.foodmanager.core.datastore.AuthPreferencesDataSource
import com.devsgroup.foodmanager.core.datastore.ProfilePreferenceList
import com.devsgroup.foodmanager.core.datastore.ProfilePreferencesDataSource
import com.devsgroup.foodmanager.core.datastore.valuesOf
import com.devsgroup.foodmanager.core.datastore.withValues
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
import kotlinx.coroutines.flow.combine
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
    private val profileSyncScheduler: ProfileSyncScheduler,
    private val json: Json,
    @Dispatcher(FoodDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : ProfileRepository {

    private val cache = MutableStateFlow<UserProfile?>(null)

    override val emailVerified: Flow<Boolean> = profilePrefs.profileFlow.map { it?.emailVerified ?: false }

    override val isBanned: Flow<Boolean> = authPrefs.accountBannedFlow

    /**
     * The stored flag, OR any taste data already on the account — see the contract for
     * why the second half matters. Reading the profile from DataStore rather than the
     * in-memory cache keeps this answerable on a cold start, before any network refresh.
     */
    override val onboardingComplete: Flow<Boolean> =
        combine(profilePrefs.onboardingCompleteFlow, profilePrefs.profileFlow) { completed, profile ->
            completed || profile?.hasTasteData() == true
        }

    private fun UserProfile.hasTasteData(): Boolean =
        diets.isNotEmpty() || foodExceptions.isNotEmpty() || foodPreferences.isNotEmpty()

    override suspend fun completeOnboarding() = profilePrefs.setOnboardingComplete()

    override fun invalidateCache() {
        cache.value = null
    }

    override fun profile(): Flow<UserProfile> = cache
        .onStart {
            if (cache.value == null) {
                // Show whatever's persisted locally right away; the caller is
                // responsible for triggering a network refresh alongside this.
                cache.value = profilePrefs.currentProfile()
            }
        }
        .filterNotNull()

    override suspend fun hasCachedProfile(): Boolean =
        cache.value != null || profilePrefs.currentProfile() != null

    override suspend fun refresh() {
        val server = apiCall {
            val user = profileApi.myProfile().user
            authPrefs.updateAccountBanned(user.isBanned)
            user.toDomain()
        }
        persistServerProfile(server)
        // A successful round-trip means connectivity is back, so give any edit still
        // queued from an offline session its chance to reach the server.
        if (profilePrefs.pendingSyncLists().isNotEmpty()) profileSyncScheduler.scheduleSync()
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
        persistServerProfile(updated, identityFrom = profile)
    }

    override suspend fun uploadAvatarImage(bytes: ByteArray, fileName: String, mimeType: String): String =
        apiCall {
            val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("image", fileName, requestBody)
            uploadApi.uploadImage(part).imageUrl.toReachableMediaUrl()
        }

    override suspend fun updateFoodPreferences(preferences: List<String>) =
        updatePreferenceList(ProfilePreferenceList.FOOD_PREFERENCES, preferences)

    override suspend fun updateFoodExceptions(exceptions: List<String>) =
        updatePreferenceList(ProfilePreferenceList.FOOD_EXCEPTIONS, exceptions)

    override suspend fun updateDiets(diets: List<String>) =
        updatePreferenceList(ProfilePreferenceList.DIETS, diets)

    /**
     * Local-first edit. The new list lands in the in-memory cache and DataStore *before*
     * any network call, so the change shows immediately and survives a restart whether or
     * not there's a connection. The push that follows is best-effort: a failure a later
     * retry could still fix (offline, server error) leaves the list flagged pending for
     * [ProfileSyncWorker] to replay once connectivity returns, while a rejection the
     * server will never accept (a bad diet code, an expired session) is rolled back and
     * rethrown so the UI stops showing a value that can't exist.
     */
    private suspend fun updatePreferenceList(list: ProfilePreferenceList, values: List<String>) {
        val previous = cache.value ?: profilePrefs.currentProfile()
        cache.value = previous?.withValues(list, values)
        // Flagged pending *before* the request rather than after it fails: if the process
        // dies mid-push the edit is still queued for replay instead of stranded local-only,
        // where the next refresh would quietly overwrite it with the server's older copy.
        profilePrefs.saveList(list, values, pendingSync = true)

        val updated = try {
            apiCall { profileApi.setPreferenceList(list, values).user.toDomain() }
        } catch (e: ProfileException) {
            if (!e.isRetryable()) {
                cache.value = previous
                profilePrefs.saveList(list, previous?.valuesOf(list).orEmpty(), pendingSync = false)
                throw e
            }
            Log.i(TAG, "Queued $list for later sync: ${e.message}")
            profileSyncScheduler.scheduleSync()
            return
        }
        // Settled first so the merge below takes the server's canonical copy of this list
        // (it sorts and dedupes what we sent) rather than preserving it as still-pending —
        // unless a concurrent edit already moved the list on, which keeps it queued.
        profilePrefs.clearPendingSyncIfUnchanged(list, values)
        persistServerProfile(updated)
    }

    /**
     * Merges a fresh server profile over the local one and persists it. [identityFrom]
     * supplies the fields the wire schema doesn't carry (`email`, `emailVerified`,
     * `cookingAttitude`) — the caller's own edit where there is one, otherwise whatever
     * was cached. Any list still queued for the server keeps its local value: the
     * response predates that edit, so adopting its copy would silently undo it.
     */
    private suspend fun persistServerProfile(server: UserProfile, identityFrom: UserProfile? = null): UserProfile {
        val previous = cache.value ?: profilePrefs.currentProfile()
        val identity = identityFrom ?: previous
        var merged = server.copy(
            email = identity?.email,
            emailVerified = identity?.emailVerified ?: false,
            cookingAttitude = identity?.cookingAttitude,
        )
        if (previous != null) {
            for (list in profilePrefs.pendingSyncLists()) {
                merged = merged.withValues(list, previous.valuesOf(list))
            }
        }
        cache.value = merged
        profilePrefs.saveProfile(merged)
        return merged
    }

    /** True for failures a later replay could still fix — no connectivity, or a server-side error. */
    private fun ProfileException.isRetryable(): Boolean = when (this) {
        is ProfileException.Network -> true
        is ProfileException.Unknown -> {
            val code = (cause as? HttpException)?.code()
            code != null && code in 500..599
        }
        else -> false
    }

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
