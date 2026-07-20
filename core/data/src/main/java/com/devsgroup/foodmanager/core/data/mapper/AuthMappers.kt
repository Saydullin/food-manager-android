package com.devsgroup.foodmanager.core.data.mapper

import com.devsgroup.foodmanager.core.model.AuthChallenge
import com.devsgroup.foodmanager.core.model.AuthDevice
import com.devsgroup.foodmanager.core.model.AuthSession
import com.devsgroup.foodmanager.core.model.AuthTokens
import com.devsgroup.foodmanager.core.model.AuthUser
import com.devsgroup.foodmanager.core.network.auth.model.ChallengeResponse
import com.devsgroup.foodmanager.core.network.auth.model.DeviceDto
import com.devsgroup.foodmanager.core.network.auth.model.RecoveryConfirmResponse
import com.devsgroup.foodmanager.core.network.auth.model.SessionResponse
import com.devsgroup.foodmanager.core.network.auth.model.TokenPairResponse
import com.devsgroup.foodmanager.core.network.auth.model.UserDto

/** Maps auth wire models (`:core:network`) to domain models (`:core:model`). */

internal fun UserDto.toDomain() = AuthUser(
    id = id,
    username = username,
    email = email,
    emailVerified = emailVerified,
)

internal fun DeviceDto.toDomain() = AuthDevice(
    id = id,
    label = deviceLabel,
    createdAt = createdAt,
    lastUsedAt = lastUsedAt,
)

internal fun TokenPairResponse.toDomain() = AuthTokens(
    accessToken = accessToken,
    refreshToken = refreshToken,
)

internal fun ChallengeResponse.toDomain() = AuthChallenge(
    challengeId = challengeId,
    nonce = nonce,
)

internal fun SessionResponse.toDomain() = AuthSession(
    user = user.toDomain(),
    device = device.toDomain(),
    tokens = AuthTokens(accessToken, refreshToken),
)

internal fun RecoveryConfirmResponse.toDomain() = AuthSession(
    user = null,
    device = device.toDomain(),
    tokens = AuthTokens(accessToken, refreshToken),
)
