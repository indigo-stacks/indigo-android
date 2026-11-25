package com.example.indigo.core.di

import com.example.indigo.core.data.UserScopedCache
import com.example.indigo.core.notes.InMemoryNotesRepository
import com.example.indigo.core.notes.NotesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindNotesRepository(implementation: InMemoryNotesRepository): NotesRepository

    companion object {
        /** Phase-one placeholder; persistence replaces this with deletion of all cached notes. */
        @Provides
        @Singleton
        fun provideUserScopedCache(): UserScopedCache =
            object : UserScopedCache {
                override suspend fun clear() = Unit
            }
    }
}
