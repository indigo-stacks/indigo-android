package com.example.indigo.core.di

import com.example.indigo.core.auth.EncryptedTokenStore
import com.example.indigo.core.auth.LiveSessionRepository
import com.example.indigo.core.auth.SessionRepository
import com.example.indigo.core.auth.TokenStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds
    @Singleton
    abstract fun bindSessionRepository(implementation: LiveSessionRepository): SessionRepository

    @Binds @Singleton abstract fun bindTokenStore(implementation: EncryptedTokenStore): TokenStore
}
