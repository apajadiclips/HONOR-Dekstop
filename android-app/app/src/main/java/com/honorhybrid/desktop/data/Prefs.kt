package com.honorhybrid.desktop.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/** One thing on a desktop: an app or a folder, at a grid cell of a virtual desktop. */
data class DesktopItem(
    val id: String,
    val col: Int,
    val row: Int,
    val appKey: String? = null,          // set for an app
    val name: String = "",               // folder name
    val apps: List<String> = emptyList(),// folder contents (app keys)
    val desk: Int = 0,
) {
    val isFolder get() = appKey == null
}

data class Clip(val text: String, val time: Long, val pinned: Boolean)

enum class ClockPos { ATAS, BAWAH }
enum class IconSize(val label: String, val dp: Int) { KECIL("Kecil", 44), SEDANG("Sedang", 54), BESAR("Besar", 66) }
enum class HomeMode(val label: String) { DESKTOP("Desktop"), TABLET("Tablet (gaya MagicOS)") }

/** Persisted launcher state, exposed as Compose state. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("hybrid_desktop", Context.MODE_PRIVATE)

    private fun list(key: String) = sp.getString(key, "")!!.split('|').filter { it.isNotBlank() }
    private fun putList(key: String, v: List<String>) = sp.edit().putString(key, v.joinToString("|")).apply()
    private fun map(key: String) = list(key).filter { '=' in it }.associate { it.substringBefore('=') to it.substringAfter('=') }
    private fun putMap(key: String, m: Map<String, String>) = putList(key, m.map { "${it.key}=${it.value.replace("|", " ")}" })
    private inline fun <reified E : Enum<E>> enumOr(v: String?, d: E): E = runCatching { enumValueOf<E>(v!!) }.getOrDefault(d)

    var taskbar by mutableStateOf(list("taskbar")); private set
    var startPins by mutableStateOf(list("start_pins")); private set
    var recents by mutableStateOf(list("recents")); private set
    var items by mutableStateOf(loadItems()); private set
    var desks by mutableStateOf(list("desks").ifEmpty { listOf("Kerja", "Hiburan") }); private set
    var currentDesk by mutableStateOf(sp.getInt("desk", 0)); private set
    var labels by mutableStateOf(map("labels")); private set
    var zoneOverrides by mutableStateOf(map("zones")); private set
    var zoneOrder by mutableStateOf(map("zone_order")); private set   // zone -> "k1,k2,..."
    var limits by mutableStateOf(map("limits").mapValues { it.value.toIntOrNull() ?: 0 }); private set
    var clips by mutableStateOf(loadClips()); private set

    var wallpaperId by mutableStateOf(sp.getString("wallpaper", "aurora")!!); private set
    var customWallpaper by mutableStateOf(sp.getString("custom_wallpaper", null)); private set
    var deskWallpapers by mutableStateOf(map("desk_wallpapers")); private set // desk index -> preset id
    var perDeskWallpaper by mutableStateOf(sp.getBoolean("per_desk_wp", true)); private set
    var dailyWallpaper by mutableStateOf(sp.getBoolean("daily_wp", false)); private set
    var accentFollow by mutableStateOf(sp.getBoolean("accent_follow", true)); private set
    var glass by mutableStateOf(sp.getInt("glass", 60)); private set
    var dim by mutableStateOf(sp.getInt("dim", 15)); private set

    var clockPos by mutableStateOf(enumOr(sp.getString("clock", null), ClockPos.ATAS)); private set
    var iconSize by mutableStateOf(enumOr(sp.getString("icon_size", null), IconSize.SEDANG)); private set
    var mode by mutableStateOf(enumOr(sp.getString("mode", null), HomeMode.DESKTOP)); private set
    var hideStatusBar by mutableStateOf(sp.getBoolean("hide_status", false)); private set
    var swipePanel by mutableStateOf(sp.getBoolean("swipe_panel", true)); private set
    var showZoneBar by mutableStateOf(sp.getBoolean("zone_bar", true)); private set
    var showDebloatWidget by mutableStateOf(sp.getBoolean("debloat_widget", true)); private set
    var sleepEnd by mutableStateOf(sp.getLong("sleep_end", 0)); private set
    private var initialized = sp.getBoolean("initialized", false)

    /** Current desktop grid size, reported by the desktop so new icons land on a visible cell. */
    var gridCols = 6
    var gridRows = 5

    // ---- simple settings
    fun setClock(v: ClockPos) { clockPos = v; sp.edit().putString("clock", v.name).apply() }
    fun setIcon(v: IconSize) { iconSize = v; sp.edit().putString("icon_size", v.name).apply() }
    fun setHomeMode(v: HomeMode) { mode = v; sp.edit().putString("mode", v.name).apply() }
    fun setHideStatus(v: Boolean) { hideStatusBar = v; sp.edit().putBoolean("hide_status", v).apply() }
    fun setSwipe(v: Boolean) { swipePanel = v; sp.edit().putBoolean("swipe_panel", v).apply() }
    fun setZoneBar(v: Boolean) { showZoneBar = v; sp.edit().putBoolean("zone_bar", v).apply() }
    fun setDebloatWidget(v: Boolean) { showDebloatWidget = v; sp.edit().putBoolean("debloat_widget", v).apply() }
    fun changeGlass(v: Int) { glass = v; sp.edit().putInt("glass", v).apply() }
    fun changeDim(v: Int) { dim = v; sp.edit().putInt("dim", v).apply() }
    fun changeAccentFollow(v: Boolean) { accentFollow = v; sp.edit().putBoolean("accent_follow", v).apply() }
    fun setDaily(v: Boolean) { dailyWallpaper = v; sp.edit().putBoolean("daily_wp", v).apply() }
    fun setPerDesk(v: Boolean) { perDeskWallpaper = v; sp.edit().putBoolean("per_desk_wp", v).apply() }
    fun setSleep(end: Long) { sleepEnd = end; sp.edit().putLong("sleep_end", end).apply() }

    // ---- apps: labels, pins, recents
    fun labelOf(app: AppEntry) = labels[app.key] ?: app.label
    fun setLabel(key: String, name: String?) {
        labels = if (name.isNullOrBlank()) labels - key else labels + (key to name.take(30))
        putMap("labels", labels)
    }

    fun toggleTaskbar(key: String) {
        taskbar = if (key in taskbar) taskbar - key else (taskbar + key).take(10); putList("taskbar", taskbar)
    }

    fun toggleStartPin(key: String) {
        startPins = if (key in startPins) startPins - key else (startPins + key).take(18); putList("start_pins", startPins)
    }

    fun recordLaunch(key: String) {
        recents = (listOf(key) + (recents - key)).take(12); putList("recents", recents)
        sp.edit().putLong("used_$key", System.currentTimeMillis()).apply()
    }

    fun lastLaunch(key: String) = sp.getLong("used_$key", 0)

    // ---- virtual desktops
    fun switchDesk(i: Int) { currentDesk = i.coerceIn(0, desks.size - 1); sp.edit().putInt("desk", currentDesk).apply() }
    fun addDesk() { desks = desks + "Desktop ${desks.size + 1}"; putList("desks", desks); switchDesk(desks.size - 1) }
    fun renameDesk(i: Int, name: String) {
        desks = desks.mapIndexed { n, d -> if (n == i) name.take(20) else d }; putList("desks", desks)
    }
    fun removeDesk(i: Int) {
        if (desks.size <= 1) return
        desks = desks.filterIndexed { n, _ -> n != i }; putList("desks", desks)
        saveItems(items.filter { it.desk != i }.map { if (it.desk > i) it.copy(desk = it.desk - 1) else it })
        switchDesk(currentDesk.coerceAtMost(desks.size - 1))
    }

    // ---- desktop items (current desk)
    val deskItems get() = items.filter { it.desk == currentDesk }

    fun onDesktop(key: String) = deskItems.any { it.appKey == key || key in it.apps }

    fun toggleDesktop(key: String, cols: Int = gridCols, rows: Int = gridRows) {
        if (onDesktop(key)) {
            saveItems(items.mapNotNull { i ->
                when {
                    i.desk != currentDesk -> i
                    i.appKey == key -> null
                    key in i.apps -> i.copy(apps = i.apps - key)
                    else -> i
                }
            })
        } else {
            val (c, r) = freeCell(cols, rows)
            saveItems(items + DesktopItem(newId(), c, r, appKey = key, desk = currentDesk))
        }
    }

    fun freeCell(cols: Int, rows: Int): Pair<Int, Int> {
        val used = deskItems.map { it.col to it.row }.toSet()
        for (c in 0 until cols) for (r in 0 until rows) if ((c to r) !in used) return c to r
        return 0 to (deskItems.maxOfOrNull { it.row } ?: 0) + 1
    }

    /** Move an item to a cell. Dropping an app onto an app makes a folder; onto a folder adds it. */
    fun move(id: String, col: Int, row: Int) {
        val moving = items.firstOrNull { it.id == id } ?: return
        val target = items.firstOrNull { it.desk == moving.desk && it.col == col && it.row == row && it.id != id }
        when {
            target == null -> saveItems(items.map { if (it.id == id) it.copy(col = col, row = row) else it })
            moving.isFolder -> saveItems(items.map {
                when (it.id) { id -> it.copy(col = col, row = row); target.id -> it.copy(col = moving.col, row = moving.row); else -> it }
            })
            target.isFolder -> saveItems(items.filter { it.id != id }
                .map { if (it.id == target.id) it.copy(apps = (it.apps + moving.appKey!!).distinct()) else it })
            else -> saveItems(items.filter { it.id != id && it.id != target.id } +
                DesktopItem(newId(), col, row, name = "Folder", apps = listOf(target.appKey!!, moving.appKey!!), desk = moving.desk))
        }
    }

    fun renameFolder(id: String, name: String) =
        saveItems(items.map { if (it.id == id) it.copy(name = name.take(30)) else it })

    fun removeFromFolder(folderId: String, key: String, cols: Int, rows: Int) {
        val f = items.firstOrNull { it.id == folderId } ?: return
        val rest = f.apps - key
        saveItems(items.mapNotNull { if (it.id == folderId) (if (rest.isEmpty()) null else it.copy(apps = rest)) else it })
        val (c, r) = freeCell(cols, rows)
        saveItems(items + DesktopItem(newId(), c, r, appKey = key, desk = f.desk))
    }

    private fun newId() = "i" + System.nanoTime().toString(36)

    private fun saveItems(list: List<DesktopItem>) {
        items = list.filter { !it.isFolder || it.apps.isNotEmpty() }
        val arr = JSONArray()
        items.forEach { i ->
            arr.put(JSONObject().put("id", i.id).put("c", i.col).put("r", i.row).put("app", i.appKey ?: "")
                .put("name", i.name).put("apps", JSONArray(i.apps)).put("d", i.desk))
        }
        sp.edit().putString("items", arr.toString()).apply()
    }

    private fun loadItems(): List<DesktopItem> = runCatching {
        val arr = JSONArray(sp.getString("items", "[]"))
        (0 until arr.length()).map { n ->
            val o = arr.getJSONObject(n)
            val apps = o.optJSONArray("apps") ?: JSONArray()
            DesktopItem(o.getString("id"), o.getInt("c"), o.getInt("r"), o.getString("app").ifBlank { null },
                o.optString("name"), (0 until apps.length()).map { apps.getString(it) }, o.optInt("d", 0))
        }
    }.getOrDefault(emptyList())

    // ---- clipboard history
    fun addClip(text: String) {
        val t = text.trim().take(2000)
        if (t.isEmpty() || clips.firstOrNull()?.text == t) return
        clips = (listOf(Clip(t, System.currentTimeMillis(), false)) + clips.filter { it.text != t })
            .let { l -> l.filter { it.pinned } + l.filter { !it.pinned }.take(25) }
        saveClips()
    }
    fun togglePin(text: String) { clips = clips.map { if (it.text == text) it.copy(pinned = !it.pinned) else it }; saveClips() }
    fun removeClip(text: String) { clips = clips.filter { it.text != text }; saveClips() }
    private fun saveClips() {
        val arr = JSONArray()
        clips.forEach { arr.put(JSONObject().put("t", it.text).put("w", it.time).put("p", it.pinned)) }
        sp.edit().putString("clips", arr.toString()).apply()
    }
    private fun loadClips(): List<Clip> = runCatching {
        val arr = JSONArray(sp.getString("clips", "[]"))
        (0 until arr.length()).map { arr.getJSONObject(it).let { o -> Clip(o.getString("t"), o.getLong("w"), o.getBoolean("p")) } }
    }.getOrDefault(emptyList())

    // ---- wallpaper
    fun setWallpaper(id: String, customUri: String? = customWallpaper) {
        customWallpaper = customUri
        if (perDeskWallpaper) {
            deskWallpapers = deskWallpapers + (currentDesk.toString() to id); putMap("desk_wallpapers", deskWallpapers)
        }
        wallpaperId = id
        sp.edit().putString("wallpaper", id).putString("custom_wallpaper", customUri).apply()
    }

    fun setWallpaperForDesk(desk: Int, id: String, customUri: String?) {
        if (customUri != null) { customWallpaper = customUri; sp.edit().putString("custom_wallpaper", customUri).apply() }
        if (perDeskWallpaper) {
            deskWallpapers = deskWallpapers + (desk.toString() to id); putMap("desk_wallpapers", deskWallpapers)
        } else { wallpaperId = id; sp.edit().putString("wallpaper", id).apply() }
    }

    /** The wallpaper id to show for a desk, honouring "per desk" and "change daily". */
    fun wallpaperFor(desk: Int): String {
        if (dailyWallpaper) {
            val ids = Wallpapers.presets.map { it.id }
            return ids[(Calendar.getInstance().get(Calendar.DAY_OF_YEAR) + desk) % ids.size]
        }
        return if (perDeskWallpaper) deskWallpapers[desk.toString()] ?: wallpaperId else wallpaperId
    }

    // ---- zones
    fun setZone(pkg: String, value: String?) {
        zoneOverrides = if (value == null) zoneOverrides - pkg else zoneOverrides + (pkg to value); putMap("zones", zoneOverrides)
    }

    fun zoneOf(app: AppEntry): Zone? = when (val o = zoneOverrides[app.packageName]) {
        null -> app.autoZone
        "NONE" -> null
        else -> runCatching { Zone.valueOf(o) }.getOrNull()
    }

    /** Apps of a zone in the user's order (new apps appended alphabetically). */
    fun zoneApps(zone: Zone, apps: List<AppEntry>): List<AppEntry> {
        val inZone = apps.filter { zoneOf(it) == zone }
        val order = zoneOrder[zone.name]?.split(',').orEmpty()
        val byKey = inZone.associateBy { it.key }
        return order.mapNotNull { byKey[it] } + inZone.filter { it.key !in order }
    }

    fun reorderZone(zone: Zone, current: List<AppEntry>, from: Int, to: Int) {
        if (from == to || from !in current.indices) return
        val keys = current.map { it.key }.toMutableList()
        val k = keys.removeAt(from)
        keys.add(to.coerceIn(0, keys.size), k)
        zoneOrder = zoneOrder + (zone.name to keys.joinToString(",")); putMap("zone_order", zoneOrder)
    }

    fun setLimit(pkg: String, minutes: Int) {
        limits = if (minutes <= 0) limits - pkg else limits + (pkg to minutes)
        putMap("limits", limits.mapValues { it.value.toString() })
    }

    /** First run: pin a few common apps so nothing starts empty. */
    fun seedDefaults(apps: List<AppEntry>) {
        if (initialized) return
        val wanted = listOf("files", "filemanager", "chrome", "browser", "settings", "camera", "gallery", "photos")
        val picks = wanted.mapNotNull { w -> apps.firstOrNull { w in it.packageName.lowercase() } }.distinct().take(6)
        taskbar = picks.map { it.key }; putList("taskbar", taskbar)
        startPins = (picks + apps).distinct().take(12).map { it.key }; putList("start_pins", startPins)
        if (items.isEmpty()) {
            val old = list("desktop")
            val keys = old.ifEmpty { picks.take(4).map { it.key } }
            saveItems(keys.mapIndexed { i, k -> DesktopItem("s$i", i % 2, i / 2, appKey = k) })
        }
        initialized = true
        sp.edit().putBoolean("initialized", true).apply()
    }

    /** v0.2 users already initialised: make sure the new start-menu pins are filled once. */
    fun ensureStartPins(apps: List<AppEntry>) {
        if (startPins.isEmpty() && apps.isNotEmpty()) {
            startPins = (taskbar + apps.map { it.key }).distinct().take(12); putList("start_pins", startPins)
        }
    }
}
