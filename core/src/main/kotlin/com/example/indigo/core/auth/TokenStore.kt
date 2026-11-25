package com.example.indigo.core.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable data class AuthTokens(val accessToken: String, val refreshToken: String)

interface TokenStore {
    val tokens: Flow<AuthTokens?>

    suspend fun save(tokens: AuthTokens)

    suspend fun clear()
}
