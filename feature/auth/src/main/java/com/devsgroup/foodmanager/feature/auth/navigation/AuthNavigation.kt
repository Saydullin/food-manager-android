package com.devsgroup.foodmanager.feature.auth.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.devsgroup.foodmanager.feature.auth.presentation.EmailConfirmationScreen
import com.devsgroup.foodmanager.feature.auth.presentation.LoginScreen
import com.devsgroup.foodmanager.feature.auth.presentation.RegistrationScreen
import kotlinx.serialization.Serializable

/** Create-account destination ("Создать аккаунт"). */
@Serializable
data object RegistrationRoute

/** Returning-user login destination ("С возвращением"). */
@Serializable
data object LoginRoute

/**
 * Email-verification gate ("Подтвердите почту") — reached right after registration,
 * and, via the app-level start-destination gate, on every later launch until the
 * account's email is confirmed.
 */
@Serializable
data object EmailConfirmationRoute

fun NavController.navigateToRegistration(navOptions: NavOptions? = null) =
    navigate(RegistrationRoute, navOptions)

fun NavController.navigateToLogin(navOptions: NavOptions? = null) =
    navigate(LoginRoute, navOptions)

fun NavController.navigateToEmailConfirmation(navOptions: NavOptions? = null) =
    navigate(EmailConfirmationRoute, navOptions)

/**
 * Registers all auth screens.
 *
 * @param onAuthenticated invoked once the user has a verified, active session — the
 *   host clears the auth back stack and lands them in the app. Only reachable from
 *   [LoginScreen] (which can only succeed for an already-verified account) and
 *   [EmailConfirmationScreen] right after a successful code confirmation.
 * @param onNavigateToLogin registration → login.
 * @param onNavigateToRegistration login → registration, and the email-confirmation
 *   screen's logout escape hatch → registration.
 * @param onNavigateToEmailConfirmation registration → email confirmation, once the
 *   account is created but its email is still unverified.
 */
fun NavGraphBuilder.authScreens(
    onAuthenticated: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToRegistration: () -> Unit,
    onNavigateToEmailConfirmation: () -> Unit,
) {
    composable<RegistrationRoute> {
        RegistrationScreen(
            onNavigateToLogin = onNavigateToLogin,
            onRegistered = onNavigateToEmailConfirmation,
        )
    }
    composable<LoginRoute> {
        LoginScreen(
            onNavigateToRegistration = onNavigateToRegistration,
            onAuthenticated = onAuthenticated,
        )
    }
    composable<EmailConfirmationRoute> {
        EmailConfirmationScreen(
            onConfirmed = onAuthenticated,
            onLoggedOut = onNavigateToRegistration,
        )
    }
}
