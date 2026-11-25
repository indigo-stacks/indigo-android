# Indigo Stack — Android

A modular Android template. Phase one supplies the app shell and shared contracts;
local persistence, networking, and complete feature flows replace the placeholders next.
Android 12+ (API 31), Compose Material 3, Navigation 3, Hilt, Room, and Retrofit.

## Setup

```sh
mise trust
mise install
# Install Android platform 37.1 and build tools with `android sdk install` if needed.
# Set ANDROID_HOME or create local.properties with sdk.dir=<SDK path>.
env JAVA_HOME="$(mise where java)" ./gradlew build
```

Gradle runs on Temurin 25; compilation targets Java 21 (install Temurin 21 too).
Edit `build-logic/convention/src/main/kotlin/IndigoConfig.kt` to change the base
package, SDK levels, or JVM target. Rename source packages when changing the base
package. Set the release API URL in `core/build.gradle.kts` before shipping.
For local API access, run `adb reverse tcp:4321 tcp:4321`; debug permits cleartext
only to localhost. Release defaults to `https://api.example.com`.

## Modules

```text
app → core, components, feature:root
feature:root → auth, account, notes-list, note-editor
all features → core, components
feature unit tests → testing → core
components → Compose only
core → Room, DataStore, Tink, Retrofit, OkHttp, Hilt
```

`build-logic/convention` supplies shared plugins and dependencies.
`testing` supplies controllable repository fakes and `MainDispatcherRule`.
Notes are a user-scoped cache, as in the iOS template. `UserScopedCache.clear()`
must delete all notes when the signed-in user changes or the session ends.
The phase-one cache binding is a no-op until persistence is implemented.

## Commands

```sh
./gradlew spotlessApply
./gradlew build                 # Debug/release assembly, unit tests, lint
./gradlew :app:assembleRelease  # R8 and resource shrinking
./gradlew :app:installDebug
```

Run Gradle with the mise JDK (`env JAVA_HOME="$(mise where java)"` or
`mise exec --`). The release APK is unsigned; configure signing for distribution.
