package com.example.indigo.feature.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.example.indigo.components.ErrorText
import com.example.indigo.components.IndigoTextField
import com.example.indigo.components.IndigoTheme
import com.example.indigo.components.PasswordField
import com.example.indigo.components.PrimaryButton
import kotlinx.serialization.Serializable

@Serializable data object AuthRoute : NavKey

@Composable
fun AuthScreen(onBack: () -> Unit = {}, viewModel: AuthFormViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AuthContent(state, viewModel::send, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthContent(state: AuthFormState, onAction: (AuthFormAction) -> Unit, onBack: () -> Unit = {}) {
    val reset = state.mode == AuthMode.ForgotPassword
    BackHandler(enabled = reset && !state.isLoading) {
        onAction(AuthFormAction.ModeChanged(AuthMode.SignIn))
    }
    val focus = LocalFocusManager.current
    val submit = {
        focus.clearFocus()
        onAction(AuthFormAction.Submit)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (reset) R.string.auth_reset_password
                            else if (state.mode == AuthMode.SignUp) R.string.auth_sign_up
                            else R.string.auth_sign_in
                        )
                    )
                },
                navigationIcon = {
                    TextButton(
                        onClick = {
                            if (reset) onAction(AuthFormAction.ModeChanged(AuthMode.SignIn))
                            else onBack()
                        },
                        enabled = !state.isLoading,
                    ) {
                        Text(stringResource(R.string.auth_back))
                    }
                },
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!reset)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(AuthMode.SignIn, AuthMode.SignUp).forEach { mode ->
                        FilterChip(
                            selected = state.mode == mode,
                            onClick = { onAction(AuthFormAction.ModeChanged(mode)) },
                            enabled = !state.isLoading,
                            label = {
                                Text(
                                    stringResource(
                                        if (mode == AuthMode.SignIn) R.string.auth_sign_in
                                        else R.string.auth_sign_up
                                    )
                                )
                            },
                        )
                    }
                }
            if (state.signUpSuccess) Text(stringResource(R.string.auth_sign_up_success))
            if (reset && state.resetEmailSent) {
                Text(stringResource(R.string.auth_reset_sent))
                Button(onClick = { onAction(AuthFormAction.ModeChanged(AuthMode.SignIn)) }) {
                    Text(stringResource(R.string.auth_done))
                }
            } else {
                if (reset) Text(stringResource(R.string.auth_reset_help))
                if (state.mode == AuthMode.SignUp)
                    IndigoTextField(
                        value = state.name,
                        onValueChange = { onAction(AuthFormAction.NameChanged(it)) },
                        label = stringResource(R.string.auth_name),
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions =
                            KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next,
                            ),
                        keyboardActions =
                            KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                    )
                IndigoTextField(
                    value = state.email,
                    onValueChange = { onAction(AuthFormAction.EmailChanged(it)) },
                    label = stringResource(R.string.auth_email),
                    enabled = !state.isLoading,
                    modifier =
                        Modifier.fillMaxWidth().semantics {
                            contentType = ContentType.EmailAddress
                        },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            autoCorrectEnabled = false,
                            imeAction = if (reset) ImeAction.Go else ImeAction.Next,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onNext = { focus.moveFocus(FocusDirection.Down) },
                            onGo = { submit() },
                        ),
                )
                if (!reset)
                    PasswordField(
                        value = state.password,
                        onValueChange = { onAction(AuthFormAction.PasswordChanged(it)) },
                        label = stringResource(R.string.auth_password),
                        enabled = !state.isLoading,
                        modifier =
                            Modifier.fillMaxWidth().semantics {
                                contentType =
                                    if (state.mode == AuthMode.SignUp) ContentType.NewPassword
                                    else ContentType.Password
                            },
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Go,
                            ),
                        keyboardActions = KeyboardActions(onGo = { submit() }),
                    )
                if (state.mode == AuthMode.SignIn)
                    TextButton(
                        onClick = { onAction(AuthFormAction.ModeChanged(AuthMode.ForgotPassword)) },
                        enabled = !state.isLoading,
                    ) {
                        Text(stringResource(R.string.auth_forgot_password))
                    }
                PrimaryButton(
                    text =
                        stringResource(
                            if (state.isLoading) R.string.auth_working
                            else if (reset) R.string.auth_send_reset
                            else if (state.mode == AuthMode.SignUp) R.string.auth_sign_up
                            else R.string.auth_sign_in
                        ),
                    onClick = submit,
                    enabled = state.canSubmit,
                    loading = state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.failed)
                    ErrorText(state.errorMessage ?: stringResource(R.string.auth_generic_error))
                if (state.unverifiedEmail != null && !state.verificationEmailSent)
                    TextButton(
                        onClick = { onAction(AuthFormAction.ResendVerification) },
                        enabled = !state.isLoading,
                    ) {
                        Text(stringResource(R.string.auth_resend))
                    }
                if (state.verificationEmailSent)
                    Text(stringResource(R.string.auth_verification_sent))
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AuthPreview() {
    IndigoTheme(dynamicColor = false) { AuthContent(AuthFormState(), {}) }
}
