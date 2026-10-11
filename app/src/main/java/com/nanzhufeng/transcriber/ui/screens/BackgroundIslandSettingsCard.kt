package com.nanzhufeng.transcriber.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nanzhufeng.transcriber.ui.components.SectionHeading
import com.nanzhufeng.transcriber.ui.components.SubtleActionButton
import com.nanzhufeng.transcriber.ui.components.WorkbenchCard

@Composable
internal fun BackgroundIslandSettingsCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var overlayAllowed by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var error by remember { mutableStateOf<String?>(null) }
    val refresh = {
        overlayAllowed = Settings.canDrawOverlays(context)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { refresh() }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    fun open(intent: Intent) {
        runCatching { launcher.launch(intent) }.onFailure { error = "无法打开系统页面，请到系统设置中手动授权" }
    }
    WorkbenchCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeading(title = "灵动岛")
            Text(if (overlayAllowed) "顶部灵动岛：已授权" else "顶部灵动岛：待授权")
            SubtleActionButton(onClick = {
                open(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
            }, modifier = Modifier.fillMaxWidth()) { Text(if (overlayAllowed) "管理灵动岛权限" else "允许显示顶部灵动岛") }
            SubtleActionButton(onClick = { open(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }, modifier = Modifier.fillMaxWidth()) { Text("设置后台电池策略") }
            SubtleActionButton(onClick = {
                open(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            }, modifier = Modifier.fillMaxWidth()) { Text("管理转写通知") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
