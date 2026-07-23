package com.devsgroup.foodmanager.feature.profile.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.devsgroup.foodmanager.feature.profile.presentation.EditProfileScreen
import com.devsgroup.foodmanager.feature.profile.presentation.GroupDetailScreen
import com.devsgroup.foodmanager.feature.profile.presentation.ProfileScreen
import kotlinx.serialization.Serializable

@Serializable
data object ProfileRoute

/** Full-screen push from the profile header's edit (pencil) button. */
@Serializable
data object EditProfileRoute

/** Full-screen push from tapping a group row in [ProfileRoute]. */
@Serializable
data class GroupDetailRoute(val groupId: String)

fun NavController.navigateToProfile(navOptions: NavOptions? = null) =
    navigate(ProfileRoute, navOptions)

fun NavController.navigateToEditProfile(navOptions: NavOptions? = null) =
    navigate(EditProfileRoute, navOptions)

fun NavController.navigateToGroupDetail(groupId: String, navOptions: NavOptions? = null) =
    navigate(GroupDetailRoute(groupId), navOptions)

/**
 * Registers the profile screen and its two nested pushes (edit profile, group detail).
 *
 * @param onNavigateToEditProfile the header's edit button → push [EditProfileRoute].
 * @param onNavigateToGroupDetail a group row tap → push [GroupDetailRoute].
 * @param onLoggedOut invoked once the user signs out — the host clears the back stack
 *   and returns them to the auth flow.
 */
fun NavGraphBuilder.profileScreen(
    onNavigateToEditProfile: () -> Unit,
    onNavigateToGroupDetail: (groupId: String) -> Unit,
    onLoggedOut: () -> Unit,
) {
    composable<ProfileRoute> {
        ProfileScreen(
            onNavigateToEditProfile = onNavigateToEditProfile,
            onNavigateToGroupDetail = onNavigateToGroupDetail,
            onLoggedOut = onLoggedOut,
        )
    }
}

fun NavGraphBuilder.editProfileScreen(onBack: () -> Unit) {
    composable<EditProfileRoute> {
        EditProfileScreen(onBack = onBack)
    }
}

fun NavGraphBuilder.groupDetailScreen(onBack: () -> Unit) {
    composable<GroupDetailRoute> {
        GroupDetailScreen(onBack = onBack)
    }
}
