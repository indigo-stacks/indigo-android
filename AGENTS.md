# Agent guidance

Indigo is an Android app template. Keep examples small, generic, and suitable
for apps cloned from this repository. Phase-one implementations are placeholders.

## Build and test

```sh
env JAVA_HOME="$(mise where java)" ./gradlew spotlessApply
env JAVA_HOME="$(mise where java)" ./gradlew build
env JAVA_HOME="$(mise where java)" ./gradlew :app:assembleRelease
```

Use JDK 25 to run Gradle and Java 21 bytecode/toolchains for every module.
Do not apply `org.jetbrains.kotlin.android`: AGP 9 provides Kotlin.
Do not disable the new Android DSL. Do not commit `local.properties`.
Run Spotless before each commit. Verify release shrinking when adding reflection.

## Layout and dependencies

- `app`: application, activity, launcher resources; hosts `IndigoRoot`.
- `core`: models, repositories, persistence, API, auth/session, Hilt modules.
- `components`: Material 3 theme and reusable UI; Compose dependencies only.
- `feature/root`: navigation and session shell; depends on all feature modules.
- `feature/{auth,account,notes-list,note-editor}`: isolated screens and ViewModels.
- `testing`: test-only fakes and main dispatcher rule.
- `build-logic/convention`: shared Gradle plugins and `IndigoConfig`.

Add versions to `gradle/libs.versions.toml`. Shared dependencies belong in
convention plugins, not copied between module build files. The feature plugin
supplies core, components, Navigation 3, lifecycle, Hilt, and testing dependencies.
Core must never depend on features. Features must not depend on each other;
root wires navigation callbacks. Keep reusable UI free from core dependencies.

## Kotlin and UI

Use immutable `XState`, sealed `XAction`, and `XViewModel` with
`val state: StateFlow<XState>` and `fun send(action: XAction)`.
Use Channel-backed `events: Flow<XEvent>` for one-off events when needed.
`XScreen` collects state with `collectAsStateWithLifecycle` and calls stateless
`XContent(state, onAction)`. Preview content with sample state, without DI.
Use assisted Hilt factories for ViewModel arguments. Navigation routes are
`@Serializable` `NavKey` types. Use string resources and accessible icon labels.
Use java.time.Instant and java.util.UUID; serialize UUIDs in lowercase.

## Auth contract

- Sign in uses Basic auth, JSON content type, and no request body. Save the JWT
  pair before best-effort revoke of displaced refresh credentials.
- Sign-up and reset-password never sign in. Verification resend handles the
  backend's `Email not verified` response.
- API errors throw `ApiException`; transport errors throw `IOException`.
  The backend uses `{error, code?}`. `ApiErrorBody.message` maps to `error`.
- Authenticated calls use Bearer access tokens. Refresh is single-flight.
  **Only a refresh-access 401 destroys credentials.** Other refresh failures
  propagate and preserve the session.
- Sign-out gates credential publication before best-effort revoke, then destroys
  local credentials. A late refresh must not resurrect a signed-out session.
- Startup reads stored tokens before leaving Loading; never wait for a network
  refresh to render the shell. Encrypt token DataStore with Tink/Keystore.
- Notes are user-scoped cached data, matching iOS. Call `UserScopedCache.clear()`
  when the signed-in user changes or the session ends; clear all notes.

## Ownership for implementation passes

`core/di/DataModule.kt` binds notes; `core/di/AuthModule.kt` binds session;
`core/di/NetworkModule.kt` supplies API Json and the account repository.
Replace placeholders in these files without changing repository public APIs.
