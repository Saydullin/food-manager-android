package com.devsgroup.foodmanager.core.model

/**
 * Someone the user cooks for — themselves, a partner, a child, a grandparent. Unlike a
 * [GroupMember] this is not an account: nobody gets invited, the user simply describes
 * the people at their table so a dish can be picked that suits all of them.
 *
 * The self eater ([isSelf], id [SELF_ID]) is the account profile seen as an eater: its
 * sex/age/doctor's restrictions are [UserProfile] fields, and its allergies/diets/likes
 * are the profile's taste lists — matching how the server models "me". Edits to it go
 * to the profile, never to a household row.
 */
data class Eater(
    val id: String,
    val name: String,
    val isSelf: Boolean = false,
    /** Optional — together with [age] it's what calorie norms are calculated from. */
    val sex: Sex? = null,
    /** Exact years rather than an age band: calorie norms and child portions need the number. */
    val age: Int? = null,
    /** Cuisine codes the person enjoys — a ranking signal only, never a filter. */
    val likes: Set<String> = emptySet(),
    /** Tag/cuisine codes the person won't eat (e.g. `SPICY`). Treated as a hard exclusion. */
    val avoid: Set<String> = emptySet(),
    val allergens: Set<Allergen> = emptySet(),
    val diets: Set<DietaryTag> = emptySet(),
    val medical: Set<MedicalRestriction> = emptySet(),
) {
    companion object {
        /** The self eater's id. Never sent to the server, which models "me" as `includeSelf`. */
        const val SELF_ID = "self"
    }
}

enum class Sex { MALE, FEMALE }

/**
 * Doctor's recommendations, as fixed codes rather than free text: a dish can only be
 * filtered against something structured. Mirrors the server enum; how each maps onto
 * dish data is decided server-side (see the server's `recommendation/dinner.ts`).
 */
enum class MedicalRestriction {
    LOW_CALORIE,
    LOW_FAT,
    LOW_CARB,
    NO_SPICY,
    LOW_SALT,
    LOW_SUGAR,
}
