package com.example.indigo.feature.account

import com.example.indigo.core.auth.AuthState
import com.example.indigo.core.network.ApiErrorBody
import com.example.indigo.core.network.ApiException
import com.example.indigo.testing.*
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun `load profile and retain it when retry fails without signing out`() = runTest {
        val account = FakeAccountRepository()
        val session = FakeSessionRepository()
        session.authState.value = AuthState.SignedIn("test-user")
        val vm = AccountViewModel(account, session)
        assertTrue(vm.state.value.isLoading)
        advanceUntilIdle()
        assertEquals("test@example.com", vm.state.value.profile?.email)
        account.onProfile = { throw ApiException(401, ApiErrorBody("Denied")) }
        vm.send(AccountAction.Retry)
        advanceUntilIdle()
        assertEquals("Denied", vm.state.value.errorMessage)
        assertEquals("test@example.com", vm.state.value.profile?.email)
        assertEquals(AuthState.SignedIn("test-user"), session.authState.value)
    }

    @Test
    fun `initial transport failure can be retried`() = runTest {
        val account = FakeAccountRepository()
        val profile = account.onProfile()
        account.onProfile = { throw IOException("offline") }
        val vm = AccountViewModel(account, FakeSessionRepository())
        advanceUntilIdle()
        assertTrue(vm.state.value.failed)
        assertNull(vm.state.value.errorMessage)
        assertFalse(vm.state.value.isLoading)
        account.onProfile = { profile }
        vm.send(AccountAction.Retry)
        advanceUntilIdle()
        assertEquals(profile, vm.state.value.profile)
        assertFalse(vm.state.value.failed)
    }

    @Test
    fun `profile failure cannot release an in flight sign out gate`() = runTest {
        val profileGate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val signOutGate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val account = FakeAccountRepository()
        account.onProfile = {
            profileGate.await()
            throw IOException("offline")
        }
        val session = FakeSessionRepository()
        var calls = 0
        session.onSignOut = {
            calls++
            signOutGate.await()
        }
        val vm = AccountViewModel(account, session)
        vm.send(AccountAction.SignOut)
        vm.send(AccountAction.ConfirmSignOut)
        runCurrent()
        profileGate.complete(Unit)
        runCurrent()
        assertTrue(vm.state.value.isSigningOut)
        vm.send(AccountAction.SignOut)
        vm.send(AccountAction.ConfirmSignOut)
        runCurrent()
        assertEquals(1, calls)
        signOutGate.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.state.value.isSigningOut)
    }

    @Test
    fun `sign out requires confirmation and calls repository once`() = runTest {
        val session = FakeSessionRepository()
        var calls = 0
        session.onSignOut = { calls++ }
        val vm = AccountViewModel(FakeAccountRepository(), session)
        advanceUntilIdle()
        vm.send(AccountAction.SignOut)
        vm.send(AccountAction.CancelSignOut)
        vm.send(AccountAction.ConfirmSignOut)
        advanceUntilIdle()
        assertEquals(0, calls)
        vm.send(AccountAction.SignOut)
        vm.send(AccountAction.ConfirmSignOut)
        vm.send(AccountAction.ConfirmSignOut)
        advanceUntilIdle()
        assertEquals(1, calls)
        assertFalse(vm.state.value.isSigningOut)
    }
}
