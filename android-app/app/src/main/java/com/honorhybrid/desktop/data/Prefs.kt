package com.honorhybrid.desktop.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/** One thing on the desktop: an app or a folder, at a grid cell. */
data class DesktopItem(
    val id: String,
    val col: Int,
    val row: Int,
    val appKey: String? = null,          // set for an app
    val name: String = "",               // folder name
    val apps: List<String> = emptyList() // folder contents (app keys)
) {
    val isFolder get() = appKey == null
}

enum class ClockPos { ATAS, BAWAH }
enum class IconSize(val label: String, val dp: Int) { KECIL("Kecil", 44), SEDANG("Sedang", 54), BESAR("Besar", 66) }
enum class HomeMode(val label: String) { DESKTOP("Desktop"), TABLET("Tablet (gaya MagicOS)") }

/** Small persisted settings, exposed as Compose state. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("hybrid_desktop", Context.MODE_PRIVATE)

    var taskbar by mutableStateOf(sp.getString("taskbar", "")!!.split('|').filter { it.isNotBlank() })
        private set
    var items by mutableStateOf(loadItems())
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
    var clockPos by mutableStateOf(enumOr(sp.getString("clock", null), ClockPos.ATAS))
        private set
    var iconSize by mutableStateOf(enumOr(sp.getString("icon_size", null), IconSize.SEDANG))
        private set
    var mode by mutableStateOf(enumOr(sp.getString("mode", null), HomeMode.DESKTOP))
        private set
    var hideStatusBar by mutableStateOf(sp.getBoolean("hide_status", false))
        private set
    var swipePanel by mutableStateOf(sp.getBoolean("swipe_panel", true))
        private set
    var showZoneBar by mutableStateOf(sp.getBoolean("zone_bar", true))
        private set
    private var initialized = sp.getBoolean("initialized", false)

    /** Current desktop grid size, reported by the desktop so new icons land on a visible cell. */
    var gridCols = 6
    var gridRows = 5

    private inline fun <reified E : Enum<E>> enumOr(v: String?, d: E): E =
        runCatching { enumValueOf<E>(v!!) }.getOrDefault(d)

    fun setClock(v: ClockPos) { clockPos = v; sp.edit().putString("clock", v.name).apply() }
    fun setIcon(v: IconSize) { iconSize = v; sp.edit().putString("icon_size", v.name).apply() }
    fun setHomeMode(v: HomeMode) { mode = v; sp.edit().putString("mode", v.name).apply() }
    fun setHideStatus(v: Boolean) { hideStatusBar = v; sp.edit().putBoolean("hide_status", v).apply() }
    fun setSwipe(v: Boolean) { swipePanel = v; sp.edit().putBoolean("swipe_panel", v).apply() }
    fun setZoneBar(v: Boolean) { showZoneBar = v; sp.edit().putBoolean("zone_bar", v).apply() }

    fun toggleTaskbar(key: String) {
        taskbar = if (key in taskbar) taskbar - key else (taskbar + key).take(10)
        sp.edit().putString("taskbar", taskbar.joinToString("|")).apply()
    }

    // ---- desktop items

    fun onDesktop(key: String) = items.any { it.appKey == key || key in it.apps }

    fun toggleDesktop(key: String, cols: Int = gridCols, rows: Int = gridRows) {
        if (onDesktop(key)) {
            saveItems(items.mapNotNull { i ->
                when {
                    i.appKey == key -> null
                    key in i.apps -> i.copy(apps = i.apps - key)
                    else -> i
                }
            })
        } else {
            val (c, r) = freeCell(cols, rows)
            saveItems(items + DesktopItem(newId(), c, r, appKey = key))
        }
    }

    fun freeCell(cols: Int, rows: Int): Pair<Int, Int> {
        val used = items.map { it.col to it.row }.toSet()
        for (c in 0 until cols) for (r in 0 until rows) if ((c to r) !in used) return c to r
        return 0 to (items.maxOfOrNull { it.row } ?: 0) + 1
    }

    /** Move an item to a cell. Dropping an app onto an app makes a folder; onto a folder adds it. */
    fun move(id: String, col: Int, row: Int) {
        val moving = items.firstOrNull { it.id == id } ?: return
        val target = items.firstOrNull { it.col == col && it.row == row && it.id != id }
        when {
            target == null -> saveItems(items.map { if (it.id == id) it.copy(col = col, row = row) else it })
            moving.isFolder -> { // swap places with whatever is there
                saveItems(items.map {
                    when (it.id) { id -> it.copy(col = col, row = row); target.id -> it.copy(col = moving.col, row = moving.row); else -> it }
                })
            }
            target.isFolder -> saveItems(items.filter { it.id != id }
                .map { if (it.id == target.id) it.copy(apps = (it.apps + moving.appKey!!).distinct()) else it })
            else -> saveItems(items.filter { it.id != id && it.id != target.id } +
                DesktopItem(newId(), col, row, name = "Folder", apps = listOf(target.appKey!!, moving.appKey!!)))
        }
    }

    fun renameFolder(id: String, name: String) =
        saveItems(items.map { if (it.id == id) it.copy(name = name.take(30)) else it })

    /** Take an app out of a folder and put it back on the desktop. Empty folders disappear. */
    fun removeFromFolder(folderId: String, key: String, cols: Int, rows: Int) {
        val f = items.firstOrNull { it.id == folderId } ?: return
        val rest = f.apps - key
        saveItems(items.mapNotNull { if (it.id == folderId) (if (rest.isEmpty()) null else it.copy(apps = rest)) else it })
        val (c, r) = freeCell(cols, rows)
        saveItems(items + DesktopItem(newId(), c, r, appKey = key))
    }

    private fun newId() = "i" + System.nanoTime().toString(36)

    private fun saveItems(list: List<DesktopItem>) {
        items = list.filter { !it.isFolder || it.apps.isNotEmpty() }
        val arr = JSONArray()
        items.forEach { i ->
            arr.put(JSONObject().put("id", i.id).put("c", i.col).put("r", i.row).put("app", i.appKey ?: "")
                .put("name", i.name).put("apps", JSONArray(i.apps)))
        }
        sp.edit().putString("items", arr.toString()).apply()
    }

    private fun loadItems(): List<DesktopItem> = runCatching {
        val arr = JSONArray(sp.getString("items", "[]"))
        (0 until arr.length()).map { n ->
            val o = arr.getJSONObject(n)
            val apps = o.optJSONArray("apps") ?: JSONArray()
            DesktopItem(o.getString("id"), o.getInt("c"), o.getInt("r"), o.getString("app").ifBlank { null },
                o.optString("name"), (0 until apps.length()).map { apps.getString(it) })
        }
    }.getOrDefault(emptyList()).ifEmpty {
        // migrate v0.1 desktop list
        sp.getString("desktop", "")!!.split('|').filter { it.isNotBlank() }
            .mapIndexed { i, k -> DesktopItem("m$i", i % 2, i / 2, appKey = k) }
    }

    // ---- wallpaper & zones

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
        sp.edit().putString("taskbar", taskbar.joinToString("|")).apply()
        if (items.isEmpty()) saveItems(picks.take(4).mapIndexed { i, a -> DesktopItem("s$i", i % 2, i / 2, appKey = a.key) })
        initialized = true
        sp.edit().putBoolean("initialized", true).apply()
    }
}
