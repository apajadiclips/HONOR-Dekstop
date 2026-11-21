package com.honorhybrid.desktop.service

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

data class GlassAction(val title: String, val intent: PendingIntent, val remoteInput: RemoteInput?)

data class GlassNotification(
    val key: String,
    val packageName: String,
    val title: String,
    val text: String,
    val postTime: Long,
    val clearable: Boolean,
    val contentIntent: PendingIntent?,
    val actions: List<GlassAction>,
) {
    val replyAction get() = actions.firstOrNull { it.remoteInput != null }
}

/** Mirrors the system's active notifications so the launcher can show them as glass cards. */
class GlassNotificationService : NotificationListenerService() {

    override fun onListenerConnected() { instance = this; refresh() }
    override fun onListenerDisconnected() { if (instance === this) instance = null; _items.value = emptyList() }
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refresh()
        if (sbn != null && !sbn.isOngoing) toGlass(sbn)?.let { _posted.tryEmit(it) }
    }

    private fun toGlass(sbn: StatusBarNotification): GlassNotification? {
        val n = sbn.notification
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return null
        val ex = n.extras
        val title = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (ex.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: ex.getCharSequence(Notification.EXTRA_TEXT))
            ?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return null
        val actions = n.actions.orEmpty().mapNotNull { a ->
            val t = a.title?.toString() ?: return@mapNotNull null
            GlassAction(t, a.actionIntent ?: return@mapNotNull null, a.remoteInputs?.firstOrNull())
        }.take(3)
        return GlassNotification(sbn.key, sbn.packageName, title, text, sbn.postTime, sbn.isClearable,
            n.contentIntent, actions)
    }

    private fun refresh() {
        val list = runCatching { activeNotifications?.toList() }.getOrNull().orEmpty()
        _items.value = list.mapNotNull { toGlass(it) }.sortedByDescending { it.postTime }.take(50)
    }

    companion object {
        private val _items = MutableStateFlow<List<GlassNotification>>(emptyList())
        val items: StateFlow<List<GlassNotification>> = _items
        private val _posted = MutableSharedFlow<GlassNotification>(extraBufferCapacity = 4)
        /** New notifications as they arrive (for the toast at the top). */
        val posted: SharedFlow<GlassNotification> = _posted
        @Volatile private var instance: GlassNotificationService? = null

        fun dismiss(key: String) { runCatching { instance?.cancelNotification(key) } }
        fun dismissAll() { runCatching { instance?.cancelAllNotifications() } }

        fun run(c: Context, a: GlassAction) = runCatching { a.intent.send() }.isSuccess

        /** Inline reply ("Balas") through the app's own reply action. */
        fun reply(c: Context, a: GlassAction, text: String): Boolean = runCatching {
            val ri = a.remoteInput ?: return false
            val intent = Intent()
            RemoteInput.addResultsToIntent(arrayOf(ri), intent, Bundle().apply { putCharSequence(ri.resultKey, text) })
            a.intent.send(c, 0, intent)
        }.isSuccess
    }
}
