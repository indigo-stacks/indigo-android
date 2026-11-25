package com.example.indigo.core.auth

import com.example.indigo.core.data.UserScopedCache
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Serializes persistent writes and the sign-out gate; network work never holds this lock. */
@Singleton
class Credentials
@Inject
constructor(
    private val store: TokenStore,
    private val cache: UserScopedCache,
    private val json: Json,
) {
    private val lock = Mutex()
    private val ready = CompletableDeferred<Unit>()
    private val mutableState = MutableStateFlow<AuthState>(AuthState.Loading)
    val state = mutableState.asStateFlow()
    private var current: AuthTokens? = null
    private var signingOut = false
    // A generation also rejects a late refresh after destroy, even if a new sign-in reuses tokens.
    private var generation = 0L

    data class Snapshot(val tokens: AuthTokens?, val generation: Long)

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                current = store.tokens.first()
                mutableState.value = stateFor(current)
                ready.complete(Unit)
            } catch (error: Exception) {
                ready.completeExceptionally(error)
            }
        }
    }

    private fun stateFor(tokens: AuthTokens?): AuthState {
        if (tokens == null) return AuthState.SignedOut
        val subject = runCatching {
            val payload =
                String(
                    Base64.getUrlDecoder().decode(tokens.accessToken.split('.')[1]),
                    Charsets.UTF_8,
                )
            json.parseToJsonElement(payload).jsonObject.getValue("sub").jsonPrimitive.content
        }
            .getOrNull()
        return subject?.takeIf { it.isNotBlank() }?.let(AuthState::SignedIn) ?: AuthState.SignedOut
    }

    suspend fun snapshot(): Snapshot {
        ready.await()
        return lock.withLock { Snapshot(current, generation) }
    }

    private suspend fun publish(tokens: AuthTokens?) {
        val next = stateFor(tokens)
        if (mutableState.value != next) cache.clear()
        if (tokens == null) store.clear() else store.save(tokens)
        current = tokens
        mutableState.value = next
    }

    suspend fun signIn(tokens: AuthTokens): AuthTokens? {
        ready.await()
        return lock.withLock {
            val old = current
            publish(tokens)
            generation++
            signingOut = false
            old
        }
    }

    suspend fun beginSignOut(): Snapshot {
        ready.await()
        return lock.withLock {
            signingOut = true
            generation++
            Snapshot(current, generation)
        }
    }

    suspend fun endSignOut(snapshot: Snapshot) = lock.withLock {
        // A newer explicit sign-in must not be destroyed by this sign-out.
        if (generation == snapshot.generation) {
            try {
                publish(null)
            } finally {
                signingOut = false
            }
        }
    }

    suspend fun rotate(source: Snapshot, tokens: AuthTokens): Boolean = lock.withLock {
        if (signingOut || generation != source.generation || current != source.tokens) false
        else {
            publish(tokens)
            true
        }
    }

    suspend fun reject(source: Snapshot) = lock.withLock {
        if (generation == source.generation && current == source.tokens) publish(null)
    }
}
