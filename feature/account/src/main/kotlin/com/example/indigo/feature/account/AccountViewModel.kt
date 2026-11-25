package com.example.indigo.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.indigo.core.account.AccountRepository
import com.example.indigo.core.account.UserProfile
import com.example.indigo.core.auth.SessionRepository
import com.example.indigo.core.network.ApiException
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AccountState(
    val profile: UserProfile? = null,
    val isLoading: Boolean = false,
    val failed: Boolean = false,
    val errorMessage: String? = null,
    val confirmSignOut: Boolean = false,
    val isSigningOut: Boolean = false,
)

sealed interface AccountAction {
    data object Retry : AccountAction

    data object SignOut : AccountAction

    data object ConfirmSignOut : AccountAction

    data object CancelSignOut : AccountAction
}

@HiltViewModel
class AccountViewModel
@Inject
constructor(private val account: AccountRepository, private val session: SessionRepository) :
    ViewModel() {
    private val mutableState = MutableStateFlow(AccountState())
    val state: StateFlow<AccountState> = mutableState.asStateFlow()

    init {
        load()
    }

    fun send(action: AccountAction) {
        if (state.value.isSigningOut) return
        when (action) {
            AccountAction.Retry -> if (!state.value.isLoading) load()
            AccountAction.SignOut -> mutableState.value = state.value.copy(confirmSignOut = true)
            AccountAction.CancelSignOut ->
                mutableState.value = state.value.copy(confirmSignOut = false)
            AccountAction.ConfirmSignOut ->
                if (state.value.confirmSignOut) {
                    mutableState.value =
                        state.value.copy(
                            confirmSignOut = false,
                            isSigningOut = true,
                            failed = false,
                            errorMessage = null,
                        )
                    viewModelScope.launch {
                        try {
                            session.signOut()
                            mutableState.value = state.value.copy(isSigningOut = false)
                        } catch (error: Exception) {
                            if (error is CancellationException) throw error
                            fail(error, signingOut = true)
                        }
                    }
                }
        }
    }

    private fun load() {
        mutableState.value = state.value.copy(isLoading = true, failed = false, errorMessage = null)
        viewModelScope.launch {
            try {
                val profile = account.profile()
                mutableState.value = state.value.copy(profile = profile, isLoading = false)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                fail(error)
            }
        }
    }

    private fun fail(error: Exception, signingOut: Boolean = false) {
        // Profile and sign-out requests can overlap; neither completion owns the other's gate.
        mutableState.value =
            state.value.copy(
                isLoading = if (signingOut) state.value.isLoading else false,
                isSigningOut = if (signingOut) false else state.value.isSigningOut,
                failed = true,
                errorMessage = (error as? ApiException)?.body?.message,
            )
    }
}
