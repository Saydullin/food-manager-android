package com.devsgroup.foodmanager.core.data.repository

import android.util.Log
import com.devsgroup.foodmanager.core.model.ProfileException
import com.devsgroup.foodmanager.core.network.auth.model.ErrorResponse
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

/**
 * Error handling shared by the repositories over the account's own cook-dinner data
 * (household, kitchen, pantry, dinner suggestions). They speak the profile's error
 * vocabulary — it's the same account, the same failure modes — so a ViewModel handles
 * them the same way it handles a profile edit.
 *
 * Retrofit's suspend calls are main-safe, so unlike the profile repository there's no
 * dispatcher hop here.
 */
internal suspend fun <T> accountApiCall(json: Json, tag: String, block: suspend () -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    Log.w(tag, "HTTP ${e.code()} calling the account API", e)
    throw e.toProfileException(json)
} catch (e: IOException) {
    Log.i(tag, "No connectivity calling the account API: ${e.message}")
    throw ProfileException.Network(e)
} catch (e: ProfileException) {
    throw e
} catch (e: Exception) {
    Log.e(tag, "Unexpected error calling the account API", e)
    throw ProfileException.Unknown(e.message, e)
}

/** True for failures a later replay could still fix — no connectivity, or a server-side error. */
internal fun ProfileException.isRetryable(): Boolean = when (this) {
    is ProfileException.Network -> true
    is ProfileException.Unknown -> (cause as? HttpException)?.code()?.let { it in 500..599 } ?: false
    else -> false
}

private fun HttpException.toProfileException(json: Json): ProfileException {
    val serverMessage = runCatching {
        response()?.errorBody()?.string()?.takeIf { it.isNotBlank() }
            ?.let { json.decodeFromString(ErrorResponse.serializer(), it).error.message }
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: message()
    return when (code()) {
        400 -> ProfileException.InvalidRequest(serverMessage)
        401, 403 -> ProfileException.Unauthorized(serverMessage)
        404 -> ProfileException.NotFound(serverMessage)
        409 -> ProfileException.Conflict(serverMessage)
        else -> ProfileException.Unknown(serverMessage, this)
    }
}
