package com.example.indigo.feature.root

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.example.indigo.feature.noteslist.NotesListRoute
import com.example.indigo.feature.noteslist.NotesListScreen

// Merge-time stand-in; replaced by feature-notes' NotesEntries.kt.
@Suppress("UNUSED_PARAMETER")
fun EntryProviderScope<NavKey>.notesEntries(
    backStack: NavBackStack<NavKey>,
    onAccountClick: () -> Unit,
) {
    entry<NotesListRoute> { NotesListScreen() }
}
