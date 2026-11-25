package com.example.indigo.core.notes

import com.example.indigo.core.model.Note
import java.util.UUID
import kotlinx.coroutines.flow.Flow

interface NotesRepository {
    /** All notes, most recently changed first (updatedAt ?: createdAt, descending). */
    fun observeNotes(): Flow<List<Note>>

    fun observeNote(id: UUID): Flow<Note?>

    suspend fun upsert(note: Note)

    suspend fun delete(id: UUID)
}
