package com.example.service

import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ReadableNotification(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val postTime: Long
)

class JarvisNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isConnected.value = true
        refreshActiveNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isConnected.value = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        refreshActiveNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        refreshActiveNotifications()
    }

    private fun refreshActiveNotifications() {
        try {
            val sbns = activeNotifications ?: return
            val pm = applicationContext.packageManager
            val parsed = sbns.mapNotNull { sbn ->
                if (sbn.packageName == applicationContext.packageName) return@mapNotNull null
                val extras = sbn.notification.extras
                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
                val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
                    ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
                if (title.isBlank() && text.isBlank()) return@mapNotNull null

                val appLabel = try {
                    val appInfo = pm.getApplicationInfo(sbn.packageName, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (_: PackageManager.NameNotFoundException) {
                    sbn.packageName.substringAfterLast('.')
                }

                ReadableNotification(
                    key = sbn.key,
                    packageName = sbn.packageName,
                    appName = appLabel,
                    title = title,
                    text = text,
                    postTime = sbn.postTime
                )
            }.sortedByDescending { it.postTime }.take(15)

            _notifications.value = parsed
        } catch (_: Exception) {
            // Ignore security exceptions when listener is not bound
        }
    }

    companion object {
        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        private val _notifications = MutableStateFlow<List<ReadableNotification>>(emptyList())
        val notifications: StateFlow<List<ReadableNotification>> = _notifications.asStateFlow()

        fun isNotificationAccessGranted(context: Context): Boolean {
            return NotificationManagerCompat
                .getEnabledListenerPackages(context)
                .contains(context.packageName)
        }
    }
}
