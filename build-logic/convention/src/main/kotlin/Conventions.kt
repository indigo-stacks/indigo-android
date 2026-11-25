import androidx.room.gradle.RoomExtension
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

private val Project.libs
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

private fun Project.dep(configuration: String, alias: String) {
    dependencies.add(configuration, libs.findLibrary(alias).get())
}

private fun Project.common() {
    pluginManager.apply("indigo.spotless")
    extensions.configure<KotlinAndroidProjectExtension> { jvmToolchain(IndigoConfig.jvmTarget) }
    dep("testImplementation", "junit")
    dep("testImplementation", "coroutines-test")
    dep("testImplementation", "turbine")
}

class AndroidApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("com.android.application")
            extensions.configure<ApplicationExtension> {
                namespace = IndigoConfig.basePackage
                compileSdk {
                    version =
                        release(IndigoConfig.compileSdk) {
                            minorApiLevel = IndigoConfig.compileSdkMinor
                        }
                }
                defaultConfig {
                    applicationId = IndigoConfig.basePackage
                    minSdk = IndigoConfig.minSdk
                    targetSdk = IndigoConfig.targetSdk
                    versionCode = 1
                    versionName = "1.0"
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.toVersion(IndigoConfig.jvmTarget)
                    targetCompatibility = JavaVersion.toVersion(IndigoConfig.jvmTarget)
                }
                packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
            }
            common()
        }
}

class AndroidLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("com.android.library")
            extensions.configure<LibraryExtension> {
                namespace = IndigoConfig.basePackage + path.replace(":", ".").replace("-", "")
                resourcePrefix = target.name.replace("-", "_") + "_"
                compileSdk {
                    version =
                        release(IndigoConfig.compileSdk) {
                            minorApiLevel = IndigoConfig.compileSdkMinor
                        }
                }
                defaultConfig {
                    minSdk = IndigoConfig.minSdk
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.toVersion(IndigoConfig.jvmTarget)
                    targetCompatibility = JavaVersion.toVersion(IndigoConfig.jvmTarget)
                }
                testOptions { unitTests.isIncludeAndroidResources = true }
            }
            common()
        }
}

class AndroidComposePlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            pluginManager.withPlugin("com.android.library") {
                extensions.configure<LibraryExtension> { buildFeatures.compose = true }
            }
            pluginManager.withPlugin("com.android.application") {
                extensions.configure<ApplicationExtension> { buildFeatures.compose = true }
            }
            dependencies.add(
                "implementation",
                dependencies.platform(libs.findLibrary("compose-bom").get()),
            )
            dependencies.add(
                "androidTestImplementation",
                dependencies.platform(libs.findLibrary("compose-bom").get()),
            )
            listOf(
                    "compose-ui",
                    "compose-foundation",
                    "compose-material3",
                    "compose-preview",
                    "compose-icons",
                )
                .forEach { dep("implementation", it) }
            dep("debugImplementation", "compose-tooling")
            dep("debugImplementation", "compose-test-manifest")
            dep("androidTestImplementation", "compose-test-junit4")
        }
}

class AndroidHiltPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")
            pluginManager.apply("com.google.dagger.hilt.android")
            dep("implementation", "hilt-android")
            dep("ksp", "hilt-compiler")
            // Hilt generates test classes even in scaffold modules with no test cases.
            tasks.withType<Test>().configureEach { failOnNoDiscoveredTests.set(false) }
        }
}

class AndroidFeaturePlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            listOf(
                    "indigo.android.library",
                    "indigo.android.compose",
                    "indigo.android.hilt",
                    "org.jetbrains.kotlin.plugin.serialization",
                )
                .forEach { pluginManager.apply(it) }
            dependencies.add("implementation", project(":core"))
            dependencies.add("implementation", project(":components"))
            dependencies.add("testImplementation", project(":testing"))
            listOf(
                    "navigation3-runtime",
                    "navigation3-ui",
                    "lifecycle-runtime-compose",
                    "lifecycle-viewmodel-compose",
                    "lifecycle-viewmodel-ktx",
                    "lifecycle-viewmodel-navigation3",
                    "hilt-viewmodel-compose",
                    "serialization-json",
                )
                .forEach { dep("implementation", it) }
        }
}

class AndroidRoomPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")
            pluginManager.apply("androidx.room")
            extensions.configure<RoomExtension> { schemaDirectory("$projectDir/schemas") }
            dep("implementation", "room-runtime")
            dep("implementation", "room-ktx")
            dep("ksp", "room-compiler")
        }
}

class SpotlessPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("com.diffplug.spotless")
            extensions.configure<SpotlessExtension> {
                kotlin {
                    target("src/**/*.kt", "build-logic/convention/src/**/*.kt")
                    ktfmt(libs.findVersion("ktfmt").get().requiredVersion).kotlinlangStyle()
                }
                kotlinGradle {
                    target(
                        "*.gradle.kts",
                        "build-logic/*.gradle.kts",
                        "build-logic/convention/*.gradle.kts",
                    )
                    ktfmt(libs.findVersion("ktfmt").get().requiredVersion).kotlinlangStyle()
                }
            }
        }
}
