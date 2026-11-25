package com.example.indigo.feature.auth

import com.example.indigo.core.auth.AuthState as SessionState
import com.example.indigo.core.network.ApiErrorBody
import com.example.indigo.core.network.ApiException
import com.example.indigo.testing.FakeSessionRepository
import com.example.indigo.testing.MainDispatcherRule
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val session = FakeSessionRepository()

    private fun form(vm: AuthViewModel) {
        vm.send(AuthAction.EmailChanged("person@example.com"))
        vm.send(AuthAction.PasswordChanged("secret"))
    }

    @Test
    fun `required fields prevent requests in each mode`() = runTest {
        var requests = 0
        session.onSignIn = { _, _ -> requests++ }
        session.onSignUp = { _, _, _ -> requests++ }
        session.onRequestPasswordReset = { requests++ }
        val vm = AuthViewModel(session)
        vm.send(AuthAction.Submit)
        vm.send(AuthAction.ModeChanged(AuthMode.SignUp))
        form(vm)
        vm.send(AuthAction.Submit)
        vm.send(AuthAction.ModeChanged(AuthMode.ForgotPassword))
        vm.send(AuthAction.EmailChanged(""))
        vm.send(AuthAction.Submit)
        advanceUntilIdle()
        assertEquals(0, requests)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `sign in publishes through repository and prevents duplicate submissions`() = runTest {
        val gate = CompletableDeferred<Unit>()
        var requests = 0
        session.onSignIn = { email, password ->
            assertEquals("person@example.com", email)
            assertEquals("secret", password)
            requests++
            gate.await()
            session.authState.value = SessionState.SignedIn("user")
        }
        val vm = AuthViewModel(session)
        form(vm)
        vm.send(AuthAction.Submit)
        vm.send(AuthAction.Submit)
        vm.send(AuthAction.ModeChanged(AuthMode.SignUp))
        runCurrent()
        assertEquals(1, requests)
        assertEquals(AuthMode.SignIn, vm.state.value.mode)
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(SessionState.SignedIn("user"), session.authState.value)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `sign up returns to sign in and clears password without a session`() = runTest {
        val vm = AuthViewModel(session)
        vm.send(AuthAction.ModeChanged(AuthMode.SignUp))
        form(vm)
        vm.send(AuthAction.NameChanged("Person"))
        vm.send(AuthAction.Submit)
        advanceUntilIdle()
        assertTrue(vm.state.value.signUpSuccess)
        assertEquals(AuthMode.SignIn, vm.state.value.mode)
        assertEquals("", vm.state.value.password)
        assertEquals(SessionState.SignedOut, session.authState.value)
    }

    @Test
    fun `unverified refusal enables resend for that address only`() = runTest {
        session.onSignIn = { _, _ -> throw ApiException(401, ApiErrorBody("Email not verified")) }
        var resent: String? = null
        session.onSendVerificationEmail = { resent = it }
        val vm = AuthViewModel(session)
        form(vm)
        vm.send(AuthAction.Submit)
        advanceUntilIdle()
        assertEquals("Email not verified", vm.state.value.errorMessage)
        vm.send(AuthAction.ResendVerification)
        advanceUntilIdle()
        assertEquals("person@example.com", resent)
        assertTrue(vm.state.value.verificationEmailSent)
        vm.send(AuthAction.EmailChanged("other@example.com"))
        assertNull(vm.state.value.unverifiedEmail)
        assertFalse(vm.state.value.verificationEmailSent)
    }

    @Test
    fun `other HTTP errors and transport failures never offer resend`() = runTest {
        val vm = AuthViewModel(session)
        form(vm)
        session.onSignIn = { _, _ -> throw ApiException(403, ApiErrorBody("Email not verified")) }
        vm.send(AuthAction.Submit)
        advanceUntilIdle()
        assertEquals("Email not verified", vm.state.value.errorMessage)
        assertNull(vm.state.value.unverifiedEmail)
        session.onSignIn = { _, _ -> throw IOException("private transport details") }
        vm.send(AuthAction.Submit)
        advanceUntilIdle()
        assertTrue(vm.state.value.failed)
        assertNull(vm.state.value.errorMessage)
    }

    @Test
    fun `password reset success preserves signed out session and clears old error`() = runTest {
        val vm = AuthViewModel(session)
        vm.send(AuthAction.ModeChanged(AuthMode.ForgotPassword))
        vm.send(AuthAction.EmailChanged("person@example.com"))
        session.onRequestPasswordReset = { throw ApiException(500, ApiErrorBody("Try later")) }
        vm.send(AuthAction.Submit)
        advanceUntilIdle()
        assertEquals("Try later", vm.state.value.errorMessage)
        session.onRequestPasswordReset = { assertEquals("person@example.com", it) }
        vm.send(AuthAction.Submit)
        advanceUntilIdle()
        assertTrue(vm.state.value.resetEmailSent)
        assertFalse(vm.state.value.failed)
        assertNull(vm.state.value.errorMessage)
        assertEquals(SessionState.SignedOut, session.authState.value)
    }
}
