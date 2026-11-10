package com.devsgroup.foodmanager.core.data.mapper

import com.devsgroup.foodmanager.core.model.MedicalRestriction
import com.devsgroup.foodmanager.core.model.Sex
import com.devsgroup.foodmanager.core.model.UserProfile
import com.devsgroup.foodmanager.core.network.profile.model.ProfileUserDto
import com.devsgroup.foodmanager.core.network.toReachableMediaUrl

/**
 * Maps profile wire models (`:core:network`) to domain models (`:core:model`).
 * `cookingAttitude` has no representation in this DTO (it's a purely client-side
 * preference) — the repository carries it forward from the caller/cache instead.
 */
internal fun ProfileUserDto.toDomain() = UserProfile(
    name = name?.takeIf { it.isNotBlank() } ?: username,
    username = "@$username",
    email = email,
    emailVerified = emailVerified,
    avatarUrl = imageUrl?.toReachableMediaUrl(),
    statusEmoji = status,
    age = age,
    bio = description,
    onboardingCompleted = onboardingCompleted,
    foodPreferences = foodPreferences,
    foodExceptions = foodExceptions,
    diets = diets,
    sex = sex.toKnownEnumOrNull<Sex>(),
    medicalRestrictions = medicalRestrictions.toKnownEnumSet<MedicalRestriction>(),
)
