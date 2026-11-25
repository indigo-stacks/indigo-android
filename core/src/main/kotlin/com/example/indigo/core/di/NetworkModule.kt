package com.example.indigo.core.di

import com.example.indigo.core.account.AccountRepository
import com.example.indigo.core.account.PlaceholderAccountRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideAccountRepository(implementation: PlaceholderAccountRepository): AccountRepository =
        implementation

    @Provides
    @Singleton
    fun provideApiJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
}
