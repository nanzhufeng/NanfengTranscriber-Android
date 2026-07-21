package com.nanzhufeng.transcriber

import android.app.Application
import androidx.room.Room
import com.nanzhufeng.transcriber.data.modelstore.AndroidModelDocumentGateway
import com.nanzhufeng.transcriber.data.modelstore.FileModelStore
import com.nanzhufeng.transcriber.data.modelstore.ModelAssetManager
import com.nanzhufeng.transcriber.data.postprocess.OpenAiCompatibleTextPostProcessor
import com.nanzhufeng.transcriber.data.task.RoomTranscriptionTaskRepository
import com.nanzhufeng.transcriber.data.task.TranscriptionDatabase
import com.nanzhufeng.transcriber.data.task.MIGRATION_1_2
import com.nanzhufeng.transcriber.data.task.MIGRATION_2_3
import com.nanzhufeng.transcriber.data.task.MIGRATION_3_4
import com.nanzhufeng.transcriber.data.settings.TranscriptionSettingsRepository
import com.nanzhufeng.transcriber.data.settings.SecureApiCredentialStore
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
        val taskDatabase = Room.databaseBuilder(
            applicationContext,
            TranscriptionDatabase::class.java,
            "nanfeng-transcriber.db",
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .enableMultiInstanceInvalidation()
            .build()
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
            textPostProcessor = OpenAiCompatibleTextPostProcessor(httpClient),
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
    val textPostProcessor: OpenAiCompatibleTextPostProcessor,
)
