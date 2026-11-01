package com.nanzhufeng.transcriber.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** 两侧等量留位，返回按钮不影响标题相对于整个内容区居中。 */
@Composable
fun SettingsPageHeader(title: String, onBack: (() -> Unit)? = null) {
    WorkbenchPageHeader(title = title, onBack = onBack)
}

@Composable
fun WorkbenchPageHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    closeLabel: String = "关闭",
    titleColor: Color = Color(0xFF242424),
) {
    Box(Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
        Text(
            text = title,
            modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 56.dp, vertical = 8.dp),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = titleColor,
        )
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Outlined.ChevronLeft, contentDescription = "返回设置", tint = Color(0xFF555555))
            }
        }
        if (onClose != null) {
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Outlined.Close, contentDescription = closeLabel, tint = titleColor)
            }
        }
    }
}
