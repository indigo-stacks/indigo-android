package com.example.indigo.core.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import java.util.UUID
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY COALESCE(updatedAt, createdAt) DESC, id ASC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id") fun observeNote(id: UUID): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id") suspend fun find(id: UUID): NoteEntity?

    @Upsert suspend fun upsert(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id") suspend fun delete(id: UUID)

    @Query("DELETE FROM notes") suspend fun deleteAll()
}
