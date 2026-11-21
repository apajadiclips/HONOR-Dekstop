package com.honorhybrid.desktop.system

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.WallpaperManager
import android.app.usage.UsageStatsManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Process
import android.provider.Settings
import java.util.Calendar

/** Read-only device facts and the few settings a normal app may change. Every call is failure-safe. */
object Sys {
    data class Ram(val availMb: Long, val totalMb: Long)

    fun ram(c: Context): Ram {
        val mi = ActivityManager.MemoryInfo()
        c.getSystemService(ActivityManager::class.java).getMemoryInfo(mi)
        return Ram(mi.availMem / 1_048_576, mi.totalMem / 1_048_576)
    }

    fun batteryTempC(c: Context): Float? =
        c.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)?.takeIf { it > 0 }?.let { it / 10f }

    fun charging(c: Context): Boolean =
        (c.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0

    fun refreshRate(c: Context): Int = runCatching { c.display!!.refreshRate.toInt() }.getOrDefault(60)

    fun wifiOn(c: Context) = runCatching { c.getSystemService(WifiManager::class.java).isWifiEnabled }.getOrDefault(false)

    fun bluetoothOn(c: Context) = runCatching {
        c.getSystemService(BluetoothManager::class.java).adapter?.isEnabled == true
    }.getOrDefault(false)

    fun darkMode(c: Context) =
        (c.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    fun airplane(c: Context) = Settings.Global.getInt(c.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1

    /** e.g. "dns.adguard-dns.com", or null when Private DNS is automatic/off (or unreadable). */
    fun privateDns(c: Context): String? = runCatching {
        val mode = Settings.Global.getString(c.contentResolver, "private_dns_mode")
        if (mode == "hostname") Settings.Global.getString(c.contentResolver, "private_dns_specifier") else null
    }.getOrNull()

    fun deviceName(c: Context): String = runCatching {
        Settings.Global.getString(c.contentResolver, "device_name")
    }.getOrNull() ?: android.os.Build.MODEL

    data class PkgStats(val total: Int, val system: Int, val disabled: Int)

    fun packageStats(c: Context): PkgStats = runCatching {
        val all = c.packageManager.getInstalledApplications(PackageManager.MATCH_DISABLED_COMPONENTS)
        PkgStats(all.size, all.count { it.flags and ApplicationInfo.FLAG_SYSTEM != 0 }, all.count { !it.enabled })
    }.getOrDefault(PkgStats(0, 0, 0))

    // ---- usage access (screen time, recently used apps)

    fun hasUsage(c: Context): Boolean {
        val ops = c.getSystemService(AppOpsManager::class.java)
        return ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName) ==
            AppOpsManager.MODE_ALLOWED
    }

    private fun startOfDay(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** Foreground minutes today per package. */
    fun usageToday(c: Context): Map<String, Long> = runCatching {
        c.getSystemService(UsageStatsManager::class.java)
            .queryAndAggregateUsageStats(startOfDay(), System.currentTimeMillis())
            .mapValues { it.value.totalTimeInForeground / 60_000 }.filterValues { it > 0 }
    }.getOrDefault(emptyMap())

    /** Last time each package was used (last 7 days). */
    fun lastUsed(c: Context): Map<String, Long> = runCatching {
        val now = System.currentTimeMillis()
        c.getSystemService(UsageStatsManager::class.java)
            .queryUsageStats(UsageStatsManager.INTERVAL_BEST, now - 7 * 86_400_000L, now)
            .groupBy { it.packageName }.mapValues { e -> e.value.maxOf { it.lastTimeUsed } }.filterValues { it > 0 }
    }.getOrDefault(emptyMap())

    // ---- brightness & volume

    fun canWriteSettings(c: Context) = Settings.System.canWrite(c)

    fun brightness(c: Context): Int =
        Settings.System.getInt(c.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)

    fun setBrightness(c: Context, v: Int) = runCatching {
        Settings.System.putInt(c.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
        Settings.System.putInt(c.contentResolver, Settings.System.SCREEN_BRIGHTNESS, v.coerceIn(1, 255))
    }

    fun autoBrightness(c: Context) = Settings.System.getInt(c.contentResolver,
        Settings.System.SCREEN_BRIGHTNESS_MODE, 0) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC

    fun setAutoBrightness(c: Context, on: Boolean) = runCatching {
        Settings.System.putInt(c.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
            if (on) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
    }

    private fun audio(c: Context) = c.getSystemService(AudioManager::class.java)
    fun volume(c: Context): Float = audio(c).let {
        it.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / it.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    }
    fun setVolume(c: Context, f: Float) = runCatching {
        val a = audio(c)
        a.setStreamVolume(AudioManager.STREAM_MUSIC, (f * a.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).toInt(), 0)
    }

    /** Stops cached background processes of other apps (what Android allows a normal app to do). */
    fun killBackground(c: Context, packages: List<String>): Int {
        val am = c.getSystemService(ActivityManager::class.java)
        packages.forEach { runCatching { am.killBackgroundProcesses(it) } }
        return packages.size
    }

    fun setLockWallpaper(c: Context, bmp: Bitmap) = runCatching {
        WallpaperManager.getInstance(c).setBitmap(bmp, null, true, WallpaperManager.FLAG_LOCK)
    }.isSuccess
}
