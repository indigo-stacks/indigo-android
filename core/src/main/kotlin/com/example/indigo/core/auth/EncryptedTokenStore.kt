package com.example.indigo.core.auth

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

private val Context.tokenDataStore by preferencesDataStore(name = "credentials")

/** Only ciphertext enters DataStore. The AEAD keyset is wrapped by an Android Keystore key. */
@Singleton
class EncryptedTokenStore
@Inject
constructor(@ApplicationContext private val context: Context, private val json: Json) : TokenStore {
    private val key = stringPreferencesKey("tokens")
    private val aad = "indigo.credentials.v1".toByteArray()
    private val aead: Aead by lazy {
        AeadConfig.register()
        AndroidKeysetManager.Builder()
            .withSharedPref(context, "keyset", "credential_keys")
            .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
            .withMasterKeyUri("android-keystore://indigo_credentials")
            .build()
            .keysetHandle
            .getPrimitive(Aead::class.java)
    }
    override val tokens =
        context.tokenDataStore.data.map { prefs ->
            prefs[key]?.let { encoded ->
                json.decodeFromString<AuthTokens>(
                    String(
                        aead.decrypt(Base64.decode(encoded, Base64.NO_WRAP), aad),
                        Charsets.UTF_8,
                    )
                )
            }
        }

    override suspend fun save(tokens: AuthTokens): Unit =
        withContext(Dispatchers.IO) {
            val encrypted = aead.encrypt(json.encodeToString(tokens).toByteArray(), aad)
            context.tokenDataStore.edit {
                it[key] = Base64.encodeToString(encrypted, Base64.NO_WRAP)
            }
        }

    override suspend fun clear() {
        context.tokenDataStore.edit { it.remove(key) }
    }
}
