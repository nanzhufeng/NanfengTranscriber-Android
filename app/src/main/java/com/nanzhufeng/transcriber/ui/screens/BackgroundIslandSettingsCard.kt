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
import com.nanzhufeng.transcriber.service.IslandTopmostService

@Composable
internal fun BackgroundIslandSettingsCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var overlayAllowed by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var topmostEnabled by remember { mutableStateOf(IslandTopmostService.isEnabled(context)) }
    var error by remember { mutableStateOf<String?>(null) }
    val refresh = {
        overlayAllowed = Settings.canDrawOverlays(context)
        topmostEnabled = IslandTopmostService.isEnabled(context)
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
            Text(if (topmostEnabled) "状态栏置顶：已开启" else "状态栏置顶：未开启")
            Text("在系统无障碍中开启“南枫转写灵动岛置顶”。仅显示转写状态，不读取屏幕或执行手势；未开启时普通悬浮窗显示在状态栏下方。",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SubtleActionButton(onClick = {
                open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }, modifier = Modifier.fillMaxWidth()) { Text(if (topmostEnabled) "管理状态栏置顶" else "开启状态栏置顶") }
            Text(if (overlayAllowed) "普通悬浮窗：已授权" else "普通悬浮窗：待授权")
            SubtleActionButton(onClick = {
                open(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
            }, modifier = Modifier.fillMaxWidth()) { Text(if (overlayAllowed) "管理普通悬浮窗" else "允许普通悬浮窗") }
            SubtleActionButton(onClick = { open(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }, modifier = Modifier.fillMaxWidth()) { Text("设置后台电池策略") }
            SubtleActionButton(onClick = {
                open(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            }, modifier = Modifier.fillMaxWidth()) { Text("管理转写通知") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
