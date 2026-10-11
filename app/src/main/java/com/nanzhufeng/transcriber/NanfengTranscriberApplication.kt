package com.nanzhufeng.transcriber

import android.app.Application
import androidx.room.Room
import com.nanzhufeng.transcriber.data.modelstore.AndroidModelDocumentGateway
import com.nanzhufeng.transcriber.data.modelstore.FileModelStore
import com.nanzhufeng.transcriber.data.modelstore.ModelAssetManager
import com.nanzhufeng.transcriber.data.invocation.RoomAsrInvocationRepository
import com.nanzhufeng.transcriber.data.task.RoomTranscriptionTaskRepository
import com.nanzhufeng.transcriber.data.task.TranscriptionDatabase
import com.nanzhufeng.transcriber.data.task.MIGRATION_1_2
import com.nanzhufeng.transcriber.data.task.MIGRATION_2_3
import com.nanzhufeng.transcriber.data.task.MIGRATION_3_4
import com.nanzhufeng.transcriber.data.task.MIGRATION_4_5
import com.nanzhufeng.transcriber.data.task.MIGRATION_5_6
import com.nanzhufeng.transcriber.data.task.MIGRATION_6_7
import com.nanzhufeng.transcriber.data.settings.TranscriptionSettingsRepository
import com.nanzhufeng.transcriber.data.settings.SecureApiCredentialStore
import com.nanzhufeng.transcriber.engine.Qwen3AsrApiEngine
import com.nanzhufeng.transcriber.engine.DashScopeFileTranscriptionEngine
import com.nanzhufeng.transcriber.engine.DashScopeFileTranscriptionClient
import com.nanzhufeng.transcriber.engine.FileTranscriptionModel
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class NanfengTranscriberApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val modelStore = FileModelStore(noBackupFilesDir.resolve("models").toPath())
        val httpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
        val modelAssets = ModelAssetManager(
            store = modelStore,
            httpClient = httpClient,
        )
        val textApiCredentials = SecureApiCredentialStore(applicationContext)
        val fileClient = DashScopeFileTranscriptionClient(
            textApiCredentials::readApiKey, httpClient, "https://dashscope.aliyuncs.com/api/v1/".toHttpUrl(),
        )
        val taskDatabase = Room.databaseBuilder(
            applicationContext,
            TranscriptionDatabase::class.java,
            "nanfeng-transcriber.db",
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
            .enableMultiInstanceInvalidation()
            .build()
        val asrInvocations = RoomAsrInvocationRepository(taskDatabase.asrInvocationDao())
        appContainer = AppContainer(
            modelStore = modelStore,
            modelAssets = modelAssets,
            modelDocuments = AndroidModelDocumentGateway(
                contentResolver = contentResolver,
                modelAssets = modelAssets,
            ),
            taskDatabase = taskDatabase,
            tasks = RoomTranscriptionTaskRepository(taskDatabase),
            settings = TranscriptionSettingsRepository(applicationContext, textApiCredentials),
            textApiCredentials = textApiCredentials,
            asrInvocations = asrInvocations,
            qwen3AsrEngine = Qwen3AsrApiEngine(
                readApiKey = textApiCredentials::readApiKey,
                client = httpClient,
            ),
            qwenFileEngine = DashScopeFileTranscriptionEngine(
                FileTranscriptionModel.QWEN_AUDIO_31, fileClient, textApiCredentials::readApiKey,
            ),
            paraformerEngine = DashScopeFileTranscriptionEngine(
                FileTranscriptionModel.PARAFORMER_V2, fileClient, textApiCredentials::readApiKey,
            ),
        )
    }
}

data class AppContainer(
    val modelStore: FileModelStore,
    val modelAssets: ModelAssetManager,
    val modelDocuments: AndroidModelDocumentGateway,
    val taskDatabase: TranscriptionDatabase,
    val tasks: RoomTranscriptionTaskRepository,
    val settings: TranscriptionSettingsRepository,
    val textApiCredentials: SecureApiCredentialStore,
    val asrInvocations: RoomAsrInvocationRepository,
    val qwen3AsrEngine: Qwen3AsrApiEngine,
    val qwenFileEngine: DashScopeFileTranscriptionEngine,
    val paraformerEngine: DashScopeFileTranscriptionEngine,
)
