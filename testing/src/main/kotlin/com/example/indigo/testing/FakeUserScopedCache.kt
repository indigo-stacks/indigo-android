package com.example.indigo.testing

import com.example.indigo.core.data.UserScopedCache

class FakeUserScopedCache : UserScopedCache {
    var clearCount = 0
        private set

    var onClear: suspend () -> Unit = {}

    override suspend fun clear() {
        onClear()
        clearCount++
    }
}
