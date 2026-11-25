package com.example.indigo.testing

import com.example.indigo.core.model.Note
import com.example.indigo.core.notes.NotesRepository
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Drive notes with upsert and delete. */
class FakeNotesRepository : NotesRepository {
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
