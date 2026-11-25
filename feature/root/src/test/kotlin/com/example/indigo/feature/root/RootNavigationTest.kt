package com.example.indigo.feature.root

import com.example.indigo.core.auth.AuthState
import com.example.indigo.feature.account.AccountRoute
import com.example.indigo.feature.auth.AuthRoute
import com.example.indigo.feature.noteslist.NotesListRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RootNavigationTest {
    @Test
    fun `account destination waits for stored tokens then uses session`() {
        assertNull(accountDestination(AuthState.Loading))
        assertEquals(AuthRoute, accountDestination(AuthState.SignedOut))
        assertEquals(AccountRoute, accountDestination(AuthState.SignedIn("user")))
    }

    @Test
    fun `only obsolete session destinations are dismissed`() {
        assertTrue(shouldDismissSessionDestination(AuthState.SignedIn("user"), AuthRoute))
        assertTrue(shouldDismissSessionDestination(AuthState.SignedOut, AccountRoute))
        assertFalse(shouldDismissSessionDestination(AuthState.Loading, AccountRoute))
        assertFalse(shouldDismissSessionDestination(AuthState.Loading, AuthRoute))
        assertFalse(shouldDismissSessionDestination(AuthState.SignedIn("user"), AccountRoute))
        assertFalse(shouldDismissSessionDestination(AuthState.SignedOut, AuthRoute))
        assertFalse(shouldDismissSessionDestination(AuthState.SignedOut, NotesListRoute))
    }
}
