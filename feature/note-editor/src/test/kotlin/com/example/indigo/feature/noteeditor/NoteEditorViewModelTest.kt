package com.example.indigo.feature.noteeditor

import app.cash.turbine.test
import com.example.indigo.core.model.Note
import com.example.indigo.core.notes.NotesRepository
import com.example.indigo.testing.FakeNotesRepository
import com.example.indigo.testing.MainDispatcherRule
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoteEditorViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `existing note loads and saves with same identity`() = runTest {
        val repository = FakeNotesRepository()
        val note = Note(title = "Original", body = "Body")
        repository.upsert(note)
        val viewModel = NoteEditorViewModel(repository, note.id)
        viewModel.state.test {
            assertTrue(awaitItem().isLoading)
            val loaded = awaitItem()
            assertEquals("Original", loaded.title)
            assertEquals("Body", loaded.body)
            assertTrue(loaded.isEditing)
        }
        viewModel.send(NoteEditorAction.TitleChanged("  Changed  "))
        viewModel.events.test {
            viewModel.send(NoteEditorAction.Save)
            assertEquals(NoteEditorEvent.Close, awaitItem())
        }
        val saved = repository.observeNote(note.id).first()!!
        assertEquals("Changed", saved.title)
        assertEquals(note.createdAt, saved.createdAt)
        assertNotNull(saved.updatedAt)
    }

    @Test
    fun `new draft is not persisted until explicit save and blank title never saves`() = runTest {
        val repository = FakeNotesRepository()
        val viewModel = NoteEditorViewModel(repository, null)
        viewModel.send(NoteEditorAction.BodyChanged("Draft body"))
        viewModel.send(NoteEditorAction.TitleChanged(" \n "))
        viewModel.send(NoteEditorAction.Save)
        runCurrent()
        assertTrue(repository.observeNotes().first().isEmpty())
        viewModel.send(NoteEditorAction.TitleChanged("New"))
        runCurrent()
        assertTrue(repository.observeNotes().first().isEmpty())
        viewModel.events.test {
            viewModel.send(NoteEditorAction.Save)
            viewModel.send(NoteEditorAction.Save)
            assertEquals(NoteEditorEvent.Close, awaitItem())
        }
        assertEquals(1, repository.observeNotes().first().size)
        assertEquals("Draft body", repository.observeNotes().first().single().body)
    }

    @Test
    fun `delete closes only after removing existing note`() = runTest {
        val repository = FakeNotesRepository()
        val note = Note(title = "Delete me")
        repository.upsert(note)
        val viewModel = NoteEditorViewModel(repository, note.id)
        runCurrent()
        viewModel.events.test {
            viewModel.send(NoteEditorAction.Delete)
            assertEquals(NoteEditorEvent.Close, awaitItem())
            assertNull(repository.observeNote(note.id).first())
        }
    }

    @Test
    fun `cancel discards draft`() = runTest {
        val repository = FakeNotesRepository()
        val viewModel = NoteEditorViewModel(repository, null)
        viewModel.send(NoteEditorAction.TitleChanged("Unsaved"))
        viewModel.events.test {
            viewModel.send(NoteEditorAction.Cancel)
            assertEquals(NoteEditorEvent.Close, awaitItem())
        }
        assertTrue(repository.observeNotes().first().isEmpty())
    }

    @Test
    fun `failed save retains draft for retry without closing`() = runTest {
        val fake = FakeNotesRepository()
        var fail = true
        val repository =
            object : NotesRepository by fake {
                override suspend fun upsert(note: Note) {
                    if (fail) throw IOException()
                    fake.upsert(note)
                }
            }
        val viewModel = NoteEditorViewModel(repository, null)
        viewModel.send(NoteEditorAction.TitleChanged("Keep me"))
        viewModel.events.test {
            viewModel.send(NoteEditorAction.Save)
            runCurrent()
            expectNoEvents()
            assertTrue(viewModel.state.value.hasError)
            assertEquals("Keep me", viewModel.state.value.title)
            assertTrue(viewModel.state.value.canSave)
            fail = false
            viewModel.send(NoteEditorAction.Save)
            assertEquals(NoteEditorEvent.Close, awaitItem())
        }
    }

    @Test
    fun `missing note cannot be recreated by save`() = runTest {
        val repository = FakeNotesRepository()
        val viewModel = NoteEditorViewModel(repository, UUID.randomUUID())
        runCurrent()
        assertTrue(viewModel.state.value.isMissing)
        viewModel.send(NoteEditorAction.TitleChanged("Gone"))
        viewModel.send(NoteEditorAction.Save)
        runCurrent()
        assertTrue(repository.observeNotes().first().isEmpty())
    }
}
