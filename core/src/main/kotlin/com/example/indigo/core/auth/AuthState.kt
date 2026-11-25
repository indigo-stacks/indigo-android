package com.example.indigo.core.auth
sealed interface AuthState {
    /** Stored credentials not read yet (launch gate). */
    data object Loading : AuthState

    data object SignedOut : AuthState

    /** userId = the access token's `sub` claim. */
    data class SignedIn(val userId: String) : AuthState
}
