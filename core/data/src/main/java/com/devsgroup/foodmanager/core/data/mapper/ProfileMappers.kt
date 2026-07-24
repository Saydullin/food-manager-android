package com.devsgroup.foodmanager.core.data.mapper

import com.devsgroup.foodmanager.core.model.UserProfile
import com.devsgroup.foodmanager.core.network.profile.model.ProfileUserDto
import com.devsgroup.foodmanager.core.network.toReachableMediaUrl

/**
 * Maps profile wire models (`:core:network`) to domain models (`:core:model`).
 * `email`/`cookingAttitude` have no representation in this DTO — they're carried
 * forward by the repository from the caller/cache instead of round-tripped here.
 */
internal fun ProfileUserDto.toDomain() = UserProfile(
    name = name?.takeIf { it.isNotBlank() } ?: username,
    username = "@$username",
    avatarUrl = imageUrl?.toReachableMediaUrl(),
    statusEmoji = status,
    age = age,
    bio = description,
)
