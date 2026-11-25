package com.example.indigo.core.di

import android.content.Context
import androidx.room.Room
import com.example.indigo.core.data.IndigoDatabase
import com.example.indigo.core.data.NoteDao
import com.example.indigo.core.data.RoomUserScopedCache
import com.example.indigo.core.data.UserScopedCache
import com.example.indigo.core.notes.NotesRepository
import com.example.indigo.core.notes.RoomNotesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindNotesRepository(implementation: RoomNotesRepository): NotesRepository

    @Binds
    @Singleton
    abstract fun bindUserScopedCache(implementation: RoomUserScopedCache): UserScopedCache

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): IndigoDatabase =
            Room.databaseBuilder(context, IndigoDatabase::class.java, "indigo.db").build()

        @Provides fun provideNoteDao(database: IndigoDatabase): NoteDao = database.noteDao()
    }
}
