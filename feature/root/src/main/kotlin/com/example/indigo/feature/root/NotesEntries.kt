package com.example.indigo.feature.root

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.example.indigo.feature.noteeditor.NoteEditorRoute
import com.example.indigo.feature.noteeditor.noteEditorEntry
import com.example.indigo.feature.noteslist.notesListEntry

fun EntryProviderScope<NavKey>.notesEntries(
    backStack: NavBackStack<NavKey>,
    onAccountClick: () -> Unit,
) {
    notesListEntry(
        onAccountClick = onAccountClick,
        onNewNote = { backStack.add(NoteEditorRoute()) },
        onOpenNote = { backStack.add(NoteEditorRoute(it.toString())) },
    )
    noteEditorEntry(onBack = { if (backStack.size > 1) backStack.removeLastOrNull() })
}
