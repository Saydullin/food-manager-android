package com.devsgroup.foodmanager.feature.auth.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devsgroup.foodmanager.core.designsystem.component.FoodButton
import com.devsgroup.foodmanager.core.designsystem.component.FoodTextButton
import com.devsgroup.foodmanager.core.designsystem.component.FoodTextField
import com.devsgroup.foodmanager.core.designsystem.theme.FoodManagerTheme

/**
 * "С возвращением" — re-authenticating this device for a returning user: a nickname
 * is enough, since the actual proof is the signing key this device already holds
 * from a prior registration. When this device holds no key yet (fresh install / new
 * phone), [LoginUiState.needsEmailLogin] reveals a username+email code-confirmation
 * fallback instead of a dead end. This is the stateful entry point: it wires
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
        onEmailChange = viewModel::onEmailChange,
        onSendLoginCode = viewModel::onSendLoginCode,
        onCodeChange = viewModel::onCodeChange,
        onConfirmLoginCode = { viewModel.onConfirmLoginCode(onAuthenticated) },
        onResendLoginCode = viewModel::onResendLoginCode,
        onNavigateToRegistration = onNavigateToRegistration,
        modifier = modifier,
    )
}

@Composable
private fun LoginContent(
    uiState: LoginUiState,
    onUsernameChange: (String) -> Unit,
    onLogin: () -> Unit,
    onEmailChange: (String) -> Unit,
    onSendLoginCode: () -> Unit,
    onCodeChange: (String) -> Unit,
    onConfirmLoginCode: () -> Unit,
    onResendLoginCode: () -> Unit,
    onNavigateToRegistration: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthScaffold(
        title = "С возвращением",
        subtitle = when {
            uiState.codeSent -> "Введите код, который мы отправили на почту."
            uiState.needsEmailLogin ->
                "На этом устройстве ещё нет ключа входа. Введите почту, привязанную к аккаунту, — пришлём код для входа."
            else -> "Введите никнейм - подтвердим вход ключом этого устройства."
        },
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
                imeAction = if (uiState.needsEmailLogin) ImeAction.Next else ImeAction.Done,
            ),
        )

        AnimatedVisibility(visible = uiState.needsEmailLogin) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(20.dp))
                FoodTextField(
                    value = uiState.email,
                    onValueChange = onEmailChange,
                    label = "ПОЧТА",
                    placeholder = "you@example.com",
                    description = "Почта, привязанная к аккаунту".takeIf { uiState.emailError == null },
                    isError = uiState.emailError != null,
                    errorMessage = uiState.emailError,
                    enabled = !uiState.isSendingCode,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done,
                    ),
                )
            }
        }

        AnimatedVisibility(visible = uiState.codeSent) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(20.dp))
                FoodTextField(
                    value = uiState.code,
                    onValueChange = onCodeChange,
                    label = "КОД ИЗ ПИСЬМА",
                    placeholder = "123456",
                    description = "Введите 6-значный код".takeIf { uiState.codeError == null },
                    isError = uiState.codeError != null,
                    errorMessage = uiState.codeError,
                    enabled = !uiState.isConfirmingCode,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done,
                    ),
                )
            }
        }

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
            text = when {
                uiState.codeSent -> if (uiState.isConfirmingCode) "Подтверждаем…" else "Подтвердить"
                uiState.needsEmailLogin -> if (uiState.isSendingCode) "Отправляем…" else "Отправить код"
                uiState.isLoading -> "Входим…"
                else -> "Войти"
            },
            onClick = when {
                uiState.codeSent -> onConfirmLoginCode
                uiState.needsEmailLogin -> onSendLoginCode
                else -> onLogin
            },
            enabled = when {
                uiState.codeSent -> uiState.canConfirmCode
                uiState.needsEmailLogin -> uiState.canSendCode
                else -> uiState.canSubmit
            },
            modifier = Modifier.fillMaxWidth(),
        )

        AnimatedVisibility(visible = uiState.codeSent) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(12.dp))
                FoodTextButton(
                    text = "Отправить код ещё раз",
                    onClick = onResendLoginCode,
                    enabled = !uiState.isSendingCode,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }

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
            onEmailChange = {},
            onSendLoginCode = {},
            onCodeChange = {},
            onConfirmLoginCode = {},
            onResendLoginCode = {},
            onNavigateToRegistration = {},
        )
    }
}

@Preview
@Composable
private fun LoginScreenEmailFallbackPreview() {
    FoodManagerTheme {
        LoginContent(
            uiState = LoginUiState(
                username = "chef_maria",
                needsEmailLogin = true,
                email = "chef_maria@example.com",
                codeSent = true,
            ),
            onUsernameChange = {},
            onLogin = {},
            onEmailChange = {},
            onSendLoginCode = {},
            onCodeChange = {},
            onConfirmLoginCode = {},
            onResendLoginCode = {},
            onNavigateToRegistration = {},
        )
    }
}
