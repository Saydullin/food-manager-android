package com.devsgroup.foodmanager.core.model

/** The signed-in user's own display profile, shown and edited from the Profile screen. */
data class UserProfile(
    val name: String,
    val username: String,
    val email: String? = null,
    val emailVerified: Boolean = false,
    val avatarUrl: String? = null,
    val statusEmoji: String? = null,
    val age: Int? = null,
    val cookingAttitude: CookingAttitude? = null,
    val bio: String? = null,
)

/** How the user relates to cooking — shown as the optional meta line under their name. */
enum class CookingAttitude {
    LOVE_COOKING,
    COOK_SOMETIMES,
    PREFER_ORDERING,
    LEARNING,
}
