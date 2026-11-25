package com.example.indigo.testing

import com.example.indigo.core.auth.*
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSessionRepository : SessionRepository {
    override val authState = MutableStateFlow<AuthState>(AuthState.SignedOut)
    var onSignIn: suspend (String, String) -> Unit = { _, _ ->
        authState.value = AuthState.SignedIn("test-user")
    }
    var onSignOut: suspend () -> Unit = { authState.value = AuthState.SignedOut }
    var onSignUp: suspend (String, String, String) -> Unit = { _, _, _ -> }
    var onRequestPasswordReset: suspend (String) -> Unit = {}
    var onSendVerificationEmail: suspend (String) -> Unit = {}
    var onResetPassword: suspend (String, String) -> Unit = { _, _ -> }

    override suspend fun signIn(email: String, password: String) = onSignIn(email, password)

    override suspend fun signOut() = onSignOut()

    override suspend fun signUp(name: String, email: String, password: String) =
        onSignUp(name, email, password)

    override suspend fun requestPasswordReset(email: String) = onRequestPasswordReset(email)

    override suspend fun sendVerificationEmail(email: String) = onSendVerificationEmail(email)

    override suspend fun resetPassword(token: String, newPassword: String) =
        onResetPassword(token, newPassword)
}
