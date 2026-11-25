plugins { `kotlin-dsl` }

kotlin { jvmToolchain(21) }

dependencies {
    implementation(libs.android.gradle)
    implementation(libs.kotlin.gradle)
    implementation(libs.kotlin.compose.gradle)
    implementation(libs.kotlin.serialization.gradle)
    implementation(libs.ksp.gradle)
    implementation(libs.hilt.gradle)
    implementation(libs.room.gradle)
    implementation(libs.spotless.gradle)
}

gradlePlugin {
    plugins {
        register("AndroidApplicationPlugin") {
            id = "indigo.android.application"
            implementationClass = "AndroidApplicationPlugin"
        }
        register("AndroidLibraryPlugin") {
            id = "indigo.android.library"
            implementationClass = "AndroidLibraryPlugin"
        }
        register("AndroidComposePlugin") {
            id = "indigo.android.compose"
            implementationClass = "AndroidComposePlugin"
        }
        register("AndroidHiltPlugin") {
            id = "indigo.android.hilt"
            implementationClass = "AndroidHiltPlugin"
        }
        register("AndroidFeaturePlugin") {
            id = "indigo.android.feature"
            implementationClass = "AndroidFeaturePlugin"
        }
        register("AndroidRoomPlugin") {
            id = "indigo.android.room"
            implementationClass = "AndroidRoomPlugin"
        }
        register("SpotlessPlugin") {
            id = "indigo.spotless"
            implementationClass = "SpotlessPlugin"
        }
    }
}
