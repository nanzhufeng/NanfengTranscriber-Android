package com.nanzhufeng.transcriber.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import com.nanzhufeng.transcriber.data.task.OutputConflictPolicy

private val Context.transcriptionSettingsStore by preferencesDataStore(
    name = "transcription-settings",
)

class TranscriptionSettingsRepository(
    private val context: Context,
    private val apiCredentials: SecureApiCredentialStore,
) {
    val settings: Flow<TranscriptionSettings> = context.transcriptionSettingsStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map { values ->
            TranscriptionSettings(
                modelId = values[MODEL_ID] ?: DEFAULT_MODEL,
                languageCode = values[LANGUAGE_CODE].orEmpty().ifBlank { null },
                threadCount = values[THREAD_COUNT]?.coerceIn(0, 8) ?: 0,
                keepScreenOn = values[KEEP_SCREEN_ON] ?: true,
                skinId = values[SKIN_ID]
                    ?.takeUnless { it == LEGACY_SKIN }
                    ?: DEFAULT_SKIN,
                defaultOutputDirectoryUri = values[OUTPUT_DIRECTORY_URI].orEmpty().ifBlank { null },
                outputConflictPolicy = values[OUTPUT_CONFLICT_POLICY]
                    ?.let { runCatching { OutputConflictPolicy.valueOf(it) }.getOrNull() }
                    ?: OutputConflictPolicy.RENAME,
                postProcessEnabled = values[POST_PROCESS_ENABLED] ?: false,
                postProcessBaseUrl = values[POST_PROCESS_BASE_URL] ?: DEFAULT_POST_PROCESS_BASE_URL,
                postProcessModel = values[POST_PROCESS_MODEL] ?: DEFAULT_POST_PROCESS_MODEL,
                postProcessApiKeyConfigured = apiCredentials.hasApiKey(),
            )
        }

    suspend fun setModelId(value: String) {
        require(value.isNotBlank()) { "模型标识不能为空" }
        context.transcriptionSettingsStore.edit { preferences ->
            preferences[MODEL_ID] = value
        }
    }

    suspend fun setLanguageCode(value: String?) {
        context.transcriptionSettingsStore.edit { preferences ->
            preferences[LANGUAGE_CODE] = value.orEmpty()
        }
    }

    suspend fun setThreadCount(value: Int) {
        require(value == 0 || value in 2..8) { "线程数必须为自动或 2–8" }
        context.transcriptionSettingsStore.edit { preferences ->
            preferences[THREAD_COUNT] = value
        }
    }

    suspend fun setKeepScreenOn(value: Boolean) {
        context.transcriptionSettingsStore.edit { preferences ->
            preferences[KEEP_SCREEN_ON] = value
        }
    }

    suspend fun setSkinId(value: String) {
        require(value.isNotBlank()) { "皮肤标识不能为空" }
        context.transcriptionSettingsStore.edit { preferences ->
            preferences[SKIN_ID] = value
        }
    }

    suspend fun setDefaultOutputDirectoryUri(value: String?) {
        context.transcriptionSettingsStore.edit { preferences ->
            preferences[OUTPUT_DIRECTORY_URI] = value.orEmpty()
        }
    }

    suspend fun setOutputConflictPolicy(value: OutputConflictPolicy) {
        context.transcriptionSettingsStore.edit { preferences ->
            preferences[OUTPUT_CONFLICT_POLICY] = value.name
        }
    }

    suspend fun setPostProcessEnabled(value: Boolean) {
        require(!value || apiCredentials.hasApiKey()) { "请先保存翻译润色 API Key" }
        context.transcriptionSettingsStore.edit { preferences ->
            preferences[POST_PROCESS_ENABLED] = value
        }
    }

    suspend fun setPostProcessConnection(baseUrl: String, model: String) {
        val safeBaseUrl = baseUrl.trim().trimEnd('/')
        require(safeBaseUrl.startsWith("https://")) { "服务地址必须使用 HTTPS" }
        require(model.isNotBlank()) { "模型名不能为空" }
        context.transcriptionSettingsStore.edit { preferences ->
            preferences[POST_PROCESS_BASE_URL] = safeBaseUrl
            preferences[POST_PROCESS_MODEL] = model.trim()
        }
    }

    suspend fun savePostProcessApiKey(value: String) {
        apiCredentials.saveApiKey(value)
        context.transcriptionSettingsStore.edit { preferences ->
            preferences[POST_PROCESS_KEY_REVISION] = (preferences[POST_PROCESS_KEY_REVISION] ?: 0) + 1
        }
    }

    suspend fun clearPostProcessApiKey() {
        apiCredentials.clearApiKey()
        context.transcriptionSettingsStore.edit { preferences ->
            preferences[POST_PROCESS_ENABLED] = false
            preferences[POST_PROCESS_KEY_REVISION] = (preferences[POST_PROCESS_KEY_REVISION] ?: 0) + 1
        }
    }

    private companion object {
        const val DEFAULT_SKIN = "forest_maple"
        const val LEGACY_SKIN = "workbench-sage"
        const val DEFAULT_MODEL = "small-q5_1"
        const val DEFAULT_POST_PROCESS_BASE_URL = "https://api.openai.com/v1"
        const val DEFAULT_POST_PROCESS_MODEL = "gpt-4o-mini"
        val MODEL_ID = stringPreferencesKey("model_id")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val THREAD_COUNT = intPreferencesKey("thread_count")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val SKIN_ID = stringPreferencesKey("skin_id")
        val OUTPUT_DIRECTORY_URI = stringPreferencesKey("output_directory_uri")
        val OUTPUT_CONFLICT_POLICY = stringPreferencesKey("output_conflict_policy")
        val POST_PROCESS_ENABLED = booleanPreferencesKey("post_process_enabled")
        val POST_PROCESS_BASE_URL = stringPreferencesKey("post_process_base_url")
        val POST_PROCESS_MODEL = stringPreferencesKey("post_process_model")
        val POST_PROCESS_KEY_REVISION = intPreferencesKey("post_process_key_revision")
    }
}

data class TranscriptionSettings(
    val modelId: String = "small-q5_1",
    val languageCode: String? = null,
    val threadCount: Int = 0,
    val keepScreenOn: Boolean = true,
    val skinId: String = "forest_maple",
    val defaultOutputDirectoryUri: String? = null,
    val outputConflictPolicy: OutputConflictPolicy = OutputConflictPolicy.RENAME,
    val postProcessEnabled: Boolean = false,
    val postProcessBaseUrl: String = "https://api.openai.com/v1",
    val postProcessModel: String = "gpt-4o-mini",
    val postProcessApiKeyConfigured: Boolean = false,
)
