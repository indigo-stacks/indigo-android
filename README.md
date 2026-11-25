# Indigo Stack — Android

A modular Android app template with a working local notes app and an optional
account backed by Indigo Fullstack. Android 12+ (API 31), Jetpack Compose,
Material 3, Navigation 3, Hilt, Room, Retrofit, and encrypted token storage.
Notes work without a backend.

## Getting Started

### Setup

1. Install [mise](https://mise.jdx.dev), then install the pinned tools:

   ```sh
   mise trust
   mise install
   ```

   Gradle runs on Temurin **25**. Install Temurin **21** as well for the Java 21
   compilation toolchain (`mise install java@temurin-21`). All modules target
   Java 21. AGP 9 supplies Kotlin; no separate Android Kotlin plugin is needed.

2. Set the Android SDK path:

   ```sh
   export ANDROID_HOME="$HOME/.local/share/mise/installs/android-sdk/23"
   ```

   Install platform **37.1** and build-tools **37.0.0** through the mise Android
   CLI (`android sdk list`, `android sdk install`). Open this folder in Android
   Studio. Set its Gradle JDK to the path from `mise where java` and its SDK path
   to the path above. A local `local.properties` with `sdk.dir` is also supported;
   do not commit it.

3. Configure `build-logic/convention/src/main/kotlin/IndigoConfig.kt` (package,
   SDK levels, JVM target). If you change the base package, also rename Kotlin
   source packages and imports, including resource `R` imports. Set your release
   API URL in `core/build.gradle.kts`; `https://api.example.com` is a placeholder.

4. Build or run from Android Studio, or use the wrapper:

   ```sh
   mise exec -- ./gradlew build
   mise exec -- ./gradlew test
   mise exec -- ./gradlew spotlessApply
   mise exec -- ./gradlew :app:installDebug
   mise exec -- ./gradlew :app:assembleRelease
   ```

   `build` runs assembly, unit tests, formatting checks, and lint. Release uses R8
   and resource shrinking. The release APK is unsigned; configure signing before
   distribution. CI installs JDK 21 and 25, uses 25 to run the Gradle wrapper,
   and installs the same Android platform and build-tools.

### Local backend

Run the `indigo-fullstack` development server on `localhost:4321` (follow that
repository's setup). Debug builds use `http://localhost:4321`:

```sh
adb reverse tcp:4321 tcp:4321
```

If host port 4321 is occupied, run the backend on another port, then map the
same device port to it: `adb reverse tcp:4321 tcp:<port>`. Debug allows cleartext
only to localhost; release uses HTTPS.

### Debug network console

Chucker records debug HTTP traffic. Make an API request, then tap its notification
to open the network console. Allow notifications if Android asks; if needed,
enable notifications for Indigo in system settings. Release links the no-op
library. Do not share captured credentials or response bodies.

## Project Architecture

| Module | Responsibility |
| --- | --- |
| `app` | Hilt application, activity, launcher resources, theme host |
| `core` | Models, Room notes, encrypted DataStore tokens, session, Retrofit APIs, Hilt bindings |
| `components` | Theme and reusable Compose controls; no core dependency |
| `feature/root` | Session shell and Navigation 3 wiring |
| `feature/auth` | Sign in, sign up, password reset, verification resend |
| `feature/account` | Profile and confirmed sign-out |
| `feature/notes-list` | Observe, open, create, and delete local notes |
| `feature/note-editor` | Load, edit, explicitly save, and delete a note |
| `testing` | Repository fakes and main dispatcher rule; test-only |
| `build-logic/convention` | Shared plugins, dependencies, and `IndigoConfig` |

### Dependency Graph

```text
app → core, components, feature:root
feature:root → auth, account, notes-list, note-editor
all features → core, components
feature unit tests → testing → core
components → Compose / Material 3 only
core → Room, DataStore, Tink, Retrofit, OkHttp, Hilt
```

Features do not depend on each other; root connects navigation callbacks. Core
never depends on a feature. Each screen uses immutable state, sealed actions,
`ViewModel.send`, and a stateless content composable with light/dark previews.

## Working with Dependencies

Convention plugins in `build-logic/convention` provide Android application and
library setup, Compose, Hilt, feature defaults, Room, and Spotless. Add versions
and aliases in `gradle/libs.versions.toml`. Put shared dependencies in the relevant
convention plugin; put module-only dependencies in that module's build file.

To add a feature, create `feature/<name>/build.gradle.kts` with
`plugins { id("indigo.android.feature") }`, include it in `settings.gradle.kts`,
and add it as a dependency of `feature/root`. The feature plugin supplies core,
components, lifecycle, Navigation 3, serialization, Hilt, and test support.

See [AGENTS.md](AGENTS.md) for implementation contracts and
[API clients](docs/api-clients.md) for endpoint and repository examples.
