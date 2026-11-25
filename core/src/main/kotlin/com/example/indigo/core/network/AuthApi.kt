package com.example.indigo.core.network

import com.example.indigo.core.auth.AuthTokens
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface AuthApi {
    @POST("api/v1/auth/sign-in")
    @Headers("Content-Type: application/json")
    suspend fun signIn(@Header("Authorization") basic: String): AuthTokens

    @POST("api/v1/auth/sign-up") suspend fun signUp(@Body body: SignUpRequest)

    @POST("api/v1/auth/forgot-password") suspend fun forgotPassword(@Body body: EmailRequest)

    @POST("api/v1/auth/send-verification-email")
    suspend fun sendVerification(@Body body: EmailRequest)

    @POST("api/v1/auth/reset-password") suspend fun resetPassword(@Body body: ResetPasswordRequest)

    @POST("api/v1/auth/refresh-access") suspend fun refresh(@Body body: RefreshRequest): AuthTokens

    @POST("api/v1/auth/revoke-access") suspend fun revoke(@Body body: RefreshRequest)
}

@Serializable data class SignUpRequest(val name: String, val email: String, val password: String)

@Serializable data class EmailRequest(val email: String)

@Serializable data class ResetPasswordRequest(val token: String, val newPassword: String)

@Serializable data class RefreshRequest(val refreshToken: String)
