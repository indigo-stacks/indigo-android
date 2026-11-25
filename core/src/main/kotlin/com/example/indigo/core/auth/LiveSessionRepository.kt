package com.example.indigo.core.auth

import com.example.indigo.core.network.AuthApi
import com.example.indigo.core.network.EmailRequest
import com.example.indigo.core.network.RefreshRequest
import com.example.indigo.core.network.ResetPasswordRequest
import com.example.indigo.core.network.SignUpRequest
import com.example.indigo.core.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Credentials as BasicCredentials

@Singleton
class LiveSessionRepository
@Inject
constructor(
    private val api: AuthApi,
    private val credentials: Credentials,
    private val json: Json,
) : SessionRepository {
    override val authState = credentials.state

    override suspend fun signIn(email: String, password: String) {
        val tokens =
            apiCall(json) { api.signIn(BasicCredentials.basic(email, password, Charsets.UTF_8)) }
        withContext(NonCancellable) {
            val displaced = credentials.signIn(tokens)
            if (displaced != null && displaced.refreshToken != tokens.refreshToken)
                revoke(displaced.refreshToken)
        }
    }

    override suspend fun signOut(): Unit =
        withContext<Unit>(NonCancellable) {
            val source = credentials.beginSignOut()
            try {
                source.tokens?.let { revoke(it.refreshToken) }
            } finally {
                credentials.endSignOut(source)
            }
        }

    private suspend fun revoke(token: String) {
        try {
            apiCall(json) { api.revoke(RefreshRequest(token)) }
        } catch (_: Exception) {
            /* Best effort; never retain local credentials for a network failure. */
        }
    }

    override suspend fun signUp(name: String, email: String, password: String) =
        apiCall(json) { api.signUp(SignUpRequest(name, email, password)) }

    override suspend fun requestPasswordReset(email: String) =
        apiCall(json) { api.forgotPassword(EmailRequest(email)) }

    override suspend fun sendVerificationEmail(email: String) =
        apiCall(json) { api.sendVerification(EmailRequest(email)) }

    override suspend fun resetPassword(token: String, newPassword: String) =
        apiCall(json) { api.resetPassword(ResetPasswordRequest(token, newPassword)) }
}
