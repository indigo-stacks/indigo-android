package com.example.indigo.feature.noteslist

import app.cash.turbine.test
import com.example.indigo.core.model.Note
import com.example.indigo.core.notes.NotesRepository
import com.example.indigo.testing.FakeNotesRepository
import com.example.indigo.testing.MainDispatcherRule
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotesListViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `list keeps repository ordering and observes deletes`() = runTest {
        val repository = FakeNotesRepository()
        val first =
            Note(
                title = "First",
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH.plusSeconds(2),
            )
        val second = Note(title = "Second", createdAt = Instant.EPOCH.plusSeconds(1))
        repository.upsert(first)
        repository.upsert(second)
        val viewModel = NotesListViewModel(repository)
        viewModel.state.test {
            assertTrue(awaitItem().isLoading)
            assertEquals(listOf(first, second), awaitItem().notes)
            viewModel.send(NotesListAction.Delete(first.id))
            assertEquals(listOf(second), awaitItem().notes)
        }
    }

    @Test
    fun `failed observation can be retried`() = runTest {
        val fake = FakeNotesRepository()
        var fail = true
        val repository =
            object : NotesRepository by fake {
                override fun observeNotes() =
                    if (fail) flow<List<Note>> { throw IOException() } else fake.observeNotes()
            }
        val viewModel = NotesListViewModel(repository)
        runCurrent()
        assertTrue(viewModel.state.value.hasError)
        fail = false
        viewModel.send(NotesListAction.Retry)
        runCurrent()
        assertFalse(viewModel.state.value.hasError)
        assertFalse(viewModel.state.value.isLoading)
    }
}
