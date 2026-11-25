package com.example.indigo.feature.auth

import com.example.indigo.core.auth.AuthState as SessionState
import com.example.indigo.core.network.ApiErrorBody
import com.example.indigo.core.network.ApiException
import com.example.indigo.testing.FakeSessionRepository
import com.example.indigo.testing.MainDispatcherRule
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthFormViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val session = FakeSessionRepository()

    private fun form(vm: AuthFormViewModel) {
        vm.send(AuthFormAction.EmailChanged("person@example.com"))
        vm.send(AuthFormAction.PasswordChanged("secret"))
    }

    @Test
    fun `unexpected repository failure shows generic error and permits retry`() = runTest {
        session.onSignIn = { _, _ -> throw IllegalStateException("Private details") }
        val vm = AuthFormViewModel(session)
        form(vm)
        vm.send(AuthFormAction.Submit)
        advanceUntilIdle()
        assertTrue(vm.state.value.failed)
        assertNull(vm.state.value.errorMessage)
        assertTrue(vm.state.value.canSubmit)
        session.onSignIn = { _, _ -> }
        vm.send(AuthFormAction.Submit)
        advanceUntilIdle()
        assertFalse(vm.state.value.failed)
    }

    @Test
    fun `required fields prevent requests in each mode`() = runTest {
        var requests = 0
        session.onSignIn = { _, _ -> requests++ }
        session.onSignUp = { _, _, _ -> requests++ }
        session.onRequestPasswordReset = { requests++ }
        val vm = AuthFormViewModel(session)
        vm.send(AuthFormAction.Submit)
        vm.send(AuthFormAction.ModeChanged(AuthMode.SignUp))
        form(vm)
        vm.send(AuthFormAction.Submit)
        vm.send(AuthFormAction.ModeChanged(AuthMode.ForgotPassword))
        vm.send(AuthFormAction.EmailChanged(""))
        vm.send(AuthFormAction.Submit)
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
        val vm = AuthFormViewModel(session)
        form(vm)
        vm.send(AuthFormAction.Submit)
        vm.send(AuthFormAction.Submit)
        vm.send(AuthFormAction.ModeChanged(AuthMode.SignUp))
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
        val vm = AuthFormViewModel(session)
        vm.send(AuthFormAction.ModeChanged(AuthMode.SignUp))
        form(vm)
        vm.send(AuthFormAction.NameChanged("Person"))
        vm.send(AuthFormAction.Submit)
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
        val vm = AuthFormViewModel(session)
        form(vm)
        vm.send(AuthFormAction.Submit)
        advanceUntilIdle()
        assertEquals("Email not verified", vm.state.value.errorMessage)
        vm.send(AuthFormAction.ResendVerification)
        advanceUntilIdle()
        assertEquals("person@example.com", resent)
        assertTrue(vm.state.value.verificationEmailSent)
        vm.send(AuthFormAction.EmailChanged("other@example.com"))
        assertNull(vm.state.value.unverifiedEmail)
        assertFalse(vm.state.value.verificationEmailSent)
    }

    @Test
    fun `other HTTP errors and transport failures never offer resend`() = runTest {
        val vm = AuthFormViewModel(session)
        form(vm)
        session.onSignIn = { _, _ -> throw ApiException(403, ApiErrorBody("Email not verified")) }
        vm.send(AuthFormAction.Submit)
        advanceUntilIdle()
        assertEquals("Email not verified", vm.state.value.errorMessage)
        assertNull(vm.state.value.unverifiedEmail)
        session.onSignIn = { _, _ -> throw IOException("private transport details") }
        vm.send(AuthFormAction.Submit)
        advanceUntilIdle()
        assertTrue(vm.state.value.failed)
        assertNull(vm.state.value.errorMessage)
    }

    @Test
    fun `password reset success preserves signed out session and clears old error`() = runTest {
        val vm = AuthFormViewModel(session)
        vm.send(AuthFormAction.ModeChanged(AuthMode.ForgotPassword))
        vm.send(AuthFormAction.EmailChanged("person@example.com"))
        session.onRequestPasswordReset = { throw ApiException(500, ApiErrorBody("Try later")) }
        vm.send(AuthFormAction.Submit)
        advanceUntilIdle()
        assertEquals("Try later", vm.state.value.errorMessage)
        session.onRequestPasswordReset = { assertEquals("person@example.com", it) }
        vm.send(AuthFormAction.Submit)
        advanceUntilIdle()
        assertTrue(vm.state.value.resetEmailSent)
        assertFalse(vm.state.value.failed)
        assertNull(vm.state.value.errorMessage)
        assertEquals(SessionState.SignedOut, session.authState.value)
    }
}
