package com.devsgroup.foodmanager.feature.auth.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.devsgroup.foodmanager.feature.auth.presentation.LoginScreen
import com.devsgroup.foodmanager.feature.auth.presentation.RegistrationScreen
import kotlinx.serialization.Serializable

/** Create-account destination ("Создать аккаунт"). */
@Serializable
data object RegistrationRoute

/** Returning-user login destination ("С возвращением"). */
@Serializable
data object LoginRoute

fun NavController.navigateToRegistration(navOptions: NavOptions? = null) =
    navigate(RegistrationRoute, navOptions)

fun NavController.navigateToLogin(navOptions: NavOptions? = null) =
    navigate(LoginRoute, navOptions)

/**
 * Registers both auth screens.
 *
 * @param onAuthenticated invoked once the user has an active session — the host clears
 *   the auth back stack and lands them in the app.
 * @param onNavigateToLogin registration → login.
 * @param onNavigateToRegistration login → registration.
 */
fun NavGraphBuilder.authScreens(
    onAuthenticated: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToRegistration: () -> Unit,
) {
    composable<RegistrationRoute> {
        RegistrationScreen(
            onNavigateToLogin = onNavigateToLogin,
            onRegistered = onAuthenticated,
        )
    }
    composable<LoginRoute> {
        LoginScreen(
            onNavigateToRegistration = onNavigateToRegistration,
            onAuthenticated = onAuthenticated,
        )
    }
}
