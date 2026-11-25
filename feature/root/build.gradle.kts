plugins { id("indigo.android.feature") }

dependencies {
    implementation(project(":feature:auth"))
    implementation(project(":feature:account"))
    implementation(project(":feature:notes-list"))
    implementation(project(":feature:note-editor"))
}
