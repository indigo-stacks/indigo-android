plugins { id("indigo.android.library") }

dependencies {
    api(project(":core"))
    api(libs.junit)
    api(libs.coroutines.test)
    api(libs.turbine)
}
