package ru.family.rasti.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureTokenStore(
    private val context: Context,
    prefsName: String = "github_secret",
    private val alias: String = "rasti.github.token",
) {
    private val preferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    fun save(token: String) {
        if (token.isBlank()) {
            preferences.edit { clear() }
            return
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        preferences.edit {
            putString("value", Base64.encodeToString(cipher.doFinal(token.toByteArray()), Base64.NO_WRAP))
            putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
        }
    }

    fun load(): String {
        val value = preferences.getString("value", null) ?: return ""
        val iv = preferences.getString("iv", null) ?: return ""
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                key(),
                GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)),
            )
            String(cipher.doFinal(Base64.decode(value, Base64.NO_WRAP)))
        }.getOrDefault("")
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }
}
