package com.teena.assistant.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureKeyStore @Inject constructor(
    @ApplicationContext private val context: Context
) : KeyVault {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun saveKey(slotId: String, rawKey: String) {
        prefs.edit().putString(keyPrefName(slotId), rawKey.trim()).apply()
    }

    override fun getKey(slotId: String): String? =
        prefs.getString(keyPrefName(slotId), null)?.takeIf { it.isNotBlank() }

    override fun deleteKey(slotId: String) {
        prefs.edit().remove(keyPrefName(slotId)).apply()
    }

    private fun keyPrefName(slotId: String): String = "api_key_$slotId"

    private companion object {
        const val PREFS_FILE_NAME = "teena_keystore_vault"
    }
}
