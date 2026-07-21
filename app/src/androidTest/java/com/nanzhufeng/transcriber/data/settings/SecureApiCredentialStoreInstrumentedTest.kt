package com.nanzhufeng.transcriber.data.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecureApiCredentialStoreInstrumentedTest {
    @Test
    fun apiKeyRoundTripIsEncryptedAndCanBeCleared() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = SecureApiCredentialStore(context)
        val testKey = "test-only-key-${System.nanoTime()}"

        store.saveApiKey(testKey)
        assertEquals(testKey, store.readApiKey())
        val rawPreferences = context
            .getSharedPreferences("secure-api-credentials", android.content.Context.MODE_PRIVATE)
            .all
            .values
            .joinToString("|")
        assertFalse(rawPreferences.contains(testKey))

        store.clearApiKey()
        assertNull(store.readApiKey())
    }
}
