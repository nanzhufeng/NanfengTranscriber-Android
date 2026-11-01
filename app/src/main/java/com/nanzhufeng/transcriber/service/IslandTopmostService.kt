package com.nanzhufeng.transcriber.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager

/** User-enabled window host only. No screen retrieval, event subscription, gestures or key filtering. */
class IslandTopmostService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply { eventTypes = 0 }
        connected = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    private fun disconnect() {
        if (connected === this) connected = null
    }

    override fun onUnbind(intent: Intent?): Boolean {
        disconnect()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        disconnect()
        super.onDestroy()
    }

    companion object {
        // Both services live in :transcription. Never pass this window token across processes.
        @Volatile internal var connected: IslandTopmostService? = null
            private set

        fun isEnabled(context: Context): Boolean {
            val component = ComponentName(context, IslandTopmostService::class.java)
            return context.getSystemService(AccessibilityManager::class.java)
                .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .any {
                    val service = it.resolveInfo.serviceInfo
                    ComponentName(service.packageName, service.name) == component
                }
        }
    }
}
