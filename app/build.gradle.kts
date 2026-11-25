plugins {
    id("indigo.android.application")
    id("indigo.android.compose")
    id("indigo.android.hilt")
}

android {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":components"))
    implementation(project(":feature:root"))
    implementation(libs.activity.compose)
    implementation(libs.core.ktx)
}
