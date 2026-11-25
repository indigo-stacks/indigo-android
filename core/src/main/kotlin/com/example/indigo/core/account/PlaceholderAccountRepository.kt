package com.example.indigo.core.account

import javax.inject.Inject

class PlaceholderAccountRepository @Inject constructor() : AccountRepository {
    override suspend fun profile(): UserProfile =
        throw UnsupportedOperationException("Not implemented yet")
}
