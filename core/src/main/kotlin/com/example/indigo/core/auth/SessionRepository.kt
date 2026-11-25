package com.example.indigo.core.auth

import kotlinx.coroutines.flow.StateFlow

/**
 * HTTP errors throw ApiException; transport errors throw IOException. Only a refresh-access 401
 * destroys credentials. Other refresh errors preserve the session.
 */
interface SessionRepository {
    val authState: StateFlow<AuthState>

    /** Save the token pair before revoking a displaced refresh token, best effort. */
    suspend fun signIn(email: String, password: String)

    /**
     * Gate credential publication, revoke best effort, then destroy locally. Never stays signed in.
     */
    suspend fun signOut()

    /** Does NOT sign in: the server returns a web session token, not a mobile JWT pair. */
    suspend fun signUp(name: String, email: String, password: String)

    suspend fun requestPasswordReset(email: String)

    /** Used after sign-in returns "Email not verified"; does not modify stored credentials. */
    suspend fun sendVerificationEmail(email: String)

    /** Redeems the emailed token; does NOT sign in. */
    suspend fun resetPassword(token: String, newPassword: String)
}
