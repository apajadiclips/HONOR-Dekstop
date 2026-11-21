package com.honorhybrid.desktop.service

import android.app.Notification
import android.app.PendingIntent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class GlassNotification(
    val key: String,
    val packageName: String,
    val title: String,
    val text: String,
    val postTime: Long,
    val clearable: Boolean,
    val contentIntent: PendingIntent?,
)

/** Mirrors the system's active notifications so the launcher can show them as glass cards. */
class GlassNotificationService : NotificationListenerService() {

    override fun onListenerConnected() { instance = this; refresh() }
    override fun onListenerDisconnected() { if (instance === this) instance = null; _items.value = emptyList() }
    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    private fun refresh() {
        val list = runCatching { activeNotifications?.toList() }.getOrNull().orEmpty()
        _items.value = list
            .filter { it.notification.flags and Notification.FLAG_GROUP_SUMMARY == 0 }
            .mapNotNull { sbn ->
                val ex = sbn.notification.extras
                val title = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
                val text = (ex.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: ex.getCharSequence(Notification.EXTRA_TEXT))
                    ?.toString().orEmpty()
                if (title.isBlank() && text.isBlank()) null
                else GlassNotification(sbn.key, sbn.packageName, title, text, sbn.postTime, sbn.isClearable,
                    sbn.notification.contentIntent)
            }
            .sortedByDescending { it.postTime }
            .take(40)
    }

    companion object {
        private val _items = MutableStateFlow<List<GlassNotification>>(emptyList())
        val items: StateFlow<List<GlassNotification>> = _items
        @Volatile private var instance: GlassNotificationService? = null

        fun dismiss(key: String) { runCatching { instance?.cancelNotification(key) } }
        fun dismissAll() { runCatching { instance?.cancelAllNotifications() } }
    }
}
