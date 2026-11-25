package com.example.indigo.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.indigo.core.model.Note
import java.time.Instant
import java.util.UUID

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: UUID,
    val title: String,
    val body: String,
    val createdAt: Instant,
    val updatedAt: Instant?,
)

internal fun NoteEntity.toModel() = Note(id, title, body, createdAt, updatedAt)

internal fun Note.toEntity() = NoteEntity(id, title, body, createdAt, updatedAt)
