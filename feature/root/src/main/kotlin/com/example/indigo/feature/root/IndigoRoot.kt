package com.example.indigo.feature.root

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.indigo.feature.account.AccountRoute
import com.example.indigo.feature.account.AccountScreen
import com.example.indigo.feature.auth.AuthRoute
import com.example.indigo.feature.auth.AuthScreen
import com.example.indigo.feature.noteeditor.NoteEditorRoute
import com.example.indigo.feature.noteeditor.NoteEditorScreen
import com.example.indigo.feature.noteslist.NotesListRoute
import com.example.indigo.feature.noteslist.NotesListScreen

@Composable
fun IndigoRoot() {
    val backStack = rememberNavBackStack(NotesListRoute)
    Surface(Modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider =
                entryProvider {
                    entry<NotesListRoute> { NotesListScreen() }
                    entry<AuthRoute> { AuthScreen() }
                    entry<AccountRoute> { AccountScreen() }
                    entry<NoteEditorRoute> { NoteEditorScreen() }
                },
        )
    }
}
