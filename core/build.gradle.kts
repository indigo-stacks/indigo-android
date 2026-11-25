plugins {
    id("indigo.android.library")
    id("indigo.android.hilt")
    id("indigo.android.room")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    buildFeatures.buildConfig = true
    buildTypes {
        debug { buildConfigField("String", "API_BASE_URL", "\"http://localhost:4321\"") }
        release { buildConfigField("String", "API_BASE_URL", "\"https://api.example.com\"") }
    }
}

dependencies {
    api(libs.coroutines.core)
    implementation(libs.serialization.json)
    implementation(libs.datastore.preferences)
    implementation(libs.tink)
    implementation(libs.retrofit)
    implementation(libs.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    debugImplementation(libs.chucker.library)
    releaseImplementation(libs.chucker.library.no.op)
    testImplementation(libs.mockwebserver3)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
