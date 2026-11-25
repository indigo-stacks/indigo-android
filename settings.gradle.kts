pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "Indigo"

include(
    ":app",
    ":core",
    ":components",
    ":testing",
    ":feature:root",
    ":feature:auth",
    ":feature:account",
    ":feature:notes-list",
    ":feature:note-editor",
)
