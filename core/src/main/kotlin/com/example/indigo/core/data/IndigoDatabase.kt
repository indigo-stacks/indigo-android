package com.example.indigo.core.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Local user-scoped cache. Keep the exported version schemas in core/schemas under version control.
 *
 * To add migration 1 → 2, increment version to 2 and declare `autoMigrations =
 * [AutoMigration(from = 1, to = 2)]` on [Database]. Keep both exported schemas; Room uses version 1
 * to generate the migration. Use an AutoMigrationSpec for renames/deletions or register a manual
 * Migration(1, 2) on the builder for data changes. Test the migration before shipping; do not use
 * destructive migration fallback for schema upgrades.
 */
@Database(entities = [NoteEntity::class], version = 1, exportSchema = true)
@TypeConverters(DatabaseConverters::class)
abstract class IndigoDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}
