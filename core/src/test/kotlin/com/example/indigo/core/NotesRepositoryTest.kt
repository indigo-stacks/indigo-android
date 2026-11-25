package com.example.indigo.core

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.example.indigo.core.data.IndigoDatabase
import com.example.indigo.core.data.RoomUserScopedCache
import com.example.indigo.core.data.toEntity
import com.example.indigo.core.model.Note
import com.example.indigo.core.notes.RoomNotesRepository
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NotesRepositoryTest {
    private lateinit var database: IndigoDatabase
    private lateinit var repository: RoomNotesRepository
    private val created = Instant.parse("2026-01-01T00:00:00Z")

    @Before
    fun setUp() {
        database =
            Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    IndigoDatabase::class.java,
                )
                .build()
        repository = RoomNotesRepository(database, database.noteDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun daoOrdersByEditOrCreationTimeAndRoundTripsNullableTimestamps() = runTest {
        val dao = database.noteDao()
        val old = Note(title = "Old", createdAt = created)
        val recent = Note(title = "Recent", createdAt = created.plusSeconds(60))
        val edited =
            Note(
                title = "Edited",
                createdAt = created.minusSeconds(60),
                updatedAt = created.plusSeconds(120),
            )
        dao.upsert(recent.toEntity())
        dao.upsert(edited.toEntity())
        dao.upsert(old.toEntity())
        assertEquals(
            listOf(edited.toEntity(), recent.toEntity(), old.toEntity()),
            dao.observeNotes().first(),
        )
        assertEquals(old.toEntity(), dao.find(old.id))
        assertEquals(edited.toEntity(), dao.find(edited.id))
        // Check storage, not only converter round trips (Room also has a built-in UUID converter).
        database.openHelper.readableDatabase
            .query("SELECT id, createdAt, updatedAt FROM notes WHERE id = '${old.id}'")
            .use {
                assertTrue(it.moveToFirst())
                assertEquals(old.id.toString(), it.getString(0))
                assertEquals(created.toEpochMilli(), it.getLong(1))
                assertTrue(it.isNull(2))
            }
    }

    @Test
    fun upsertCreatesThenStampsContentChangesAndIgnoresStaleMetadata() = runTest {
        val note = Note(title = "Original", createdAt = created)
        repository.upsert(note)
        assertEquals(note, repository.observeNote(note.id).first())
        val before = Instant.ofEpochMilli(Instant.now().toEpochMilli())
        repository.upsert(note.copy(title = "Edited", createdAt = created.plusSeconds(20)))
        val edited = requireNotNull(repository.observeNote(note.id).first())
        assertEquals("Edited", edited.title)
        assertEquals(created, edited.createdAt)
        assertTrue(!requireNotNull(edited.updatedAt).isBefore(before))
        assertTrue(!edited.updatedAt.isAfter(Instant.now()))
        // The caller may still have the original timestamps. Saving unchanged content is a no-op.
        repository.upsert(note.copy(title = "Edited", updatedAt = Instant.EPOCH))
        assertEquals(edited, repository.observeNote(note.id).first())
        repository.upsert(edited.copy(body = "New body"))
        assertEquals("New body", repository.observeNote(note.id).first()?.body)
    }

    @Test
    fun listAndSingleNoteObserversEmitAfterWritesAndDeletion() = runTest {
        val note = Note(title = "Observed", createdAt = created)
        repository.observeNotes().test {
            assertEquals(emptyList<Note>(), awaitItem())
            val listObserver = this
            repository.observeNote(note.id).test {
                assertNull(awaitItem())
                repository.upsert(note)
                assertEquals(note, awaitItem())
                assertEquals(listOf(note), listObserver.awaitItem())
                repository.delete(note.id)
                assertNull(awaitItem())
                assertEquals(emptyList<Note>(), listObserver.awaitItem())
            }
        }
        repository.delete(UUID.randomUUID())
        assertNull(repository.observeNote(note.id).first())
    }

    @Test
    fun cacheResetDeletesAllNotesAndAllowsNewWrites() = runTest {
        repository.upsert(Note(title = "First", createdAt = created))
        repository.upsert(Note(title = "Second", createdAt = created))
        RoomUserScopedCache(database).clear()
        assertEquals(emptyList<Note>(), repository.observeNotes().first())
        val next = Note(title = "Next user", createdAt = created)
        repository.upsert(next)
        assertEquals(listOf(next), repository.observeNotes().first())
    }
}
