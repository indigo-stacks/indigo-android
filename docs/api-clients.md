# API clients

Keep endpoint DTOs, Retrofit interfaces, and repository implementations in `core`.
Features use repository interfaces, not Retrofit or OkHttp directly. Follow
`account/LiveAccountRepository.kt` for a complete existing example.

## Add an endpoint

1. Add a serializable response DTO with the backend's actual fields. Use
   `@SerialName` when the wire name differs. Do not assume undocumented fields.
2. Add a suspend Retrofit endpoint to the appropriate API:

   ```kotlin
   interface ExampleApi {
       @GET("api/v1/example")
       suspend fun example(): ExampleResponse
   }
   ```

   Paths are relative to the base URL and include `api/v1`. Imports above are
   omitted for brevity: use Retrofit's `GET` and your response type.
3. Add a repository interface with domain-facing methods. Inject the API and the
   shared `Json` into its live implementation. Wrap each call with the boundary:

   ```kotlin
   override suspend fun example(): ExampleResponse =
       apiCall(json) { api.example() }
   ```

   `apiCall` maps HTTP failures to `ApiException` and unexpected response failures
   (including serialization) to `IOException`. It preserves cancellation and
   existing I/O failures. Do not expose raw exception messages in UI.
4. Provide the API in `core/di/NetworkModule.kt` using its Retrofit builder and
   `@AuthenticatedClient` for Bearer-protected endpoints or `@PublicClient` for
   public endpoints. Bind the repository through Hilt; use the account provider
   as the model. Do not build a new Json or HTTP client in a feature.
5. Add a fake in `testing` if feature tests need the new interface. Use
   MockWebServer for wire-contract tests: method, headers, JSON, and errors.
   Check that release shrinking preserves the new serializers and endpoints.

## Authentication boundaries

`AuthApi` uses the public client, including refresh and revoke; it must not recurse
through the token authenticator. Sign-in uses a Basic header with JSON content type
and no request body. All other authenticated APIs use the authenticated client.

Only a **401 from refresh-access** rejects stored credentials. A profile 401 first
attempts refresh; offline, timeout, 5xx, and malformed responses preserve the
session. Keep refresh single-flight and the sign-out gate intact. Use
`SessionContractTest` when changing this behavior.

## URLs and debugging

`core/build.gradle.kts` owns `BuildConfig.API_BASE_URL`. Debug uses localhost and
release has a placeholder HTTPS URL that each cloned app must replace.
`NetworkModule` appends a trailing slash for Retrofit. For local development,
reverse device port 4321 to the backend's host port with adb (see README).

Chucker is installed on the debug public client, which the authenticated client
extends. Open its notification after a request to inspect traffic. The release
variant links the no-op dependency. Captures can contain secrets; do not commit or
share them.
