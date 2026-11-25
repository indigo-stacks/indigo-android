package com.example.indigo.core.account
interface AccountRepository {
    suspend fun profile(): UserProfile
}
