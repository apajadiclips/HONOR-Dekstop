package com.honorhybrid.desktop.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Small persisted settings, exposed as Compose state. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("hybrid_desktop", Context.MODE_PRIVATE)

    var taskbar by mutableStateOf(sp.getString("taskbar", "")!!.split('|').filter { it.isNotBlank() })
        private set
    var desktop by mutableStateOf(sp.getString("desktop", "")!!.split('|').filter { it.isNotBlank() })
        private set
    var wallpaperId by mutableStateOf(sp.getString("wallpaper", "aurora")!!)
        private set
    var customWallpaper by mutableStateOf(sp.getString("custom_wallpaper", null))
        private set
    var zoneOverrides by mutableStateOf(
        sp.getString("zones", "")!!.split('|').filter { '=' in it }
            .associate { it.substringBefore('=') to it.substringAfter('=') }
    )
        private set
    var initialized by mutableStateOf(sp.getBoolean("initialized", false))
        private set

    private fun save(key: String, list: List<String>) = sp.edit().putString(key, list.joinToString("|")).apply()

    fun toggleTaskbar(key: String) {
        taskbar = if (key in taskbar) taskbar - key else (taskbar + key).take(10)
        save("taskbar", taskbar)
    }

    fun toggleDesktop(key: String) {
        desktop = if (key in desktop) desktop - key else desktop + key
        save("desktop", desktop)
    }

    fun setWallpaper(id: String, customUri: String? = customWallpaper) {
        wallpaperId = id
        customWallpaper = customUri
        sp.edit().putString("wallpaper", id).putString("custom_wallpaper", customUri).apply()
    }

    /** value: Zone name, or "NONE" to remove an app from every zone. */
    fun setZone(pkg: String, value: String?) {
        zoneOverrides = if (value == null) zoneOverrides - pkg else zoneOverrides + (pkg to value)
        sp.edit().putString("zones", zoneOverrides.entries.joinToString("|") { "${it.key}=${it.value}" }).apply()
    }

    fun zoneOf(app: AppEntry): Zone? = when (val o = zoneOverrides[app.packageName]) {
        null -> app.autoZone
        "NONE" -> null
        else -> runCatching { Zone.valueOf(o) }.getOrNull()
    }

    /** First run: pin a few common apps so the taskbar isn't empty. */
    fun seedDefaults(apps: List<AppEntry>) {
        if (initialized) return
        val wanted = listOf("files", "filemanager", "chrome", "browser", "settings", "camera", "gallery", "photos")
        val picks = wanted.mapNotNull { w -> apps.firstOrNull { w in it.packageName.lowercase() } }.distinct().take(6)
        taskbar = picks.map { it.key }
        desktop = picks.take(4).map { it.key }
        save("taskbar", taskbar); save("desktop", desktop)
        initialized = true
        sp.edit().putBoolean("initialized", true).apply()
    }
}
