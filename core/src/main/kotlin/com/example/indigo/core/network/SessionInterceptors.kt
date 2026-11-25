package com.example.indigo.core.network

import com.example.indigo.core.auth.Credentials
import com.example.indigo.core.auth.TokenRefresh
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import okhttp3.*

class BearerInterceptor @Inject constructor(private val credentials: Credentials) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val tokens = runBlocking { credentials.snapshot().tokens }
        val request = chain.request().newBuilder()
        tokens?.let { request.header("Authorization", "Bearer ${it.accessToken}") }
        return chain.proceed(request.build())
    }
}

class TokenAuthenticator @Inject constructor(private val refresh: TokenRefresh) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponse != null) return null
        val failed =
            response.request.header("Authorization")?.removePrefix("Bearer ") ?: return null
        val tokens = runBlocking { refresh.refresh(failed) } ?: return null
        return response.request
            .newBuilder()
            .header("Authorization", "Bearer ${tokens.accessToken}")
            .build()
    }
}
