package com.example.indigo.feature.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object AuthRoute : NavKey

@Composable
fun AuthScreen(onBack: () -> Unit = {}, viewModel: AuthViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AuthContent(state, viewModel::send, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthContent(state: AuthState, onAction: (AuthAction) -> Unit, onBack: () -> Unit = {}) {
    val reset = state.mode == AuthMode.ForgotPassword
    BackHandler(enabled = reset && !state.isLoading) {
        onAction(AuthAction.ModeChanged(AuthMode.SignIn))
    }
    val focus = LocalFocusManager.current
    val submit = {
        focus.clearFocus()
        onAction(AuthAction.Submit)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (reset) R.string.reset_password
                            else if (state.mode == AuthMode.SignUp) R.string.sign_up
                            else R.string.sign_in
                        )
                    )
                },
                navigationIcon = {
                    TextButton(
                        onClick = {
                            if (reset) onAction(AuthAction.ModeChanged(AuthMode.SignIn))
                            else onBack()
                        },
                        enabled = !state.isLoading,
                    ) {
                        Text(stringResource(R.string.back))
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
                            onClick = { onAction(AuthAction.ModeChanged(mode)) },
                            enabled = !state.isLoading,
                            label = {
                                Text(
                                    stringResource(
                                        if (mode == AuthMode.SignIn) R.string.sign_in
                                        else R.string.sign_up
                                    )
                                )
                            },
                        )
                    }
                }
            if (state.signUpSuccess) Text(stringResource(R.string.sign_up_success))
            if (reset && state.resetEmailSent) {
                Text(stringResource(R.string.reset_sent))
                Button(onClick = { onAction(AuthAction.ModeChanged(AuthMode.SignIn)) }) {
                    Text(stringResource(R.string.done))
                }
            } else {
                if (reset) Text(stringResource(R.string.reset_help))
                if (state.mode == AuthMode.SignUp)
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { onAction(AuthAction.NameChanged(it)) },
                        label = { Text(stringResource(R.string.name)) },
                        enabled = !state.isLoading,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions =
                            KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next,
                            ),
                        keyboardActions =
                            KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                    )
                OutlinedTextField(
                    value = state.email,
                    onValueChange = { onAction(AuthAction.EmailChanged(it)) },
                    label = { Text(stringResource(R.string.email)) },
                    enabled = !state.isLoading,
                    singleLine = true,
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
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = { onAction(AuthAction.PasswordChanged(it)) },
                        label = { Text(stringResource(R.string.password)) },
                        enabled = !state.isLoading,
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
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
                        onClick = { onAction(AuthAction.ModeChanged(AuthMode.ForgotPassword)) },
                        enabled = !state.isLoading,
                    ) {
                        Text(stringResource(R.string.forgot_password))
                    }
                Button(
                    onClick = submit,
                    enabled = state.canSubmit,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            if (state.isLoading) R.string.working
                            else if (reset) R.string.send_reset
                            else if (state.mode == AuthMode.SignUp) R.string.sign_up
                            else R.string.sign_in
                        )
                    )
                }
                if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (state.failed)
                    Text(
                        state.errorMessage ?: stringResource(R.string.generic_error),
                        color = MaterialTheme.colorScheme.error,
                    )
                if (state.unverifiedEmail != null && !state.verificationEmailSent)
                    TextButton(
                        onClick = { onAction(AuthAction.ResendVerification) },
                        enabled = !state.isLoading,
                    ) {
                        Text(stringResource(R.string.resend))
                    }
                if (state.verificationEmailSent) Text(stringResource(R.string.verification_sent))
            }
        }
    }
}

@Preview
@Composable
private fun AuthPreview() {
    MaterialTheme { AuthContent(AuthState(), {}) }
}
