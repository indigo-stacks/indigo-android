package com.example.indigo.testing

import com.example.indigo.core.account.*

class FakeAccountRepository : AccountRepository {
    var onProfile: suspend () -> UserProfile = {
        UserProfile("test-user", "Test User", "test@example.com", true, null)
    }

    override suspend fun profile(): UserProfile = onProfile()
}
