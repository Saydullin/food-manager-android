package com.devsgroup.foodmanager.feature.profile.presentation

/**
 * Picks the Russian plural form matching [count] and prefixes it — e.g.
 * `pluralize(3, "участник", "участника", "участников")` → "3 участника".
 */
internal fun pluralize(count: Int, one: String, few: String, many: String): String {
    val mod100 = count % 100
    val mod10 = count % 10
    val word = when {
        mod100 in 11..14 -> many
        mod10 == 1 -> one
        mod10 in 2..4 -> few
        else -> many
    }
    return "$count $word"
}
