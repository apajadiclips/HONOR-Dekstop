package com.honorhybrid.desktop.ui

import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.honorhybrid.desktop.data.AppEntry
import com.honorhybrid.desktop.data.Prefs
import com.honorhybrid.desktop.data.Wallpapers
import com.honorhybrid.desktop.data.Zone
import com.honorhybrid.desktop.service.GlassNotificationService
import com.honorhybrid.desktop.system.Media
import com.honorhybrid.desktop.system.Sys

private fun zoneSubtitle(z: Zone) = when (z) {
    Zone.GAME -> "Semua game anda di satu tempat, dengan mod prestasi"
    Zone.SOSIAL -> "Semua sembang dan suapan dalam satu skrin"
    Zone.HIBURAN -> "Video, muzik dan podcast, sedia untuk ditonton"
    Zone.KERJA -> "Dokumen, e-mel dan kalendar"
    Zone.KREATIF -> "Kamera, galeri dan suntingan"
    Zone.ALAT -> "Fail, tetapan dan utiliti"
}

@Composable
fun ZonesScreen(apps: List<AppEntry>, prefs: Prefs, zone: Zone, onZone: (Zone) -> Unit, onClose: () -> Unit) {
    val inZone = remember(apps, prefs.zoneOverrides, prefs.zoneOrder, zone) { prefs.zoneApps(zone, apps) }
    val preset = Wallpapers.forZone(zone)
    Box(Modifier.fillMaxSize()) {
        PresetWallpaper(preset)
        BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
            val wide = maxWidth > 760.dp
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (wide) Column(Modifier.width(210.dp).fillMaxHeight().glass(24.dp).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Ruangan", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(8.dp))
                    Zone.entries.forEach { z -> ZoneNav(z, z == zone) { onZone(z) } }
                    Spacer(Modifier.weight(1f))
                    Column(Modifier.fillMaxWidth().glass(16.dp, strong = false).padding(12.dp)) {
                        Text("Wallpaper ruangan", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("${preset.name} · ditukar automatik bila masuk ruangan ini", color = Muted, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Pill("Kembali ke desktop", Glyph.home, Modifier.fillMaxWidth()) { onClose() }
                }
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Ruangan ${zone.label}", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            Text(zoneSubtitle(zone), color = Muted, fontSize = 13.sp)
                        }
                        when (zone) {
                            Zone.SOSIAL -> ScreenTimePill(inZone, prefs)
                            else -> FocusButton(zone)
                        }
                        if (!wide) { Spacer(Modifier.width(8.dp)); GlassIconButton(Glyph.home, "Kembali") { onClose() } }
                    }
                    if (!wide) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Zone.entries.forEach { z -> Box(Modifier.width(130.dp)) { ZoneNav(z, z == zone) { onZone(z) } } }
                    }
                    when (zone) {
                        Zone.GAME -> GameZone(inZone, prefs, onClose)
                        Zone.SOSIAL -> SocialZone(apps, inZone, prefs, onClose)
                        Zone.HIBURAN -> EntertainmentZone(inZone, prefs, onClose)
                        else -> Panel(Modifier.weight(1f)) { ZoneApps(zone, inZone, prefs, onClose) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    Column(modifier.fillMaxWidth().glass(24.dp).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)

@Composable
private fun ZoneNav(z: Zone, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(12.dp))
        .background(if (selected) Color(0x38FFFFFF) else Color(0x14FFFFFF)).clickable(onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(zoneIcon(z), null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(z.label, color = Color.White, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
    }
}

/** Re-orderable, renamable app grid used by every zone (long-press + drag to move). */
@Composable
private fun ZoneApps(zone: Zone, inZone: List<AppEntry>, prefs: Prefs, onClose: () -> Unit,
                     sub: (AppEntry) -> String? = { null }) {
    val context = LocalContext.current
    var menuFor by remember { mutableStateOf<String?>(null) }
    val icon = (prefs.iconSize.dp + 6).dp
    Text("${inZone.size} aplikasi · seret untuk susun · tekan lama untuk tukar nama atau ruangan", color = Muted, fontSize = 12.sp)
    if (inZone.isEmpty()) Text("Belum ada aplikasi di sini. Buka Menu Mula, tekan lama aplikasi dan pilih \"Pindah ke ruangan ${zone.label}\".",
        color = Color.White, fontSize = 14.sp)
    Column(Modifier.verticalScroll(rememberScrollState())) {
        ReorderGrid(inZone, { it.key }, icon + 46.dp, icon + 62.dp,
            onMove = { from, to -> prefs.reorderZone(zone, inZone, from, to) },
            onTap = { Actions.launch(context, it); onClose() },
            onLongPress = { menuFor = it.key }) { app, _ ->
            Box {
                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppGlyph(app, icon)
                    IconLabel(prefs.labelOf(app))
                    sub(app)?.let { Text(it, color = Muted, fontSize = 11.sp, maxLines = 1) }
                }
                AppMenu(app, prefs, menuFor == app.key, { menuFor = null })
            }
        }
    }
}

// ---------------------------------------------------------------- Game

@Composable
private fun ColumnScope.GameZone(games: List<AppEntry>, prefs: Prefs, onClose: () -> Unit) {
    val context = LocalContext.current
    val ram = rememberPolled(3000) { Sys.ram(it) }
    val temp = rememberPolled(5000) { Sys.batteryTempC(it) }
    val hz = rememberPolled(10_000) { Sys.refreshRate(it) }
    val battery = rememberBattery()
    val charging = rememberPolled(5000) { Sys.charging(it) }
    val usage = rememberPolled(10_000) { Sys.hasUsage(it) }
    val last = rememberPolled(15_000, usage) { if (usage) Sys.lastUsed(it) else emptyMap() }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Meter("Kadar segar skrin", "$hz Hz", "Paparan semasa", Modifier.weight(1f))
        Meter("Suhu bateri", temp?.let { "%.0f°C".format(it) } ?: "—",
            when { temp == null -> ""; temp < 40 -> "Normal"; temp < 45 -> "Hangat"; else -> "Panas - rehatkan tablet" }, Modifier.weight(1f))
        Meter("RAM tersedia", "%.1f GB".format(ram.availMb / 1024f), "daripada %.0f GB".format(ram.totalMb / 1024f), Modifier.weight(1f))
        Meter("Bateri", "$battery%", if (charging) "Sedang dicas" else "Tidak dicas", Modifier.weight(1f))
    }
    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Panel(Modifier.weight(1f).fillMaxHeight()) {
            Row { SectionTitle("Perpustakaan game", Modifier.weight(1f)); Text("Susunan anda", color = Muted, fontSize = 12.sp) }
            ZoneApps(Zone.GAME, games, prefs, onClose) { a ->
                val t = last[a.packageName] ?: prefs.lastLaunch(a.key)
                if (t > 0) ago(t) else null
            }
        }
        Panel(Modifier.width(300.dp).fillMaxHeight()) {
            SectionTitle("Tetapan mod game")
            val dnd = rememberPolled(1500) { Focus.isOn(it) }
            val manual = rememberPolled(2000) { !Sys.autoBrightness(it) }
            val dns = rememberPolled(5000) { Sys.privateDns(it) }
            SwitchRow("Senyapkan notifikasi", "Jangan Ganggu semasa main", dnd) { Focus.toggle(context) }
            SwitchRow("Kunci kecerahan", "Matikan kecerahan automatik", manual) {
                if (Sys.canWriteSettings(context)) Sys.setAutoBrightness(context, !it)
                else Actions.start(context, android.content.Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    android.net.Uri.parse("package:${context.packageName}")))
            }
            SwitchRow("Sekat iklan (DNS)", dns ?: "Tetapkan dalam Tetapan > Private DNS", dns != null) {
                Actions.settings(context, Settings.ACTION_WIRELESS_SETTINGS)
            }
            SwitchRow("\"Main terakhir\" tepat", "Akses penggunaan", usage) {
                Actions.settings(context, Settings.ACTION_USAGE_ACCESS_SETTINGS)
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFFF2E88)).clickable {
                val gamePkgs = games.map { it.packageName }.toSet()
                val others = context.packageManager.getInstalledApplications(0).map { it.packageName }
                    .filter { it !in gamePkgs && it != context.packageName }
                Sys.killBackground(context, others)
                Actions.toast(context, "Aplikasi latar ditutup. Pilih game untuk mula.")
            }, contentAlignment = Alignment.Center) {
                Text("Tutup aplikasi latar & main", color = Color(0xFF1A0614), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun Meter(label: String, value: String, sub: String, modifier: Modifier) {
    Column(modifier.glass(20.dp).padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(label, color = Muted, fontSize = 12.sp, maxLines = 1)
        Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(sub, color = Muted, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun SwitchRow(title: String, hint: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!on) }, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 13.sp)
            Text(hint, color = Muted, fontSize = 11.sp, maxLines = 2)
        }
        Switch(on, onChange)
    }
}

// ---------------------------------------------------------------- Media Sosial

@Composable
private fun ScreenTimePill(social: List<AppEntry>, prefs: Prefs) {
    val usage = rememberPolled(30_000) { if (Sys.hasUsage(it)) Sys.usageToday(it) else emptyMap() }
    val used = social.sumOf { usage[it.packageName] ?: 0L }
    val limit = social.sumOf { (prefs.limits[it.packageName] ?: 0).toLong() }
    Row(Modifier.height(44.dp).glass(22.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Masa skrin hari ini: ${minutes(used)}" + if (limit > 0) " / ${minutes(limit)}" else "", color = Color.White,
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        if (limit > 0) {
            Spacer(Modifier.width(10.dp))
            Box(Modifier.width(80.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0x40FFFFFF))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth((used.toFloat() / limit).coerceIn(0f, 1f)).background(Color.White))
            }
        }
    }
}

@Composable
private fun ColumnScope.SocialZone(apps: List<AppEntry>, social: List<AppEntry>, prefs: Prefs, onClose: () -> Unit) {
    val context = LocalContext.current
    val notifs by GlassNotificationService.items.collectAsState()
    val socialPkgs = social.map { it.packageName }.toSet()
    val inbox = notifs.filter { it.packageName in socialPkgs }
    val counts = inbox.groupingBy { it.packageName }.eachCount()
    val granted = rememberPolled(3000) { listenerEnabled(it) }
    val hasUsage = rememberPolled(5000) { Sys.hasUsage(it) }
    val usage = rememberPolled(30_000, hasUsage) { if (hasUsage) Sys.usageToday(it) else emptyMap() }
    var menuFor by remember { mutableStateOf<String?>(null) }

    Row(Modifier.fillMaxWidth().glass(22.dp).horizontalScroll(rememberScrollState()).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (social.isEmpty()) Text("Tekan lama aplikasi di Menu Mula > \"Pindah ke ruangan Media Sosial\".", color = Color.White, fontSize = 13.sp)
        social.forEach { a ->
            Box {
                Column(Modifier.width(84.dp).clip(RoundedCornerShape(12.dp)).clickable { Actions.launch(context, a); onClose() }
                    .padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    AppGlyph(a, 52.dp); Spacer(Modifier.height(6.dp)); IconLabel(prefs.labelOf(a))
                }
                counts[a.packageName]?.let { n ->
                    Box(Modifier.align(Alignment.TopEnd).padding(end = 10.dp).size(20.dp).clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFF4D5E)), contentAlignment = Alignment.Center) {
                        Text("$n", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Panel(Modifier.weight(1f).fillMaxHeight()) {
            Row { SectionTitle("Peti masuk bersatu", Modifier.weight(1f)); Text("Semua aplikasi sembang", color = Muted, fontSize = 12.sp) }
            if (!granted) Pill("Benarkan akses notifikasi", Glyph.bell) { Actions.settings(context, Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS) }
            else if (inbox.isEmpty()) Text("Tiada mesej baharu.", color = Muted, fontSize = 13.sp)
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val iconOf = social.associate { it.packageName to it.icon }
                inbox.forEach { NotificationCard(it, iconOf[it.packageName], apps, onClose) }
            }
        }
        Panel(Modifier.width(320.dp).fillMaxHeight()) {
            SectionTitle("Kesejahteraan digital")
            if (!hasUsage) {
                Text("Benarkan \"Akses penggunaan\" untuk lihat masa skrin sebenar.", color = Muted, fontSize = 12.sp)
                Pill("Benarkan", null) { Actions.settings(context, Settings.ACTION_USAGE_ACCESS_SETTINGS) }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                social.sortedByDescending { usage[it.packageName] ?: 0 }.forEach { a ->
                    val used = usage[a.packageName] ?: 0L
                    val limit = prefs.limits[a.packageName] ?: 0
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable {
                        val steps = listOf(0, 15, 30, 60, 90, 120)
                        prefs.setLimit(a.packageName, steps[(steps.indexOf(limit) + 1) % steps.size])
                    }.padding(4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row {
                            Text(prefs.labelOf(a), color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1)
                            Text(if (limit > 0) "$used / $limit min" else "$used min · tiada had", color = Muted, fontSize = 12.sp)
                        }
                        if (limit > 0) Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0x38FFFFFF))) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth((used.toFloat() / limit).coerceIn(0f, 1f))
                                .background(if (used >= limit) Color(0xFFFF6B6B) else if (used > limit * 0.75) Color(0xFFFFB347) else Color.White))
                        }
                    }
                }
            }
            Text("Ketik aplikasi untuk tukar had harian (15-120 min).", color = Muted, fontSize = 11.sp)
            Column(Modifier.fillMaxWidth().glass(16.dp, strong = false).clickable {
                Actions.settings(context, "android.settings.ZEN_MODE_AUTOMATION_SETTINGS", "android.settings.ZEN_MODE_SETTINGS")
            }.padding(12.dp)) {
                Text("Waktu rehat", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Jadualkan Jangan Ganggu, cth 10:00 PTM – 7:00 PG", color = Muted, fontSize = 12.sp)
            }
        }
    }
}

// ---------------------------------------------------------------- Hiburan

@Composable
private fun ColumnScope.EntertainmentZone(media: List<AppEntry>, prefs: Prefs, onClose: () -> Unit) {
    val context = LocalContext.current
    val hasUsage = rememberPolled(5000) { Sys.hasUsage(it) }
    val last = rememberPolled(15_000, hasUsage) { if (hasUsage) Sys.lastUsed(it) else emptyMap() }
    val recent = media.map { it to (last[it.packageName] ?: prefs.lastLaunch(it.key)) }.filter { it.second > 0 }
        .sortedByDescending { it.second }.take(3)
    val colors = listOf(Color(0xFF8E2F52), Color(0xFF3D2A7A), Color(0xFFB4502E))

    Panel {
        SectionTitle("Sambung tonton")
        if (recent.isEmpty()) Text("Aplikasi hiburan yang anda buka akan muncul di sini.", color = Muted, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            recent.forEachIndexed { i, (a, t) ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable { Actions.launch(context, a); onClose() },
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(16.dp)).background(colors[i % 3])
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        Image(a.icon, null, Modifier.size(56.dp))
                        Box(Modifier.align(Alignment.BottomEnd).padding(10.dp).size(40.dp).clip(RoundedCornerShape(20.dp))
                            .background(Color(0xE6FFFFFF)), contentAlignment = Alignment.Center) {
                            Icon(Glyph.playFill, null, tint = Color(0xFF2A1020), modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(prefs.labelOf(a), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    Text("Dibuka ${ago(t)}", color = Muted, fontSize = 12.sp)
                }
            }
            repeat(3 - recent.size) { Spacer(Modifier.weight(1f)) }
        }
    }
    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Panel(Modifier.weight(1f).fillMaxHeight()) {
            SectionTitle("Aplikasi hiburan")
            Box(Modifier.weight(1f)) { Column { ZoneApps(Zone.HIBURAN, media, prefs, onClose) } }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill("Hantar ke TV (cast)", Glyph.cast, Modifier.weight(1f)) {
                    Actions.settings(context, Settings.ACTION_CAST_SETTINGS, Settings.ACTION_WIRELESS_SETTINGS)
                }
                Pill("Gambar dalam gambar", Glyph.pip, Modifier.weight(1f)) {
                    Actions.toast(context, "Semasa video dimainkan, tekan Home. Aplikasi yang menyokong akan masuk tetingkap kecil.")
                }
                val end = prefs.sleepEnd
                Pill(if (end > System.currentTimeMillis()) "Tidur ${fmt("h:mm", end)}" else "Pemasa tidur", Glyph.timer, Modifier.weight(1f)) {
                    val steps = listOf(15, 30, 60, 0)
                    val currentLeft = if (end > System.currentTimeMillis()) ((end - System.currentTimeMillis()) / 60_000).toInt() else -1
                    val nextMin = steps.firstOrNull { it > currentLeft + 1 } ?: 0
                    prefs.setSleep(Media.sleepTimer(context, nextMin))
                    Actions.toast(context, if (nextMin == 0) "Pemasa tidur dimatikan" else "Media akan dijeda dalam $nextMin minit")
                }
            }
        }
        Column(Modifier.width(340.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Panel {
                Text("Sedang dimainkan", color = Muted, fontSize = 12.sp)
                MediaCard()
            }
            Panel(Modifier.weight(1f)) {
                SectionTitle("Mod Teater")
                Text("Senyapkan notifikasi supaya tontonan tidak diganggu. Sekatan iklan DNS kekal aktif jika sudah dihidupkan.",
                    color = Muted, fontSize = 13.sp, lineHeight = 18.sp)
                FocusButton(Zone.HIBURAN)
            }
        }
    }
}

/** Mod Game / Mod Teater / Mod Fokus: a real Do-Not-Disturb toggle. */
@Composable
fun FocusButton(zone: Zone) {
    val context = LocalContext.current
    val on = rememberPolled(1500) { Focus.isOn(it) }
    val name = when (zone) { Zone.GAME -> "Mod Game"; Zone.HIBURAN -> "Mod Teater"; else -> "Mod Fokus" }
    Row(Modifier.height(44.dp).glass(22.dp).clickable { Focus.toggle(context) }.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(if (on) Color(0xFF5CF29A) else Color(0xFF9A97B0)))
        Spacer(Modifier.width(8.dp))
        Text("$name: ${if (on) "Aktif" else "Mati"}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
