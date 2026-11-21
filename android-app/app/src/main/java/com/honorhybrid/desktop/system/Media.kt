package com.honorhybrid.desktop.system

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import com.honorhybrid.desktop.service.GlassNotificationService

data class NowPlaying(val pkg: String, val title: String, val artist: String, val art: Bitmap?, val playing: Boolean)

/** "Sedang dimainkan": works once notification access is granted (Android ties media sessions to it). */
object Media {
    private fun controllers(c: Context): List<MediaController> = runCatching {
        c.getSystemService(MediaSessionManager::class.java)
            .getActiveSessions(ComponentName(c, GlassNotificationService::class.java))
    }.getOrDefault(emptyList())

    private fun main(c: Context) = controllers(c).let { list ->
        list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: list.firstOrNull()
    }

    fun now(c: Context): NowPlaying? = main(c)?.let { mc ->
        val md = mc.metadata
        val title = md?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: return@let null
        NowPlaying(mc.packageName, title,
            md.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: md.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: "",
            md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: md.getBitmap(MediaMetadata.METADATA_KEY_ART),
            mc.playbackState?.state == PlaybackState.STATE_PLAYING)
    }

    fun playPause(c: Context) = main(c)?.let {
        if (it.playbackState?.state == PlaybackState.STATE_PLAYING) it.transportControls.pause() else it.transportControls.play()
    }
    fun next(c: Context) = main(c)?.transportControls?.skipToNext()
    fun prev(c: Context) = main(c)?.transportControls?.skipToPrevious()
    fun pauseAll(c: Context) = controllers(c).forEach { runCatching { it.transportControls.pause() } }

    // ---- sleep timer

    private fun pi(c: Context) = PendingIntent.getBroadcast(c, 7, Intent(c, SleepReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    /** minutes = 0 cancels. Returns the wall-clock end time, or 0. */
    fun sleepTimer(c: Context, minutes: Int): Long {
        val am = c.getSystemService(AlarmManager::class.java)
        am.cancel(pi(c))
        if (minutes <= 0) return 0
        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + minutes * 60_000L, pi(c))
        return System.currentTimeMillis() + minutes * 60_000L
    }
}

class SleepReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Media.pauseAll(context)
        context.getSharedPreferences("hybrid_desktop", Context.MODE_PRIVATE).edit().putLong("sleep_end", 0).apply()
    }
}
