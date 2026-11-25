# AGENTS.md

Guidance for coding agents working in this repository.

## What this is

Indigo is an Android app **template** for API 31+. It ships working local notes
and optional backend accounts. Every cloned app inherits this code. Keep changes
small, generic, and exemplary. There are no phase-one repository placeholders.

## Build & test

`mise.toml` pins Temurin 25 and the Android SDK CLI. Gradle runs on JDK 25; all
modules (including build logic) use a Java 21 toolchain and bytecode. Install JDK
21 as well. Use SDK `~/.local/share/mise/installs/android-sdk/23`, platform 37.1,
and build-tools 37.0.0. See README for Android Studio setup.

```sh
mise exec -- ./gradlew spotlessApply
mise exec -- ./gradlew build
mise exec -- ./gradlew test
mise exec -- ./gradlew :app:assembleRelease
mise exec -- ./gradlew :app:installDebug
```

Run Spotless before every commit. Verify release shrinking after reflection or
serialization changes. Use the wrapper (Gradle 9.9.2), not a system Gradle. AGP 9
provides Kotlin: never apply `org.jetbrains.kotlin.android` or disable the new
Android DSL. Do not commit `local.properties`. CI uses JDK 25 to run Gradle with
JDK 21 available for compilation and the same SDK versions.

## Layout

- `app/`: application, activity, launcher resources; hosts `IndigoRoot`.
- `core/`: models, Room database, repositories, networking, auth, Hilt modules.
- `components/`: Material 3 theme and reusable Compose controls; no core types.
- `feature/root/`: session shell, back stack, and Navigation 3 entry wiring.
- `feature/{auth,account,notes-list,note-editor}/`: isolated screens and ViewModels.
- `testing/`: test-only repository fakes and `MainDispatcherRule`.
- `build-logic/convention/`: plugins and `IndigoConfig` (package, SDKs, JVM target).

Dependencies: `app → core, components, root`; `root → all features`;
`features → core, components`; `feature tests → testing → core`.
Core never depends on features. Features never depend on siblings; root connects
callbacks. Components use Compose/Material 3 only.

## Dependencies

Add versions and library aliases to `gradle/libs.versions.toml`. Shared dependencies
belong in convention plugins, not copied across module build files:

- `indigo.android.application` / `.library`: SDK, Java target, tests, formatting.
- `indigo.android.compose`: Compose compiler, BOM, UI, Material 3, previews.
- `indigo.android.hilt`: Hilt and KSP.
- `indigo.android.feature`: library + Compose + Hilt + serialization; core,
  components, Navigation 3, lifecycle, and test support.
- `indigo.android.room`: Room, KSP, schema export.
- `indigo.spotless`: ktfmt Kotlin and Gradle formatting.

For a new feature, apply `indigo.android.feature`, include `:feature:<name>` in
`settings.gradle.kts`, and add it to root's dependencies. Register its route entry
in root. Module-only dependencies stay in the module build file.

## Conventions

- Immutable `XState`, sealed `XAction`, `XViewModel` with
  `val state: StateFlow<XState>` and `fun send(action: XAction)`.
- Screen types never reuse a `:core` type name; add a qualifier (`AuthForm…`) when they would.
- `XScreen` collects with `collectAsStateWithLifecycle` and calls stateless
  `XContent(state, onAction)`. Preview content with sample state, no DI, in both
  themes. Root is a session/navigation shell and exposes core session state.
- Use Channel-backed `events: Flow<XEvent>` for one-off effects. Collect events
  inside `repeatOnLifecycle` and use the latest navigation callback.
- ViewModels with arguments use assisted Hilt factories. Routes are
  `@Serializable` `NavKey` types. Root owns the saved back stack and entry
  decorators for saved state and ViewModel lifetime. Do not pass repositories
  through navigation callbacks.
- Use shared `IndigoTextField`, `PasswordField`, and `PrimaryButton`. Keep IME
  actions and autofill semantics. Put user-facing text in string resources and
  label icon buttons. Preview-only sample text can remain inline.
- Use explicit imports, `java.time.Instant`, and `java.util.UUID`. Serialize UUIDs
  in lowercase. Notes are sorted by `updatedAt ?: createdAt`, newest first.
- API error text is `ApiException.body?.message`, otherwise a generic resource;
  never show raw exception messages. `apiCall` normalizes unexpected API failures
  to `IOException`. ViewModels also handle repository exceptions (including local
  storage failures) but always rethrow `CancellationException`.

## Persistence

`IndigoDatabase` is a Room database; `DataModule` provides it and binds notes plus
`UserScopedCache`. Exported schemas live in `core/schemas` and must be committed.
For a schema change, increment the database version, retain all old schemas, and
add an auto migration for compatible changes. Use an `AutoMigrationSpec` for
renames/deletions or register a manual `Migration` in the database builder for
data changes. Test upgrades before shipping. Never use destructive fallback for
schema upgrades. Notes are user-scoped cached data, not a remote sync feature.

## Networking / auth

`NetworkModule` supplies the single API `Json`, public and authenticated clients,
APIs, and account repository. `AuthModule` binds the session and encrypted token
store. [API clients](docs/api-clients.md) describes endpoint additions.

- Sign in: Basic auth, JSON content type, no request body. Save the JWT pair before
  best-effort revoke of displaced refresh credentials.
- Sign-up and password reset never sign in. Resend handles `Email not verified`.
- HTTP errors become `ApiException`; transport and response-processing failures
  become `IOException`. Backend `{error, code?}` maps `error` to `message`.
- Authenticated calls use Bearer access tokens. Refresh is single-flight.
  **Only a refresh-access 401 destroys credentials.** Other refresh failures
  propagate and preserve the session.
- Sign-out gates credential publication, then revokes best-effort, then destroys
  local credentials. A late refresh must not restore a signed-out session.
- Startup reads encrypted tokens before leaving Loading; never block the local
  notes shell on a network refresh. Tink AEAD encrypts token DataStore with a
  Keystore-backed key. Do not replace it with deprecated security-crypto.
- Clear **all notes** through `UserScopedCache.clear()` when identity changes or
  a session ends, as in iOS. Credentials owns this ordering before publication.

Debug API URL: `http://localhost:4321`. Run the `indigo-fullstack` dev server and
`adb reverse tcp:4321 tcp:4321`; for another host port use
`adb reverse tcp:4321 tcp:<port>`. Only debug localhost permits cleartext. Change
the release URL in `core/build.gradle.kts` before shipping. Chucker is debug-only:
open its notification after an API request; release uses its no-op library.

## Configuration

Change the application ID and namespace root in
`build-logic/convention/src/main/kotlin/IndigoConfig.kt`; also rename Kotlin source
packages/imports. Update app labels and launcher assets in `app`. Keep `CLAUDE.md`
as a symlink to this file. Do not change public repository contracts without
checking all features and testing fakes.

## Resource names

Every library resource uses its module prefix: `components_`, `core_`, `auth_`,
`account_`, `notes_list_`, `note_editor_`, or `root_` (`testing_` if needed).
The library convention plugin derives `resourcePrefix` from the module name
with hyphens replaced by underscores. Lint checks this rule. Prefix strings,
plurals, and file resources as well: resource names merge into the app and can
otherwise override resources in another module. App-owned resources need no prefix.
