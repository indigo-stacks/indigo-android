package com.example.indigo.core.model

import java.time.Instant
import java.util.UUID

data class Note(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val body: String = "",
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant? = null,
)
