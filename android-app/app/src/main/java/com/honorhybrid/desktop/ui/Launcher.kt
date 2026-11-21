package com.honorhybrid.desktop.ui

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.honorhybrid.desktop.data.AppEntry
import com.honorhybrid.desktop.data.Prefs
import com.honorhybrid.desktop.data.Wallpapers
import com.honorhybrid.desktop.data.Zone
import com.honorhybrid.desktop.service.GlassNotification
import com.honorhybrid.desktop.service.GlassNotificationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Overlay { NONE, START, PANEL, ZONES, WALLPAPER }

private val MS = Locale("ms", "MY")

@Composable
fun Launcher(apps: List<AppEntry>, prefs: Prefs, homeTick: Int) {
    var overlay by remember { mutableStateOf(Overlay.NONE) }
    var zone by remember { mutableStateOf(Zone.GAME) }
    LaunchedEffect(homeTick) { overlay = Overlay.NONE }
    BackHandler(overlay != Overlay.NONE) { overlay = Overlay.NONE }
    val close = { overlay = Overlay.NONE }
    val byKey = remember(apps) { apps.associateBy { it.key } }
    val now = rememberNow()
    val battery = rememberBattery()
    val preset = Wallpapers.byId(prefs.wallpaperId)

    Box(Modifier.fillMaxSize()) {
        Wallpaper(preset, if (prefs.wallpaperId == "custom") prefs.customWallpaper else null)

        // Tap on empty desktop closes any open panel.
        Box(Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null) { close() })

        BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            val wide = maxWidth > 700.dp
            DesktopIcons(prefs.desktop.mapNotNull { byKey[it] }, prefs,
                Modifier.align(Alignment.TopStart).padding(16.dp))
            ClockWidget(now, battery, Modifier.align(Alignment.TopEnd).padding(16.dp).width(if (wide) 320.dp else 220.dp))

            AnimatedVisibility(overlay == Overlay.START, Modifier.align(Alignment.BottomCenter),
                enter = fadeIn() + slideInVertically { it / 6 }, exit = fadeOut() + slideOutVertically { it / 6 }) {
                StartMenu(apps, prefs, onClose = close, onWallpaper = { overlay = Overlay.WALLPAPER },
                    modifier = Modifier.padding(bottom = 76.dp, start = 16.dp, end = 16.dp)
                        .widthIn(max = 680.dp).heightIn(max = (maxHeight - 110.dp).coerceAtLeast(300.dp)))
            }
            AnimatedVisibility(overlay == Overlay.PANEL, Modifier.align(Alignment.TopEnd),
                enter = fadeIn() + slideInHorizontally { it / 3 }, exit = fadeOut() + slideOutHorizontally { it / 3 }) {
                QuickPanel(apps, now, battery, onWallpaper = { overlay = Overlay.WALLPAPER }, onClose = close,
                    modifier = Modifier.padding(top = 12.dp, end = 12.dp, bottom = 76.dp)
                        .width(if (wide) 400.dp else maxWidth - 24.dp).fillMaxHeight())
            }
            Taskbar(prefs.taskbar.mapNotNull { byKey[it] }, prefs, now, battery, overlay,
                onToggle = { o -> overlay = if (overlay == o) Overlay.NONE else o },
                modifier = Modifier.align(Alignment.BottomCenter))
        }

        AnimatedVisibility(overlay == Overlay.ZONES, enter = fadeIn(), exit = fadeOut()) {
            ZonesScreen(apps, prefs, zone, onZone = { zone = it }, onClose = close)
        }
        AnimatedVisibility(overlay == Overlay.WALLPAPER, enter = fadeIn(), exit = fadeOut()) {
            WallpaperPicker(prefs, onClose = close)
        }
    }
}

// ---------------------------------------------------------------- desktop

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DesktopIcons(apps: List<AppEntry>, prefs: Prefs, modifier: Modifier) {
    FlowRow(modifier.width(196.dp), maxItemsInEachRow = 2, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        apps.forEach { AppIcon(it, prefs, iconSize = 52.dp, labelColor = Color.White) }
    }
}

@Composable
private fun ClockWidget(now: Long, battery: Int, modifier: Modifier) {
    Column(modifier.glass(20.dp).padding(18.dp)) {
        Text(SimpleDateFormat("h:mm", MS).format(Date(now)), color = Color.White, fontSize = 44.sp,
            fontWeight = FontWeight.SemiBold)
        Text(SimpleDateFormat("EEEE, d MMMM", MS).format(Date(now)), color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Text("Bateri $battery%", color = Muted, fontSize = 13.sp)
    }
}

// ---------------------------------------------------------------- taskbar

@Composable
private fun Taskbar(pinned: List<AppEntry>, prefs: Prefs, now: Long, battery: Int, overlay: Overlay,
                    onToggle: (Overlay) -> Unit, modifier: Modifier) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp).glass(18.dp).height(60.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Glyph.pad, "Ruangan", overlay == Overlay.ZONES) { onToggle(Overlay.ZONES) }
            Text("Ruangan", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(start = 2.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            GlassIconButton(Glyph.grid, "Menu Mula", overlay == Overlay.START) { onToggle(Overlay.START) }
            pinned.forEach { AppIcon(it, prefs, iconSize = 34.dp, showLabel = false) }
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Text("$battery%", color = Color.White, fontSize = 12.sp)
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(SimpleDateFormat("h:mm a", MS).format(Date(now)), color = Color.White, fontSize = 12.sp)
                Text(SimpleDateFormat("d/M/yyyy", MS).format(Date(now)), color = Muted, fontSize = 11.sp)
            }
            Spacer(Modifier.width(4.dp))
            GlassIconButton(Glyph.bell, "Notifikasi & tetapan pantas", overlay == Overlay.PANEL) { onToggle(Overlay.PANEL) }
        }
    }
}

// ---------------------------------------------------------------- start menu

@Composable
private fun StartMenu(apps: List<AppEntry>, prefs: Prefs, onClose: () -> Unit, onWallpaper: () -> Unit,
                      modifier: Modifier) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val shown = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter { it.label.contains(query, true) || it.packageName.contains(query, true) }
    }
    Column(modifier.glass(24.dp).clickable(remember { MutableInteractionSource() }, null) {}.padding(18.dp)) {
        Row(
            Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(23.dp)).background(Color(0x33000000))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(23.dp)).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Glyph.search, null, tint = Muted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) Text("Cari aplikasi", color = Muted, fontSize = 15.sp)
                BasicTextField(query, { query = it }, singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 15.sp), cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.fillMaxWidth())
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (query.isBlank()) "Semua aplikasi (${apps.size})" else "${shown.size} hasil", color = Color.White,
                fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text("Tekan lama untuk sematkan", color = Muted, fontSize = 12.sp)
        }
        LazyVerticalGrid(GridCells.Adaptive(92.dp), Modifier.weight(1f, fill = false).heightIn(min = 120.dp)) {
            items(shown, key = { it.key }) { AppIcon(it, prefs, iconSize = 48.dp, onLaunched = onClose) }
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("Wallpaper", Glyph.image, Modifier.weight(1f)) { onWallpaper() }
            Pill("Tetapan", Glyph.gear, Modifier.weight(1f)) { Actions.settings(context, Settings.ACTION_SETTINGS); onClose() }
            Pill("Tukar launcher", Glyph.home, Modifier.weight(1f)) {
                Actions.settings(context, Settings.ACTION_HOME_SETTINGS); onClose()
            }
        }
    }
}

@Composable
private fun Pill(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier,
                 onClick: () -> Unit) {
    Row(
        modifier.height(44.dp).glass(14.dp, strong = false).clickable(onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ---------------------------------------------------------------- quick settings + notifications

private data class Tile(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val action: String)

@Composable
private fun QuickPanel(apps: List<AppEntry>, now: Long, battery: Int, onWallpaper: () -> Unit, onClose: () -> Unit,
                       modifier: Modifier) {
    val context = LocalContext.current
    val notifs by GlassNotificationService.items.collectAsState()
    val iconOf = remember(apps) { apps.associate { it.packageName to it.icon } }
    var granted by remember { mutableStateOf(listenerEnabled(context)) }
    LaunchedEffect(Unit) { while (true) { granted = listenerEnabled(context); delay(2000) } }
    val tiles = listOf(
        Tile("Wi-Fi", Glyph.wifi, Settings.Panel.ACTION_WIFI),
        Tile("Bluetooth", Glyph.bluetooth, Settings.ACTION_BLUETOOTH_SETTINGS),
        Tile("Bunyi", Glyph.volume, Settings.Panel.ACTION_VOLUME),
        Tile("Kecerahan", Glyph.sun, Settings.ACTION_DISPLAY_SETTINGS),
        Tile("Jangan Ganggu", Glyph.moon, "android.settings.ZEN_MODE_SETTINGS"),
        Tile("Internet", Glyph.globe, Settings.Panel.ACTION_INTERNET_CONNECTIVITY),
        Tile("Sekat Iklan", Glyph.shield, Settings.ACTION_WIRELESS_SETTINGS),
        Tile("Tetapan", Glyph.gear, Settings.ACTION_SETTINGS),
    )
    LazyColumn(
        modifier.glass(26.dp).clickable(remember { MutableInteractionSource() }, null) {},
        contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(SimpleDateFormat("h:mm", MS).format(Date(now)), color = Color.White, fontSize = 30.sp,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${SimpleDateFormat("EEE, d MMM", MS).format(Date(now))} · $battery%", color = Muted, fontSize = 13.sp)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                tiles.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { t ->
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                                .clickable { Actions.settings(context, t.action); onClose() }.padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.size(52.dp).glass(17.dp, strong = false), contentAlignment = Alignment.Center) {
                                    Icon(t.icon, t.label, tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                                Spacer(Modifier.height(5.dp))
                                Text(t.label, color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
        item { Pill("Tukar wallpaper", Glyph.image, Modifier.fillMaxWidth()) { onWallpaper() } }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Notifikasi", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    modifier = Modifier.weight(1f))
                if (granted && notifs.any { it.clearable }) Text("Kosongkan semua", color = Color(0xFFCFE0FF),
                    fontSize = 13.sp, modifier = Modifier.clickable { GlassNotificationService.dismissAll() })
            }
        }
        if (!granted) item {
            Column(Modifier.fillMaxWidth().glass(18.dp, strong = false).padding(14.dp)) {
                Text("Benarkan akses notifikasi", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Supaya notifikasi dipaparkan di sini dalam gaya Liquid Glass. Kandungan kekal pada tablet anda.",
                    color = Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp))
                Pill("Buka tetapan", Glyph.bell) {
                    Actions.settings(context, Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                }
            }
        } else if (notifs.isEmpty()) item {
            Text("Tiada notifikasi", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
        }
        if (granted) items(notifs, key = { it.key }) { n -> NotificationCard(n, iconOf[n.packageName], onClose) }
    }
}

@Composable
private fun NotificationCard(n: GlassNotification, icon: androidx.compose.ui.graphics.ImageBitmap?, onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().glass(20.dp, strong = false).clickable {
            runCatching { n.contentIntent?.send() }; onClose()
        }.padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (icon != null) Image(icon, null, Modifier.size(32.dp))
        else Box(Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(Color(0x33FFFFFF)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(n.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            if (n.text.isNotBlank()) Text(n.text, color = Muted, fontSize = 13.sp, maxLines = 3,
                overflow = TextOverflow.Ellipsis, lineHeight = 17.sp)
            Text(SimpleDateFormat("h:mm a", MS).format(Date(n.postTime)), color = Muted, fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp))
        }
        if (n.clearable) Box(Modifier.size(32.dp).clip(RoundedCornerShape(16.dp))
            .clickable { GlassNotificationService.dismiss(n.key) }, contentAlignment = Alignment.Center) {
            Icon(Glyph.close, "Buang", tint = Muted, modifier = Modifier.size(16.dp))
        }
    }
}

private fun listenerEnabled(context: Context) =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

// ---------------------------------------------------------------- zones

@Composable
private fun ZonesScreen(apps: List<AppEntry>, prefs: Prefs, zone: Zone, onZone: (Zone) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val inZone = remember(apps, prefs.zoneOverrides, zone) { apps.filter { prefs.zoneOf(it) == zone } }
    Box(Modifier.fillMaxSize()) {
        PresetWallpaper(Wallpapers.forZone(zone))
        BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
            val wide = maxWidth > 700.dp
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (wide) Column(Modifier.width(220.dp).fillMaxHeight().glass(24.dp).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Ruangan", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(8.dp))
                    Zone.entries.forEach { z -> ZoneNav(z, z == zone) { onZone(z) } }
                    Spacer(Modifier.weight(1f))
                    Pill("Kembali ke desktop", Glyph.home, Modifier.fillMaxWidth()) { onClose() }
                }
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Ruangan ${zone.label}", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            Text(zoneSubtitle(zone), color = Muted, fontSize = 13.sp)
                        }
                        Pill(if (zone == Zone.HIBURAN) "Mod Teater" else "Mod Fokus", Glyph.moon) {
                            Actions.settings(context, "android.settings.ZEN_MODE_SETTINGS")
                        }
                    }
                    if (!wide) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Zone.entries.forEach { z -> Box(Modifier.weight(1f)) { ZoneNav(z, z == zone) { onZone(z) } } }
                    }
                    Column(Modifier.weight(1f).fillMaxWidth().glass(24.dp).padding(18.dp)) {
                        Text("${inZone.size} aplikasi · tekan lama untuk pindah ruangan", color = Muted, fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 10.dp))
                        if (inZone.isEmpty()) Text(
                            "Belum ada aplikasi di sini. Buka Menu Mula, tekan lama sesuatu aplikasi dan pilih " +
                                "\"Pindah ke ruangan ${zone.label}\".", color = Color.White, fontSize = 14.sp)
                        LazyVerticalGrid(GridCells.Adaptive(104.dp)) {
                            items(inZone, key = { it.key }) { AppIcon(it, prefs, iconSize = 60.dp, onLaunched = onClose) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoneNav(z: Zone, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0x38FFFFFF) else Color(0x14FFFFFF)).clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(when (z) { Zone.GAME -> Glyph.pad; Zone.SOSIAL -> Glyph.chat; Zone.HIBURAN -> Glyph.play }, null,
            tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(z.label, color = Color.White, fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
    }
}

private fun zoneSubtitle(z: Zone) = when (z) {
    Zone.GAME -> "Semua game anda di satu tempat"
    Zone.SOSIAL -> "Sembang dan media sosial"
    Zone.HIBURAN -> "Video, muzik dan podcast"
}

// ---------------------------------------------------------------- wallpaper picker

@Composable
private fun WallpaperPicker(prefs: Prefs, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) scope.launch {
            val path = withContext(Dispatchers.IO) { copyWallpaper(context, uri) }
            if (path != null) prefs.setWallpaper("custom", path)
        }
    }
    Box(Modifier.fillMaxSize().background(Color(0x99000000)).clickable(remember { MutableInteractionSource() }, null) { onClose() },
        contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(24.dp).widthIn(max = 640.dp).glass(26.dp)
                .clickable(remember { MutableInteractionSource() }, null) {}.verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Wallpaper", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Wallpapers.presets.forEach { p ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.fillMaxWidth().aspectRatio(1.4f).clip(RoundedCornerShape(12.dp))
                            .border(if (prefs.wallpaperId == p.id) 2.dp else 1.dp,
                                if (prefs.wallpaperId == p.id) Color.White else Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                            .clickable { prefs.setWallpaper(p.id) }) { PresetWallpaper(p) }
                        Text(p.name, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill("Pilih dari Galeri", Glyph.image, Modifier.weight(1f)) {
                    pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                Pill("Selesai", Glyph.home, Modifier.weight(1f)) { onClose() }
            }
            if (prefs.wallpaperId == "custom") Text("Gambar kustom sedang digunakan.", color = Muted, fontSize = 12.sp)
            Text("Setiap ruangan (Game, Sosial, Hiburan) menggunakan wallpaper tersendiri secara automatik.",
                color = Muted, fontSize = 12.sp)
        }
    }
}

/** Copy the picked image into app storage so it survives without long-lived URI permissions. */
private fun copyWallpaper(context: Context, uri: Uri): String? = runCatching {
    val out = File(context.filesDir, "wallpaper_${System.currentTimeMillis()}.img")
    context.contentResolver.openInputStream(uri)!!.use { input -> out.outputStream().use { input.copyTo(it) } }
    context.filesDir.listFiles { f -> f.name.startsWith("wallpaper_") && f != out }?.forEach { it.delete() }
    out.absolutePath
}.getOrNull()

// ---------------------------------------------------------------- live values

@Composable
private fun rememberNow(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(15_000) } }
    return now
}

@Composable
private fun rememberBattery(): Int {
    val context = LocalContext.current
    var level by remember { mutableIntStateOf(-1) }
    DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                val l = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val s = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                if (l >= 0) level = l * 100 / s
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.let { receiver.onReceive(context, it) }
        onDispose { context.unregisterReceiver(receiver) }
    }
    return level
}
