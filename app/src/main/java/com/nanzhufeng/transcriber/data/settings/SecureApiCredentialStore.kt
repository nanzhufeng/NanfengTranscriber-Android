package com.nanzhufeng.transcriber.data.settings

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureApiCredentialStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun saveApiKey(value: String) {
        val apiKey = value.trim()
        require(apiKey.isNotEmpty()) { "API Key 不能为空" }
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        }
        val encrypted = cipher.doFinal(apiKey.toByteArray(StandardCharsets.UTF_8))
        preferences.edit()
            .putString(ENCRYPTED_KEY, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(IV_KEY, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .apply()
    }

    fun readApiKey(): String? = runCatching {
        val encrypted = preferences.getString(ENCRYPTED_KEY, null)?.let {
            Base64.decode(it, Base64.NO_WRAP)
        } ?: return null
        val iv = preferences.getString(IV_KEY, null)?.let {
            Base64.decode(it, Base64.NO_WRAP)
        } ?: return null
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        }
        cipher.doFinal(encrypted).toString(StandardCharsets.UTF_8).trim().ifBlank { null }
    }.getOrNull()

    /**
     * 设置页只需要知道凭据是否存在，不应为了画一行状态而解密 Key。
     * 明文仅在用户点按显隐控制，或实际发起已授权的转写请求时读取。
     */
    fun hasApiKey(): Boolean =
        preferences.contains(ENCRYPTED_KEY) && preferences.contains(IV_KEY)

    fun clearApiKey() {
        preferences.edit().remove(ENCRYPTED_KEY).remove(IV_KEY).apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "secure-api-credentials"
        const val ENCRYPTED_KEY = "encrypted_api_key"
        const val IV_KEY = "encrypted_api_key_iv"
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "nanfeng-transcriber-text-api"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
