package com.devsgroup.foodmanager.feature.auth.presentation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devsgroup.foodmanager.core.designsystem.component.FoodButton
import com.devsgroup.foodmanager.core.designsystem.component.FoodTextField
import com.devsgroup.foodmanager.core.designsystem.component.FoodTextFieldCheckStatus
import com.devsgroup.foodmanager.core.designsystem.theme.FoodManagerTheme

/**
 * "Создать аккаунт" — a required nickname plus a required email (confirmed by a mailed
 * code on the follow-up email-confirmation screen). This is the stateful entry point:
 * it wires [RegistrationViewModel] to the stateless [RegistrationContent] and hoists
 * navigation to the caller.
 *
 * @param onNavigateToLogin switch to the login screen ("Уже есть аккаунт? Войти").
 * @param onRegistered leave this screen once the account is created — the caller
 *   navigates to the email-confirmation screen from here, not into the app.
 */
@Composable
internal fun RegistrationScreen(
    onNavigateToLogin: () -> Unit,
    onRegistered: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegistrationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RegistrationContent(
        uiState = uiState,
        onUsernameChange = viewModel::onUsernameChange,
        onEmailChange = viewModel::onEmailChange,
        onCreateAccount = { viewModel.onCreateAccount(onRegistered) },
        onNavigateToLogin = onNavigateToLogin,
        modifier = modifier,
    )
}

@Composable
private fun RegistrationContent(
    uiState: RegistrationUiState,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onCreateAccount: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthScaffold(
        title = "Создать\nаккаунт",
        subtitle = "Никнейм — как вас увидят в семейной группе. Почту нужно подтвердить кодом из письма.",
        modifier = modifier,
    ) {
        FoodTextField(
            value = uiState.username,
            onValueChange = onUsernameChange,
            label = "НИКНЕЙМ",
            placeholder = "например, chef_maria",
            description = "Придумайте уникальный никнейм",
            isError = uiState.usernameError != null,
            errorMessage = uiState.usernameError,
            checkStatus = uiState.usernameAvailability.toCheckStatus(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
            ),
        )

        Spacer(Modifier.height(20.dp))

        FoodTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            label = "ПОЧТА",
            isRequired = true,
            placeholder = "you@example.com",
            description = "Понадобится, чтобы войти с нового устройства.",
            isError = uiState.emailError != null,
            errorMessage = uiState.emailError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
            ),
        )

        if (uiState.submitError != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = uiState.submitError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(28.dp))

        FoodButton(
            text = if (uiState.isLoading) "Создаём…" else "Создать аккаунт",
            onClick = onCreateAccount,
            enabled = uiState.canSubmit,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        AuthFooter(
            prompt = "Уже есть аккаунт?",
            action = "Войти",
            onActionClick = onNavigateToLogin,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun UsernameAvailability.toCheckStatus(): FoodTextFieldCheckStatus? = when (this) {
    UsernameAvailability.UNKNOWN -> null
    UsernameAvailability.CHECKING -> FoodTextFieldCheckStatus.Checking("Проверяем доступность...")
    UsernameAvailability.AVAILABLE -> FoodTextFieldCheckStatus.Available("Свободен — можно занять")
}

@Preview
@Composable
private fun RegistrationScreenPreview() {
    FoodManagerTheme {
        RegistrationContent(
            uiState = RegistrationUiState(username = "chef_maria"),
            onUsernameChange = {},
            onEmailChange = {},
            onCreateAccount = {},
            onNavigateToLogin = {},
        )
    }
}
