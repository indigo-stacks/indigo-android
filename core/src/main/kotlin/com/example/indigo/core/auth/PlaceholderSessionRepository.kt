package com.example.indigo.core.auth

import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PlaceholderSessionRepository @Inject constructor() : SessionRepository {
    override val authState: StateFlow<AuthState> = MutableStateFlow(AuthState.SignedOut)

    override suspend fun signIn(email: String, password: String) {
        throw UnsupportedOperationException("Not implemented yet")
    }

    override suspend fun signOut() {
        throw UnsupportedOperationException("Not implemented yet")
    }

    override suspend fun signUp(name: String, email: String, password: String) {
        throw UnsupportedOperationException("Not implemented yet")
    }

    override suspend fun requestPasswordReset(email: String) {
        throw UnsupportedOperationException("Not implemented yet")
    }

    override suspend fun sendVerificationEmail(email: String) {
        throw UnsupportedOperationException("Not implemented yet")
    }

    override suspend fun resetPassword(token: String, newPassword: String) {
        throw UnsupportedOperationException("Not implemented yet")
    }
}
