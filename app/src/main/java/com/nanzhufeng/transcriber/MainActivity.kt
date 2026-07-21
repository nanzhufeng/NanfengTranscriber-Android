package com.nanzhufeng.transcriber

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nanzhufeng.transcriber.service.TranscriptionForegroundService
import com.nanzhufeng.transcriber.ui.NanfengTranscriberApp
import com.nanzhufeng.transcriber.ui.TranscriptionViewModel
import com.nanzhufeng.transcriber.ui.theme.NanfengTranscriberTheme

class MainActivity : ComponentActivity() {
    private var requestedTaskId by mutableStateOf<String?>(null)
    private var sharedInputRequest by mutableStateOf<SharedInputRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedTaskId = intent.getStringExtra(TranscriptionForegroundService.EXTRA_TASK_ID)
        sharedInputRequest = intent.toSharedInputRequest()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val transcriptionViewModel: TranscriptionViewModel = viewModel()
            val settings by transcriptionViewModel.settings.collectAsStateWithLifecycle()
            NanfengTranscriberTheme(skinId = settings.skinId) {
                NanfengTranscriberApp(
                    viewModel = transcriptionViewModel,
                    notificationTaskId = requestedTaskId,
                    sharedInputRequest = sharedInputRequest,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedTaskId = intent.getStringExtra(TranscriptionForegroundService.EXTRA_TASK_ID)
        sharedInputRequest = intent.toSharedInputRequest()
    }
}

data class SharedInputRequest(
    val token: Long,
    val uris: List<Uri>,
)

@Suppress("DEPRECATION")
private fun Intent.toSharedInputRequest(): SharedInputRequest? {
    if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) return null
    val streams = buildList {
        getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let(::add)
        getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let(::addAll)
        clipData?.let { clip ->
            repeat(clip.itemCount) { index -> clip.getItemAt(index).uri?.let(::add) }
        }
    }.distinctBy(Uri::toString)
    return streams.takeIf(List<Uri>::isNotEmpty)?.let {
        SharedInputRequest(token = System.nanoTime(), uris = it)
    }
}
