package com.example.indigo.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.indigo.core.auth.SessionRepository
import com.example.indigo.core.network.ApiException
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AuthMode {
    SignIn,
    SignUp,
    ForgotPassword,
}

data class AuthState(
    val mode: AuthMode = AuthMode.SignIn,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val failed: Boolean = false,
    val errorMessage: String? = null,
    val signUpSuccess: Boolean = false,
    val resetEmailSent: Boolean = false,
    val unverifiedEmail: String? = null,
    val verificationEmailSent: Boolean = false,
) {
    val canSubmit: Boolean
        get() =
            !isLoading &&
                email.isNotEmpty() &&
                (mode == AuthMode.ForgotPassword || password.isNotEmpty()) &&
                (mode != AuthMode.SignUp || name.isNotEmpty())
}

sealed interface AuthAction {
    data class NameChanged(val value: String) : AuthAction

    data class EmailChanged(val value: String) : AuthAction

    data class PasswordChanged(val value: String) : AuthAction

    data class ModeChanged(val mode: AuthMode) : AuthAction

    data object Submit : AuthAction

    data object ResendVerification : AuthAction
}

@HiltViewModel
class AuthViewModel @Inject constructor(private val session: SessionRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(AuthState())
    val state = mutableState.asStateFlow()

    fun send(action: AuthAction) {
        val current = state.value
        if (current.isLoading) return
        when (action) {
            is AuthAction.NameChanged -> mutableState.value = current.copy(name = action.value)
            is AuthAction.PasswordChanged ->
                mutableState.value = current.copy(password = action.value)
            is AuthAction.EmailChanged ->
                mutableState.value =
                    current.copy(
                        email = action.value,
                        unverifiedEmail = null,
                        verificationEmailSent = false,
                    )
            is AuthAction.ModeChanged ->
                mutableState.value =
                    current.copy(
                        mode = action.mode,
                        failed = false,
                        errorMessage = null,
                        signUpSuccess = false,
                        unverifiedEmail = null,
                        verificationEmailSent = false,
                        resetEmailSent = false,
                    )
            AuthAction.Submit -> if (current.canSubmit) request(current, false)
            AuthAction.ResendVerification ->
                if (current.unverifiedEmail != null && !current.verificationEmailSent)
                    request(current, true)
        }
    }

    private fun request(submitted: AuthState, resend: Boolean) {
        mutableState.value =
            submitted.copy(
                isLoading = true,
                failed = false,
                errorMessage = null,
                unverifiedEmail = if (resend) submitted.unverifiedEmail else null,
                verificationEmailSent = false,
            )
        viewModelScope.launch {
            try {
                if (resend) session.sendVerificationEmail(requireNotNull(submitted.unverifiedEmail))
                else
                    when (submitted.mode) {
                        AuthMode.SignIn -> session.signIn(submitted.email, submitted.password)
                        AuthMode.SignUp ->
                            session.signUp(submitted.name, submitted.email, submitted.password)
                        AuthMode.ForgotPassword -> session.requestPasswordReset(submitted.email)
                    }
                val current = state.value
                mutableState.value =
                    when {
                        resend -> current.copy(isLoading = false, verificationEmailSent = true)
                        submitted.mode == AuthMode.SignUp ->
                            current.copy(
                                isLoading = false,
                                mode = AuthMode.SignIn,
                                password = "",
                                signUpSuccess = true,
                            )
                        submitted.mode == AuthMode.ForgotPassword ->
                            current.copy(isLoading = false, resetEmailSent = true)
                        else -> current.copy(isLoading = false)
                    }
            } catch (error: IOException) {
                val api = error as? ApiException
                mutableState.value =
                    state.value.copy(
                        isLoading = false,
                        failed = true,
                        errorMessage = api?.body?.message,
                        unverifiedEmail =
                            if (
                                !resend &&
                                    submitted.mode == AuthMode.SignIn &&
                                    api?.statusCode == 401 &&
                                    api.body?.message == "Email not verified"
                            )
                                submitted.email
                            else state.value.unverifiedEmail,
                    )
            }
        }
    }
}
