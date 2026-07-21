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
import com.devsgroup.foodmanager.core.designsystem.theme.FoodManagerTheme

/**
 * "С возвращением" — re-authenticating this device for a returning user: a nickname
 * is enough, since the actual proof is the signing key this device already holds
 * from a prior registration. This is the stateful entry point: it wires
 * [LoginViewModel] to the stateless [LoginContent] and hoists navigation to the caller.
 *
 * @param onNavigateToRegistration switch to registration ("Нет аккаунта? Регистрация").
 * @param onAuthenticated leave the auth flow into the app once login succeeds.
 */
@Composable
internal fun LoginScreen(
    onNavigateToRegistration: () -> Unit,
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LoginContent(
        uiState = uiState,
        onUsernameChange = viewModel::onUsernameChange,
        onLogin = { viewModel.onLogin(onAuthenticated) },
        onNavigateToRegistration = onNavigateToRegistration,
        modifier = modifier,
    )
}

@Composable
private fun LoginContent(
    uiState: LoginUiState,
    onUsernameChange: (String) -> Unit,
    onLogin: () -> Unit,
    onNavigateToRegistration: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthScaffold(
        title = "С возвращением",
        subtitle = "Введите никнейм — подтвердим вход ключом этого устройства.",
        titleStyle = MaterialTheme.typography.headlineLarge,
        modifier = modifier,
    ) {
        FoodTextField(
            value = uiState.username,
            onValueChange = onUsernameChange,
            label = "НИКНЕЙМ",
            placeholder = "ваш никнейм",
            isError = uiState.usernameError != null,
            errorMessage = uiState.usernameError,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                keyboardType = KeyboardType.Text,
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
            text = if (uiState.isLoading) "Входим…" else "Войти",
            onClick = onLogin,
            enabled = uiState.canSubmit,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        AuthFooter(
            prompt = "Нет аккаунта?",
            action = "Регистрация",
            onActionClick = onNavigateToRegistration,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview
@Composable
private fun LoginScreenPreview() {
    FoodManagerTheme {
        LoginContent(
            uiState = LoginUiState(username = "chef_maria"),
            onUsernameChange = {},
            onLogin = {},
            onNavigateToRegistration = {},
        )
    }
}
