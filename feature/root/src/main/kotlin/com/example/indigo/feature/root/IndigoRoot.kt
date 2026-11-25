package com.example.indigo.feature.root

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.indigo.feature.account.AccountRoute
import com.example.indigo.feature.account.AccountScreen
import com.example.indigo.feature.auth.AuthRoute
import com.example.indigo.feature.auth.AuthScreen
import com.example.indigo.feature.noteslist.NotesListRoute

@Composable
fun IndigoRoot(viewModel: RootViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(NotesListRoute)
    val destination = backStack.lastOrNull()
    LaunchedEffect(state, destination) {
        if (shouldDismissSessionDestination(state, destination)) backStack.removeLastOrNull()
    }
    val onBack: () -> Unit = { if (backStack.size > 1) backStack.removeLastOrNull() }
    Surface(Modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            onBack = onBack,
            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
            entryProvider =
                entryProvider {
                    notesEntries(
                        backStack,
                        onAccountClick = {
                            accountDestination(state)?.let {
                                if (backStack.lastOrNull() != it) backStack.add(it)
                            }
                        },
                    )
                    entry<AuthRoute> { AuthScreen(onBack = onBack) }
                    entry<AccountRoute> { AccountScreen(onBack = onBack) }
                },
        )
    }
}
