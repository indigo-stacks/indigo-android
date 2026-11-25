package com.example.indigo.core.di

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.example.indigo.core.BuildConfig
import com.example.indigo.core.account.*
import com.example.indigo.core.network.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class PublicClient

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class AuthenticatedClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideAccountRepository(implementation: LiveAccountRepository): AccountRepository =
        implementation

    @Provides
    @Singleton
    fun provideApiJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    @PublicClient
    fun publicClient(@ApplicationContext context: Context): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC)
                    )
                    addInterceptor(ChuckerInterceptor.Builder(context).build())
                }
            }
            .build()

    @Provides
    @Singleton
    @AuthenticatedClient
    fun authenticatedClient(
        @PublicClient client: OkHttpClient,
        bearer: BearerInterceptor,
        authenticator: TokenAuthenticator,
    ): OkHttpClient =
        client
            .newBuilder()
            // A refresh must not queue behind authenticated calls blocked in the authenticator.
            .dispatcher(okhttp3.Dispatcher())
            .addInterceptor(bearer)
            .authenticator(authenticator)
            .build()

    private fun retrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL.trimEnd('/') + "/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun authApi(@PublicClient client: OkHttpClient, json: Json): AuthApi =
        retrofit(client, json).create(AuthApi::class.java)

    @Provides
    @Singleton
    fun accountApi(@AuthenticatedClient client: OkHttpClient, json: Json): AccountApi =
        retrofit(client, json).create(AccountApi::class.java)
}
