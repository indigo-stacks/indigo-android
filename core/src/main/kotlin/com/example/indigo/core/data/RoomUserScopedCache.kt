package com.example.indigo.core.data

import androidx.room.withTransaction
import javax.inject.Inject

/**
 * Clears every user-scoped table when the signed-in user changes or the session ends. When you add
 * a user-scoped table, add its delete here.
 */
class RoomUserScopedCache @Inject constructor(private val database: IndigoDatabase) :
    UserScopedCache {
    override suspend fun clear() {
        database.withTransaction { database.noteDao().deleteAll() }
    }
}
