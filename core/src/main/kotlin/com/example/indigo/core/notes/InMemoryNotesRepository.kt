package com.example.indigo.core.notes

import com.example.indigo.core.model.Note
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Phase-one placeholder. Persistence and user-cache clearing replace this in the next
 * implementation pass.
 */
class InMemoryNotesRepository @Inject constructor() : NotesRepository {
    private val notes = MutableStateFlow<List<Note>>(emptyList())

    override fun observeNotes() = notes

    override fun observeNote(id: UUID) = notes.map { list -> list.find { it.id == id } }

    override suspend fun upsert(note: Note) {
        notes.update { list ->
            (list.filterNot { it.id == note.id } + note).sortedByDescending {
                it.updatedAt ?: it.createdAt
            }
        }
    }

    override suspend fun delete(id: UUID) {
        notes.update { list -> list.filterNot { it.id == id } }
    }
}
