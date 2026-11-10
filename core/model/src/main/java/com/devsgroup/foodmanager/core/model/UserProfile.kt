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
    /**
     * Whether this account has answered (or declined) the onboarding questions. Owned by
     * the server so it holds across logout/login and across devices — a device-local flag
     * would re-ask on every sign-in and every reinstall.
     */
    val onboardingCompleted: Boolean = false,
    val foodPreferences: List<String> = emptyList(),
    val foodExceptions: List<String> = emptyList(),
    val diets: List<String> = emptyList(),
    /** The account owner as an eater at the dinner table (the "Я" in the cook-dinner flow). */
    val sex: Sex? = null,
    val medicalRestrictions: Set<MedicalRestriction> = emptySet(),
)

/** How the user relates to cooking — shown as the optional meta line under their name. */
enum class CookingAttitude {
    LOVE_COOKING,
    COOK_SOMETIMES,
    PREFER_ORDERING,
    LEARNING,
}
