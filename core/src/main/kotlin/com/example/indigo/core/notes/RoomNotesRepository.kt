package com.example.indigo.core.notes

import androidx.room.withTransaction
import com.example.indigo.core.data.IndigoDatabase
import com.example.indigo.core.data.NoteDao
import com.example.indigo.core.data.toEntity
import com.example.indigo.core.data.toModel
import com.example.indigo.core.model.Note
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class RoomNotesRepository
@Inject
constructor(
    private val database: IndigoDatabase,
    private val dao: NoteDao,
) : NotesRepository {
    override fun observeNotes() =
        dao.observeNotes().map { rows -> rows.map { it.toModel() } }.distinctUntilChanged()

    override fun observeNote(id: UUID) =
        dao.observeNote(id).map { it?.toModel() }.distinctUntilChanged()

    /** Creation leaves updatedAt null. Only a content change advances the stored edit time. */
    override suspend fun upsert(note: Note) {
        database.withTransaction {
            val existing = dao.find(note.id)
            if (existing == null) {
                dao.upsert(note.copy(updatedAt = null).toEntity())
            } else if (existing.title != note.title || existing.body != note.body) {
                // Keep creation metadata, even when an editor submits an older snapshot.
                dao.upsert(
                    existing.copy(title = note.title, body = note.body, updatedAt = Instant.now())
                )
            }
        }
    }

    override suspend fun delete(id: UUID) = dao.delete(id)
}
