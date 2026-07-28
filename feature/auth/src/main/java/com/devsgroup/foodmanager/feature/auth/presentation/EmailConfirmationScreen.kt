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
 * "Подтвердите почту" — the hard gate between having an account and using the app.
 * Reached right after registration, and re-entered on every later launch until the
 * mailed code is confirmed (see the app-level start-destination gate in `:app`).
 * Lets the user resend the code, correct a mistyped address inline, or bail out via
 * logout if they can't access this inbox at all. This is the stateful entry point:
 * it wires [EmailConfirmationViewModel] to the stateless [EmailConfirmationContent].
 *
 * @param onConfirmed leave the auth flow into the app once the code is confirmed.
 * @param onLoggedOut leave the auth flow back to registration after the escape-hatch logout.
 */
@Composable
internal fun EmailConfirmationScreen(
    onConfirmed: () -> Unit,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EmailConfirmationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EmailConfirmationContent(
        uiState = uiState,
        onCodeChange = viewModel::onCodeChange,
        onConfirm = { viewModel.onConfirm(onConfirmed) },
        onResendCode = viewModel::onResendCode,
        onToggleChangeEmail = viewModel::onToggleChangeEmail,
        onNewEmailChange = viewModel::onNewEmailChange,
        onSubmitNewEmail = viewModel::onSubmitNewEmail,
        onLogout = { viewModel.onLogout(onLoggedOut) },
        modifier = modifier,
    )
}

@Composable
private fun EmailConfirmationContent(
    uiState: EmailConfirmationUiState,
    onCodeChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onResendCode: () -> Unit,
    onToggleChangeEmail: () -> Unit,
    onNewEmailChange: (String) -> Unit,
    onSubmitNewEmail: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthScaffold(
        title = "Подтвердите\nпочту",
        subtitle = "Мы отправили код на ${uiState.email}. Введите его, чтобы подтвердить почту.",
        modifier = modifier,
    ) {
        FoodTextField(
            value = uiState.code,
            onValueChange = onCodeChange,
            label = "КОД ИЗ ПИСЬМА",
            placeholder = "1234",
            description = "Введите 4-значный код".takeIf { uiState.codeError == null },
            isError = uiState.codeError != null,
            errorMessage = uiState.codeError,
            enabled = !uiState.isConfirming,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done,
            ),
        )

        if (uiState.resendMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = uiState.resendMessage,
                style = MaterialTheme.typography.bodySmall,
                color = if (uiState.codeError != null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        }

        Spacer(Modifier.height(28.dp))

        FoodButton(
            text = if (uiState.isConfirming) "Подтверждаем…" else "Подтвердить",
            onClick = onConfirm,
            enabled = uiState.canConfirm,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        FoodTextButton(
            text = "Отправить код ещё раз",
            compact = true,
            onClick = onResendCode,
            enabled = !uiState.isResending,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(Modifier.height(4.dp))

        FoodTextButton(
            text = if (uiState.isChangingEmail) "Отменить" else "Изменить почту?",
            onClick = onToggleChangeEmail,
            compact = true,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        AnimatedVisibility(visible = uiState.isChangingEmail) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(12.dp))
                FoodTextField(
                    value = uiState.newEmail,
                    onValueChange = onNewEmailChange,
                    label = "НОВАЯ ПОЧТА",
                    placeholder = "you@example.com",
                    isError = uiState.newEmailError != null,
                    errorMessage = uiState.newEmailError,
                    enabled = !uiState.isSubmittingNewEmail,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done,
                    ),
                )
                Spacer(Modifier.height(12.dp))
                FoodButton(
                    text = if (uiState.isSubmittingNewEmail) "Сохраняем…" else "Сохранить",
                    onClick = onSubmitNewEmail,
                    enabled = uiState.canSubmitNewEmail,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        FoodTextButton(
            text = "Не вы? Выйти",
            onClick = onLogout,
            compact = true,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Preview
@Composable
private fun EmailConfirmationScreenPreview() {
    FoodManagerTheme {
        EmailConfirmationContent(
            uiState = EmailConfirmationUiState(email = "chef_maria@example.com"),
            onCodeChange = {},
            onConfirm = {},
            onResendCode = {},
            onToggleChangeEmail = {},
            onNewEmailChange = {},
            onSubmitNewEmail = {},
            onLogout = {},
        )
    }
}

@Preview
@Composable
private fun EmailConfirmationScreenChangeEmailPreview() {
    FoodManagerTheme {
        EmailConfirmationContent(
            uiState = EmailConfirmationUiState(
                email = "chef_maria@example.com",
                isChangingEmail = true,
            ),
            onCodeChange = {},
            onConfirm = {},
            onResendCode = {},
            onToggleChangeEmail = {},
            onNewEmailChange = {},
            onSubmitNewEmail = {},
            onLogout = {},
        )
    }
}
