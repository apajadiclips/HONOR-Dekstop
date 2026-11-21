package com.honorhybrid.desktop.ui

import android.content.ClipData
import android.content.ClipboardManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.honorhybrid.desktop.data.AppEntry
import com.honorhybrid.desktop.data.ClockPos
import com.honorhybrid.desktop.data.DesktopItem
import com.honorhybrid.desktop.data.HomeMode
import com.honorhybrid.desktop.data.Prefs
import com.honorhybrid.desktop.data.Wallpapers
import com.honorhybrid.desktop.data.Zone
import com.honorhybrid.desktop.service.GlassNotification
import com.honorhybrid.desktop.service.GlassNotificationService
import com.honorhybrid.desktop.system.Sys
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

enum class Overlay { NONE, START, PANEL, ZONES, WALLPAPER, SETTINGS, FOLDER, MULTITASK }

@Composable
fun Launcher(apps: List<AppEntry>, prefs: Prefs, homeTick: Int) {
    var overlay by remember { mutableStateOf(Overlay.NONE) }
    var zone by remember { mutableStateOf(Zone.GAME) }
    var folder by remember { mutableStateOf<DesktopItem?>(null) }
    var grid by remember { mutableStateOf(GridSpec(6, 5)) }
    var toast by remember { mutableStateOf<GlassNotification?>(null) }
    LaunchedEffect(homeTick) { overlay = Overlay.NONE }
    LaunchedEffect(Unit) {
        GlassNotificationService.posted.collectLatest { toast = it; delay(4500); toast = null }
    }
    BackHandler(overlay != Overlay.NONE) { overlay = Overlay.NONE }
    SideEffect {
        GlassStyle.strength = prefs.glass / 100f
        Actions.onLaunch = { prefs.recordLaunch(it.key) }
    }
    val close = { overlay = Overlay.NONE }
    val toggle = { o: Overlay -> overlay = if (overlay == o) Overlay.NONE else o }
    val byKey = remember(apps) { apps.associateBy { it.key } }
    val now = rememberNow()
    val battery = rememberBattery()
    val wpId = prefs.wallpaperFor(prefs.currentDesk)
    val tablet = prefs.mode == HomeMode.TABLET
    val clockTop = tablet || prefs.clockPos == ClockPos.ATAS
    val swapClock = { prefs.setClock(if (prefs.clockPos == ClockPos.ATAS) ClockPos.BAWAH else ClockPos.ATAS) }
    val density = LocalDensity.current

    Box(Modifier.fillMaxSize()) {
        Wallpaper(Wallpapers.byId(wpId), if (wpId == "custom") prefs.customWallpaper else null)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = prefs.dim / 100f * 0.6f)))

        // Empty desktop: tap closes panels, long-press opens settings, swipe down opens the glass panel.
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val topZone = with(density) { maxHeight.toPx() } * 0.35f
            val minSwipe = with(density) { 90.dp.toPx() }
            var startY by remember { mutableStateOf(0f) }
            var dy by remember { mutableStateOf(0f) }
            Box(Modifier.fillMaxSize()
                .pointerInput(Unit) { detectTapGestures(onTap = { close() }, onLongPress = { overlay = Overlay.SETTINGS }) }
                .pointerInput(prefs.swipePanel) {
                    if (prefs.swipePanel) detectVerticalDragGestures(
                        onDragStart = { startY = it.y; dy = 0f },
                        onVerticalDrag = { _, d -> dy += d },
                        onDragEnd = { if (startY < topZone && dy > minSwipe) overlay = Overlay.PANEL },
                    )
                })
        }

        BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            val wide = maxWidth > 700.dp
            val sideW = if (wide) 330.dp else 230.dp
            val sideVisible = clockTop || prefs.showDebloatWidget
            DesktopGrid(byKey, prefs,
                Modifier.fillMaxSize().padding(start = 12.dp, top = if (prefs.showZoneBar) 64.dp else 12.dp,
                    end = if (sideVisible) sideW + 24.dp else 12.dp, bottom = if (tablet) 100.dp else 84.dp),
                onGrid = { grid = it }, onOpenFolder = { folder = it; overlay = Overlay.FOLDER })
            if (prefs.showZoneBar) ZoneBar(Modifier.align(Alignment.TopCenter).padding(top = 10.dp)) {
                zone = it; overlay = Overlay.ZONES
            }
            if (sideVisible) Column(Modifier.align(Alignment.TopEnd).padding(16.dp).width(sideW),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (clockTop) ClockWidget(now, canSwap = !tablet, onSwap = swapClock)
                if (prefs.showDebloatWidget) DebloatWidget()
            }

            AnimatedVisibility(overlay == Overlay.START, Modifier.align(Alignment.BottomCenter),
                enter = fadeIn() + slideInVertically { it / 6 }, exit = fadeOut() + slideOutVertically { it / 6 }) {
                StartMenu(apps, prefs, onClose = close, onSettings = { overlay = Overlay.SETTINGS },
                    modifier = Modifier.padding(bottom = 84.dp, start = 16.dp, end = 16.dp)
                        .widthIn(max = 640.dp).heightIn(max = (maxHeight - 100.dp).coerceAtLeast(320.dp)))
            }
            AnimatedVisibility(overlay == Overlay.PANEL, Modifier.fillMaxSize(),
                enter = fadeIn() + slideInVertically { -it / 5 }, exit = fadeOut() + slideOutVertically { -it / 5 }) {
                NotificationCenter(apps, prefs, now, battery, wide, onClose = close)
            }
            AnimatedVisibility(overlay == Overlay.MULTITASK, Modifier.fillMaxSize(), enter = fadeIn(), exit = fadeOut()) {
                Multitask(apps, prefs, onClose = close)
            }
            if (tablet) Dock(prefs.taskbar.mapNotNull { byKey[it] }, prefs, overlay, toggle, Modifier.align(Alignment.BottomCenter))
            else Taskbar(prefs.taskbar.mapNotNull { byKey[it] }, prefs, now, battery, overlay, showClock = !clockTop,
                onSwapClock = swapClock, onToggle = toggle, modifier = Modifier.align(Alignment.BottomCenter))

            AnimatedVisibility(toast != null && overlay != Overlay.PANEL, Modifier.align(Alignment.TopCenter),
                enter = fadeIn() + slideInVertically { -it }, exit = fadeOut() + slideOutVertically { -it }) {
                toast?.let { NotificationToast(it, byKey.values.firstOrNull { a -> a.packageName == it.packageName }) {
                    toast = null; overlay = Overlay.PANEL
                } }
            }
        }

        AnimatedVisibility(overlay == Overlay.ZONES, enter = fadeIn(), exit = fadeOut()) {
            ZonesScreen(apps, prefs, zone, onZone = { zone = it }, onClose = close)
        }
        AnimatedVisibility(overlay == Overlay.WALLPAPER, enter = fadeIn(), exit = fadeOut()) {
            WallpaperScreen(prefs, byKey, onClose = close)
        }
        AnimatedVisibility(overlay == Overlay.SETTINGS, enter = fadeIn(), exit = fadeOut()) {
            SettingsPanel(prefs, onWallpaper = { overlay = Overlay.WALLPAPER }, onClose = close)
        }
        val f = folder
        if (overlay == Overlay.FOLDER && f != null) FolderPopup(f, byKey, prefs, grid, onClose = close)
    }
}

// ---------------------------------------------------------------- top & side

@Composable
private fun ZoneBar(modifier: Modifier, onPick: (Zone) -> Unit) {
    Row(modifier.glass(22.dp).padding(5.dp).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Zone.entries.forEach { z ->
            Row(Modifier.height(38.dp).clip(RoundedCornerShape(19.dp)).clickable { onPick(z) }.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(zoneIcon(z), null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(z.short, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

fun zoneIcon(z: Zone) = when (z) {
    Zone.GAME -> Glyph.pad; Zone.SOSIAL -> Glyph.chat; Zone.HIBURAN -> Glyph.play
    Zone.KERJA -> Glyph.work; Zone.KREATIF -> Glyph.brush; Zone.ALAT -> Glyph.tool
}

@Composable
private fun ClockWidget(now: Long, canSwap: Boolean, onSwap: () -> Unit) {
    Column(Modifier.fillMaxWidth().glass(18.dp).padding(horizontal = 20.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Text(fmt("h:mm", now), color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f))
            if (canSwap) GlassIconButton(Glyph.swap, "Pindah jam ke bawah", size = 40.dp) { onSwap() }
        }
        Text(fmt("EEEE, d MMMM", now), color = Muted, fontSize = 14.sp)
    }
}

/** The "Debloat Manager" card from the design, with live values. */
@Composable
private fun DebloatWidget() {
    val context = LocalContext.current
    val stats = rememberPolled(60_000) { Sys.packageStats(it) }
    val dns = rememberPolled(10_000) { Sys.privateDns(it) }
    val termux = rememberPolled(30_000) { c -> runCatching { c.packageManager.getPackageInfo("com.termux", 0); true }.getOrDefault(false) }
    fun runInTermux(cmd: String) {
        context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("hdm", cmd))
        if (Actions.launchPackage(context, "com.termux")) Actions.toast(context, "Arahan disalin. Tampal dalam Termux:\n$cmd")
        else Actions.toast(context, "Pasang Termux dahulu untuk guna hdm")
    }
    Column(Modifier.fillMaxWidth().glass(18.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Glyph.shield, null, tint = Color(0xFF7FD4A6), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Debloat Manager", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                modifier = Modifier.weight(1f))
            Text(android.os.Build.MODEL, color = Muted, fontSize = 12.sp)
        }
        val tiles = listOf(
            Triple("Sekatan iklan", if (dns != null) "Aktif (DNS)" else "Mati", dns != null),
            Triple("Pakej disabled", "${stats.disabled}", true),
            Triple("Jumlah pakej", "${stats.total}", true),
            Triple("Pemulihan", if (termux) "Tersedia" else "Perlu Termux", termux),
        )
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, value, good) ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0x33000000)).padding(10.dp)) {
                        Text(label, color = Muted, fontSize = 11.sp, maxLines = 1)
                        Text(value, color = if (good) Color(0xFF7FD4A6) else Color(0xFFFFC07A), fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF5B9BFF))
                .clickable { runInTermux("cd ~/HONOR-Dekstop && ./hdm --adb scan") }, contentAlignment = Alignment.Center) {
                Text("Imbas pakej", color = Color(0xFF0B1424), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(10.dp))
                .clickable { runInTermux("cd ~/HONOR-Dekstop && ./hdm --adb backup --label asal") }, contentAlignment = Alignment.Center) {
                Text("Backup", color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun NotificationToast(n: GlassNotification, app: AppEntry?, onClick: () -> Unit) {
    Row(Modifier.padding(top = 12.dp).widthIn(max = 460.dp).glass(22.dp).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (app != null) Image(app.icon, null, Modifier.size(36.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(n.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text("${app?.label ?: n.packageName} · ${n.text}", color = Muted, fontSize = 12.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Text("kini", color = Muted, fontSize = 12.sp)
    }
}

// ---------------------------------------------------------------- taskbar & dock

@Composable
private fun Taskbar(pinned: List<AppEntry>, prefs: Prefs, now: Long, battery: Int, overlay: Overlay,
                    showClock: Boolean, onSwapClock: () -> Unit, onToggle: (Overlay) -> Unit, modifier: Modifier) {
    val ram = rememberPolled(5_000) { Sys.ram(it) }
    val dnd = rememberPolled(3_000) { Focus.isOn(it) }
    val wifi = rememberPolled(5_000) { Sys.wifiOn(it) }
    Row(
        modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp).glass(18.dp).height(60.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            prefs.desks.forEachIndexed { i, _ ->
                DeskPill("${i + 1}", i == prefs.currentDesk) { prefs.switchDesk(i) }
            }
            DeskPill("+", false) { prefs.addDesk() }
            Text("Desktop ${prefs.currentDesk + 1} · ${prefs.desks.getOrElse(prefs.currentDesk) { "" }}", color = Muted,
                fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp).clip(RoundedCornerShape(8.dp)).clickable { onToggle(Overlay.MULTITASK) })
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            GlassIconButton(Glyph.grid, "Menu Mula", overlay == Overlay.START) { onToggle(Overlay.START) }
            pinned.forEach { AppIcon(it, prefs, iconSize = 34.dp, showLabel = false) }
            GlassIconButton(Glyph.windows, "Multitasking", overlay == Overlay.MULTITASK) { onToggle(Overlay.MULTITASK) }
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.clip(RoundedCornerShape(14.dp)).background(Color(0x22FFFFFF)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                Text("RAM ${"%.1f".format((ram.totalMb - ram.availMb) / 1024f)}/${"%.1f".format(ram.totalMb / 1024f)} GB",
                    color = Color.White, fontSize = 11.sp, maxLines = 1)
            }
            Spacer(Modifier.width(10.dp))
            if (dnd) { Icon(Glyph.moon, "Jangan Ganggu aktif", tint = Color.White, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(8.dp)) }
            if (wifi) { Icon(Glyph.wifi, "Wi-Fi", tint = Color.White, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(8.dp)) }
            Text("$battery%", color = Color.White, fontSize = 12.sp)
            Spacer(Modifier.width(8.dp))
            if (showClock) {
                Column(horizontalAlignment = Alignment.End,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onSwapClock() }.padding(4.dp)) {
                    Text(fmt("h:mm a", now), color = Color.White, fontSize = 12.sp)
                    Text(fmt("d/M/yyyy", now), color = Muted, fontSize = 11.sp)
                }
                GlassIconButton(Glyph.swap, "Pindah jam ke atas", size = 36.dp) { onSwapClock() }
            }
            GlassIconButton(Glyph.bell, "Notifikasi & tetapan pantas", overlay == Overlay.PANEL) { onToggle(Overlay.PANEL) }
        }
    }
}

@Composable
private fun DeskPill(label: String, active: Boolean, onClick: () -> Unit) {
    Box(Modifier.height(32.dp).widthIn(min = 40.dp).clip(RoundedCornerShape(16.dp))
        .background(if (active) Color(0x4DFFFFFF) else Color.Transparent)
        .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(16.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center) {
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Tablet (MagicOS-style) mode: floating dock instead of the full-width taskbar. */
@Composable
private fun Dock(pinned: List<AppEntry>, prefs: Prefs, overlay: Overlay, onToggle: (Overlay) -> Unit, modifier: Modifier) {
    Row(modifier.padding(bottom = 12.dp).glass(26.dp).height(76.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        pinned.forEach { AppIcon(it, prefs, iconSize = 48.dp, showLabel = false) }
        Box(Modifier.width(1.dp).height(40.dp).background(Color(0x40FFFFFF)))
        GlassIconButton(Glyph.grid, "Semua aplikasi", overlay == Overlay.START) { onToggle(Overlay.START) }
        GlassIconButton(Glyph.windows, "Multitasking", overlay == Overlay.MULTITASK) { onToggle(Overlay.MULTITASK) }
        GlassIconButton(Glyph.bell, "Notifikasi", overlay == Overlay.PANEL) { onToggle(Overlay.PANEL) }
    }
}
