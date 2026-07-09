package com.devsgroup.foodmanager.navigation

import com.devsgroup.foodmanager.feature.discover.navigation.DiscoverRoute
import com.devsgroup.foodmanager.feature.favorites.navigation.FavoritesRoute
import com.devsgroup.foodmanager.feature.profile.navigation.ProfileRoute
import com.devsgroup.foodmanager.feature.recommendations.navigation.RecommendationsRoute

/**
 * The destinations reachable from the bottom navigation bar. Each maps to a
 * feature module's type-safe route. (Emoji stand in for icons so the skeleton
 * carries no icon dependency — swap for Material Symbols / vector assets later.)
 */
enum class TopLevelDestination(
    val label: String,
    val emoji: String,
    val route: Any,
) {
    DISCOVER("Discover", "🍽️", DiscoverRoute),
    RECOMMENDATIONS("For You", "✨", RecommendationsRoute),
    FAVORITES("Favorites", "❤️", FavoritesRoute),
    PROFILE("Profile", "👤", ProfileRoute),
}
