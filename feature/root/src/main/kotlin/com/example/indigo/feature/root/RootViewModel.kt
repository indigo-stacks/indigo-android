package com.example.indigo.feature.root

import androidx.lifecycle.ViewModel
import androidx.navigation3.runtime.NavKey
import com.example.indigo.core.auth.AuthState
import com.example.indigo.core.auth.SessionRepository
import com.example.indigo.feature.account.AccountRoute
import com.example.indigo.feature.auth.AuthRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(session: SessionRepository) : ViewModel() {
    val state = session.authState
}

/** Loading never forces navigation or blocks local notes. */
internal fun accountDestination(state: AuthState): NavKey? =
    when (state) {
        AuthState.Loading -> null
        AuthState.SignedOut -> AuthRoute
        is AuthState.SignedIn -> AccountRoute
    }

internal fun shouldDismissSessionDestination(state: AuthState, destination: NavKey?): Boolean =
    (state is AuthState.SignedIn && destination == AuthRoute) ||
        (state == AuthState.SignedOut && destination == AccountRoute)
