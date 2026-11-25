package com.example.indigo.core.data

/** Clear cached user data when the signed-in user changes or the session ends. */
interface UserScopedCache {
    suspend fun clear()
}
