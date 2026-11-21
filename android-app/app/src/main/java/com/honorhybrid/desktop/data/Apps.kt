package com.honorhybrid.desktop.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

enum class Zone(val label: String, val short: String) {
    GAME("Game", "Game"), SOSIAL("Media Sosial", "Sosial"), HIBURAN("Hiburan", "Hiburan"),
    KERJA("Kerja", "Kerja"), KREATIF("Kreatif", "Kreatif"), ALAT("Alat", "Alat"),
}

data class AppEntry(
    val packageName: String,
    val activity: String,
    val label: String,
    val icon: ImageBitmap,
    val autoZone: Zone?,
    val isSystem: Boolean,
) {
    val key: String get() = "$packageName/$activity"
    val component: ComponentName get() = ComponentName(packageName, activity)
}

private val SOCIAL_HINTS = listOf(
    "whatsapp", "telegram", "facebook", "instagram", "musically", "tiktok", "twitter",
    "messenger", "discord", "snapchat", "threads", "linkedin", "reddit", "wechat", "line.android",
    "pinterest", "signal", "viber", "orca", "katana",
)
private val MEDIA_HINTS = listOf(
    "youtube", "netflix", "spotify", "music", "video", "avod", "disney", "viu", "iqiyi", "joox",
    "podcast", "radio", "player", "twitch", "primevideo", "hbo", "wetv", "tonton",
)

private val CREATIVE_HINTS = listOf(
    "lvoverseas", "capcut", "canva", "picsart", "lightroom", "snapseed", "camera", "gallery", "photo",
    "inshot", "vsco", "kinemaster", "editor", "paint", "draw", "sketch",
)
private val WORK_HINTS = listOf(
    "docs", "sheets", "slides", "office", "word", "excel", "powerpoint", "wps", "notion", "gmail", "email",
    "mail", "calendar", "keep", "drive", "zoom", "teams", "meet", "slack", "translate", "classroom", "pdf", "notes",
)
private val TOOL_HINTS = listOf(
    "files", "filemanager", "calculator", "clock", "settings", "compass", "recorder", "scanner", "vpn",
    "termux", "shizuku", "adguard", "googlequicksearchbox", "contacts", "phonemanager", "systemmanager",
)

fun guessZone(pkg: String, category: Int): Zone? {
    val p = pkg.lowercase()
    return when {
        TOOL_HINTS.any { it in p } -> Zone.ALAT
        CREATIVE_HINTS.any { it in p } || category == ApplicationInfo.CATEGORY_IMAGE -> Zone.KREATIF
        WORK_HINTS.any { it in p } || category == ApplicationInfo.CATEGORY_PRODUCTIVITY -> Zone.KERJA
        category == ApplicationInfo.CATEGORY_GAME || ".game" in p || "games" in p -> Zone.GAME
        category == ApplicationInfo.CATEGORY_SOCIAL || SOCIAL_HINTS.any { it in p } -> Zone.SOSIAL
        category == ApplicationInfo.CATEGORY_VIDEO || category == ApplicationInfo.CATEGORY_AUDIO ||
            MEDIA_HINTS.any { it in p } -> Zone.HIBURAN
        else -> null
    }
}

object AppRepository {
    fun load(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val sizePx = (56 * context.resources.displayMetrics.density).toInt()
        return pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .mapNotNull { ri ->
                runCatching {
                    val ai = ri.activityInfo
                    AppEntry(
                        packageName = ai.packageName,
                        activity = ai.name,
                        label = ri.loadLabel(pm).toString(),
                        icon = ri.loadIcon(pm).toBitmap(sizePx, sizePx).asImageBitmap(),
                        autoZone = guessZone(ai.packageName, ai.applicationInfo.category),
                        isSystem = (ai.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    )
                }.getOrNull()
            }
            .distinctBy { it.key }
            .sortedBy { it.label.lowercase() }
    }
}
