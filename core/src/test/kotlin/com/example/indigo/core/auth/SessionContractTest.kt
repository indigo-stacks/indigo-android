package com.example.indigo.core.auth

import com.example.indigo.core.account.AccountApi
import com.example.indigo.core.account.LiveAccountRepository
import com.example.indigo.core.data.UserScopedCache
import com.example.indigo.core.network.ApiException
import com.example.indigo.core.network.AuthApi
import com.example.indigo.core.network.BearerInterceptor
import com.example.indigo.core.network.TokenAuthenticator
import java.io.IOException
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class SessionContractTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    private val server = MockWebServer()
    private val store = MemoryTokens()
    private val clears = AtomicInteger()
    private lateinit var credentials: Credentials
    private lateinit var session: LiveSessionRepository
    private lateinit var refresh: TokenRefresh
    private lateinit var account: LiveAccountRepository

    @Before
    fun setUp() {
        server.start()
        credentials =
            Credentials(
                store,
                object : UserScopedCache {
                    override suspend fun clear() {
                        clears.incrementAndGet()
                    }
                },
                json,
            )
        val client = OkHttpClient.Builder().callTimeout(5, TimeUnit.SECONDS).build()
        fun retrofit(client: OkHttpClient) =
            Retrofit.Builder()
                .baseUrl(server.url("/"))
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
        val api = retrofit(client).create(AuthApi::class.java)
        session = LiveSessionRepository(api, credentials, json)
        refresh = TokenRefresh(api, credentials, json)
        account =
            LiveAccountRepository(
                retrofit(
                        client
                            .newBuilder()
                            .dispatcher(okhttp3.Dispatcher())
                            .addInterceptor(BearerInterceptor(credentials))
                            .authenticator(TokenAuthenticator(refresh))
                            .build()
                    )
                    .create(AccountApi::class.java),
                json,
            )
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun tokens(user: String = "one", refresh: String = "refresh-one") =
        AuthTokens(
            "e30.${Base64.getUrlEncoder().withoutPadding().encodeToString("""{"sub":"$user"}""".toByteArray())}.sig",
            refresh,
        )

    private fun response(code: Int = 200, body: String = "{}") =
        MockResponse.Builder().code(code).body(body).build()

    private fun tokenBody(tokens: AuthTokens) = json.encodeToString(tokens)

    @Test
    fun signInUsesBasicAndPersistsBeforeDisplacedRevoke() = runBlocking {
        val old = tokens()
        credentials.signIn(old)
        val next = tokens("two", "refresh-two")
        server.enqueue(response(body = tokenBody(next)))
        server.enqueue(response(503))
        session.signIn("two@example.com", "secret")
        val signIn = server.takeRequest()
        assertEquals("/api/v1/auth/sign-in", signIn.url.encodedPath)
        assertEquals("Basic dHdvQGV4YW1wbGUuY29tOnNlY3JldA==", signIn.headers["Authorization"])
        assertEquals("application/json", signIn.headers["Content-Type"])
        assertEquals(0L, signIn.bodySize)
        assertEquals(next, store.tokens.value)
        assertEquals(AuthState.SignedIn("two"), session.authState.value)
        assertEquals("/api/v1/auth/revoke-access", server.takeRequest().url.encodedPath)
        assertEquals(2, clears.get())
    }

    @Test
    fun accountEntryRoutesNeverStoreCredentials() = runBlocking {
        repeat(4) { server.enqueue(response()) }
        session.signUp("Name", "one@example.com", "secret")
        session.requestPasswordReset("one@example.com")
        session.sendVerificationEmail("one@example.com")
        session.resetPassword("reset", "new-secret")
        assertNull(store.tokens.value)
        assertEquals(AuthState.SignedOut, credentials.snapshot().let { session.authState.value })
    }

    @Test
    fun concurrentUnauthorizedCallsShareRotationAndRetryBearer() = runBlocking {
        val old = tokens()
        val rotated = old.copy(accessToken = old.accessToken + "new", refreshToken = "rotated")
        credentials.signIn(old)
        val rotations = AtomicInteger()
        server.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse =
                    when {
                        request.url.encodedPath.endsWith("refresh-access") -> {
                            rotations.incrementAndGet()
                            response(body = tokenBody(rotated))
                        }
                        request.headers["Authorization"] == "Bearer ${rotated.accessToken}" ->
                            response(
                                body =
                                    """{"id":"one","name":"Name","email":"one@example.com","emailVerified":true,"image":null}"""
                            )
                        else -> response(401)
                    }
            }
        val profiles = (1..8).map { async(Dispatchers.IO) { account.profile() } }.awaitAll()
        assertTrue(profiles.all { it.id == "one" })
        assertEquals(1, rotations.get())
        assertEquals(rotated, store.tokens.value)
    }

    @Test
    fun onlyRefresh401DestroysCredentialsAndParsesError() = runBlocking {
        val old = tokens()
        credentials.signIn(old)
        server.enqueue(response(500, """{"error":"Try later","code":"BUSY"}"""))
        val transient =
            runCatching { refresh.refresh(old.accessToken) }.exceptionOrNull() as ApiException
        assertEquals(500, transient.statusCode)
        assertEquals("Try later", transient.body?.message)
        assertEquals("BUSY", transient.body?.code)
        assertEquals(old, store.tokens.value)
        server.enqueue(response(401, """{"error":"Rejected"}"""))
        assertTrue(
            runCatching { refresh.refresh(old.accessToken) }.exceptionOrNull() is ApiException
        )
        assertNull(store.tokens.value)
        assertEquals(AuthState.SignedOut, session.authState.value)
    }

    @Test
    fun transportFailurePreservesSession() = runBlocking {
        val old = tokens()
        credentials.signIn(old)
        server.close()
        assertTrue(
            runCatching { refresh.refresh(old.accessToken) }.exceptionOrNull() is IOException
        )
        assertEquals(old, store.tokens.value)
        assertEquals(AuthState.SignedIn("one"), session.authState.value)
    }

    @Test
    fun signOutRevokesBeforeDestroyEvenWhenRevokeFails() = runBlocking {
        val old = tokens()
        credentials.signIn(old)
        server.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    assertEquals(old, store.tokens.value)
                    assertEquals("/api/v1/auth/revoke-access", request.url.encodedPath)
                    return response(500)
                }
            }
        session.signOut()
        assertNull(store.tokens.value)
        assertEquals(AuthState.SignedOut, session.authState.value)
        assertEquals(2, clears.get())
    }

    @Test
    fun lateRotationCannotRepublishAfterSignOutAndIsRevoked() = runBlocking {
        val old = tokens()
        val rotated = tokens(refresh = "rotated")
        credentials.signIn(old)
        val refreshStarted = CountDownLatch(1)
        val releaseRefresh = CountDownLatch(1)
        val revoked = mutableListOf<String>()
        server.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    if (request.url.encodedPath.endsWith("refresh-access")) {
                        refreshStarted.countDown()
                        check(releaseRefresh.await(5, TimeUnit.SECONDS))
                        return response(body = tokenBody(rotated))
                    }
                    synchronized(revoked) { revoked.add(requireNotNull(request.body).utf8()) }
                    return response()
                }
            }
        val flight = async(Dispatchers.IO) { refresh.refresh(old.accessToken) }
        assertTrue(withContext(Dispatchers.IO) { refreshStarted.await(5, TimeUnit.SECONDS) })
        session.signOut()
        releaseRefresh.countDown()
        assertNull(flight.await())
        assertNull(store.tokens.value)
        assertEquals(AuthState.SignedOut, session.authState.value)
        assertEquals(
            listOf("""{"refreshToken":"refresh-one"}""", """{"refreshToken":"rotated"}"""),
            revoked,
        )
    }

    @Test
    fun startupReadsStoredTokensWithoutNetwork() = runBlocking {
        val read = CompletableDeferred<Unit>()
        val held = tokens()
        val delayed =
            object : TokenStore {
                override val tokens =
                    kotlinx.coroutines.flow.flow {
                        read.await()
                        emit(held)
                    }

                override suspend fun save(tokens: AuthTokens) = Unit

                override suspend fun clear() = Unit
            }
        val restored =
            Credentials(
                delayed,
                object : UserScopedCache {
                    override suspend fun clear() {
                        fail("Startup must not clear cached notes")
                    }
                },
                json,
            )
        assertEquals(AuthState.Loading, restored.state.value)
        read.complete(Unit)
        assertEquals(held, restored.snapshot().tokens)
        assertEquals(AuthState.SignedIn("one"), restored.state.value)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun failedRotationSaveRetriesPersistenceNotSpentRefreshToken() = runBlocking {
        val old = tokens()
        val rotated = tokens(refresh = "rotated")
        credentials.signIn(old)
        server.enqueue(response(body = tokenBody(rotated)))
        store.failNextSave = true
        assertTrue(
            runCatching { refresh.refresh(old.accessToken) }.exceptionOrNull() is IOException
        )
        assertEquals(old, store.tokens.value)
        assertEquals(rotated, refresh.refresh(old.accessToken))
        assertEquals(rotated, store.tokens.value)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun cancellingCallerDuringRevokeStillDestroysCredentials() = runBlocking {
        credentials.signIn(tokens())
        val revokeStarted = CountDownLatch(1)
        val releaseRevoke = CountDownLatch(1)
        server.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    assertEquals("/api/v1/auth/revoke-access", request.url.encodedPath)
                    revokeStarted.countDown()
                    check(releaseRevoke.await(5, TimeUnit.SECONDS))
                    return response(503)
                }
            }
        val caller = launch(Dispatchers.IO) { session.signOut() }
        assertTrue(withContext(Dispatchers.IO) { revokeStarted.await(5, TimeUnit.SECONDS) })
        caller.cancel()
        releaseRevoke.countDown()
        withTimeout(5_000) { caller.join() }
        assertTrue(caller.isCancelled)
        assertNull(store.tokens.value)
        assertEquals(AuthState.SignedOut, session.authState.value)
    }

    private class MemoryTokens : TokenStore {
        override val tokens = MutableStateFlow<AuthTokens?>(null)

        var failNextSave = false

        override suspend fun save(tokens: AuthTokens) {
            if (failNextSave) {
                failNextSave = false
                throw IOException("Disk unavailable")
            }
            this.tokens.value = tokens
        }

        override suspend fun clear() {
            tokens.value = null
        }
    }
}
