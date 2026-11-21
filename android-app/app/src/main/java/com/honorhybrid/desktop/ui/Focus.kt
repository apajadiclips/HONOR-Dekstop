package com.honorhybrid.desktop.ui

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

/** Real Do-Not-Disturb toggle used by Mod Teater / Mod Game / Mod Fokus. */
object Focus {
    private fun nm(c: Context) = c.getSystemService(NotificationManager::class.java)

    fun hasAccess(c: Context) = nm(c).isNotificationPolicyAccessGranted

    fun isOn(c: Context) = nm(c).currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL

    /** Returns false (and opens the permission screen) when access hasn't been granted yet. */
    fun toggle(c: Context): Boolean {
        if (!hasAccess(c)) {
            Actions.start(c, Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)); return false
        }
        nm(c).setInterruptionFilter(
            if (isOn(c)) NotificationManager.INTERRUPTION_FILTER_ALL else NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        return true
    }
}
