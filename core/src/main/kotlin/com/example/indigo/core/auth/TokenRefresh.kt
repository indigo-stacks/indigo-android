package com.example.indigo.core.auth

import com.example.indigo.core.network.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/** One independent flight per source: cancelling an HTTP caller cannot lose a server rotation. */
@Singleton
class TokenRefresh
@Inject
constructor(
    private val api: AuthApi,
    private val credentials: Credentials,
    private val json: Json,
) {
    private val lock = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var flight: Pair<Credentials.Snapshot, Deferred<AuthTokens?>>? = null
    // Keep a server rotation across a failed disk save: its source refresh token is spent.
    private var completed: Pair<Credentials.Snapshot, AuthTokens>? = null

    suspend fun refresh(failedAccess: String): AuthTokens? {
        val shared = lock.withLock {
            val source = credentials.snapshot()
            val tokens = source.tokens ?: return null
            if (tokens.accessToken != failedAccess) return tokens
            if (completed?.first != source) completed = null
            flight?.takeIf { it.first == source }?.second
                ?: scope
                    .async {
                        try {
                            val rotated =
                                lock.withLock { completed?.takeIf { it.first == source }?.second }
                                    ?: apiCall(json) {
                                            api.refresh(RefreshRequest(tokens.refreshToken))
                                        }
                                        .also {
                                            lock.withLock { completed = source to it }
                                        }
                            if (credentials.rotate(source, rotated)) rotated
                            else {
                                try {
                                    apiCall(json) {
                                        api.revoke(RefreshRequest(rotated.refreshToken))
                                    }
                                } catch (_: Exception) {}
                                null
                            }
                        } catch (error: ApiException) {
                            // Only refresh-access 401 is a verdict on the still-current
                            // credentials.
                            if (error.statusCode == 401) credentials.reject(source)
                            throw error
                        }
                    }
                    .also { flight = source to it }
        }
        try {
            return shared.await()
        } catch (error: Exception) {
            lock.withLock { if (flight?.second === shared && shared.isCompleted) flight = null }
            throw error
        }
    }
}
