package com.example.indigo.core.data

import androidx.room.TypeConverter
import java.time.Instant
import java.util.UUID

/** UUIDs use lowercase TEXT; timestamps use epoch milliseconds (INTEGER). */
class DatabaseConverters {
    @TypeConverter fun uuidToText(value: UUID): String = value.toString()

    @TypeConverter fun textToUuid(value: String): UUID = UUID.fromString(value)

    @TypeConverter fun instantToMillis(value: Instant): Long = value.toEpochMilli()

    @TypeConverter fun millisToInstant(value: Long): Instant = Instant.ofEpochMilli(value)
}
