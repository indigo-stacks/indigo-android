package com.example.indigo.core

import com.example.indigo.core.model.Note
import com.example.indigo.core.notes.InMemoryNotesRepository
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class NotesRepositoryTest {
    @Test
    fun changesAreOrderedAndObservedById() = runTest {
        val repository = InMemoryNotesRepository()
        val old = Note(title = "Old", createdAt = Instant.parse("2026-01-01T00:00:00Z"))
        val recent = Note(title = "Recent", createdAt = Instant.parse("2026-02-01T00:00:00Z"))
        repository.upsert(recent)
        repository.upsert(old)
        assertEquals(listOf(recent, old), repository.observeNotes().first())

        val edited = old.copy(title = "Edited", updatedAt = Instant.parse("2026-03-01T00:00:00Z"))
        repository.upsert(edited)
        assertEquals(listOf(edited, recent), repository.observeNotes().first())
        assertEquals(edited, repository.observeNote(old.id).first())

        repository.delete(old.id)
        assertNull(repository.observeNote(old.id).first())
        assertEquals(listOf(recent), repository.observeNotes().first())
    }
}
