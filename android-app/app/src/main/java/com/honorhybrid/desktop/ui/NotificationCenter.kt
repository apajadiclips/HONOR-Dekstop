package com.honorhybrid.desktop.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.honorhybrid.desktop.data.AppEntry
import com.honorhybrid.desktop.data.Prefs
import com.honorhybrid.desktop.service.GlassNotification
import com.honorhybrid.desktop.service.GlassNotificationService
import com.honorhybrid.desktop.system.Media
import com.honorhybrid.desktop.system.Sys

fun listenerEnabled(context: Context) =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

private val ON = Color(0xFF2F6FE0)

/** Liquid Glass notification centre: notifications column + quick settings, as in the design. */
@Composable
fun NotificationCenter(apps: List<AppEntry>, prefs: Prefs, now: Long, battery: Int, wide: Boolean, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null) { onClose() }) {
        Row(Modifier.align(Alignment.TopEnd).fillMaxHeight().padding(top = 12.dp, end = 12.dp, bottom = 84.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (wide) NotificationList(apps, Modifier.width(360.dp).fillMaxHeight(), onClose)
            QuickSettings(now, battery, Modifier.width(if (wide) 370.dp else 340.dp).fillMaxHeight(),
                showNotifications = !wide, apps = apps, onClose = onClose)
        }
    }
}

@Composable
private fun NotificationList(apps: List<AppEntry>, modifier: Modifier, onClose: () -> Unit) {
    val context = LocalContext.current
    val notifs by GlassNotificationService.items.collectAsState()
    val granted = rememberPolled(2000) { listenerEnabled(it) }
    val iconOf = remember(apps) { apps.associate { it.packageName to it.icon } }
    LazyColumn(modifier.clickable(remember { MutableInteractionSource() }, null) {},
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth().glass(18.dp).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Notifikasi", Modifier.weight(1f))
                if (granted && notifs.any { it.clearable }) Text("Kosongkan semua", color = Color(0xFFCFE0FF), fontSize = 13.sp,
                    modifier = Modifier.clickable { GlassNotificationService.dismissAll() })
            }
        }
        if (!granted) item {
            Column(Modifier.fillMaxWidth().glass(20.dp).padding(14.dp)) {
                Text("Benarkan akses notifikasi", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Supaya notifikasi, butang balas dan media dipaparkan di sini. Kandungan kekal pada tablet anda.",
                    color = Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp))
                Pill("Buka tetapan", Glyph.bell) { Actions.settings(context, Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS) }
            }
        } else if (notifs.isEmpty()) item {
            Text("Tiada notifikasi", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(8.dp))
        }
        if (granted) items(notifs, key = { it.key }) { NotificationCard(it, iconOf[it.packageName], apps, onClose) }
    }
}

@Composable
fun NotificationCard(n: GlassNotification, icon: ImageBitmap?, apps: List<AppEntry>, onClose: () -> Unit) {
    val context = LocalContext.current
    var replying by remember(n.key) { mutableStateOf(false) }
    var reply by remember(n.key) { mutableStateOf("") }
    val appName = remember(apps, n.packageName) { apps.firstOrNull { it.packageName == n.packageName }?.label ?: "" }
    Column(Modifier.fillMaxWidth().glass(20.dp).padding(14.dp)) {
        Row(Modifier.fillMaxWidth().clickable { runCatching { n.contentIntent?.send() }; onClose() },
            verticalAlignment = Alignment.Top) {
            if (icon != null) Image(icon, null, Modifier.size(34.dp))
            else Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Color(0x33FFFFFF)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row {
                    Text(appName, color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1)
                    Text(ago(n.postTime), color = Muted, fontSize = 12.sp)
                }
                Text(n.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                if (n.text.isNotBlank()) Text(n.text, color = Color(0xFFEEF2F9), fontSize = 13.sp, maxLines = 3,
                    overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
            }
            if (n.clearable) Box(Modifier.size(28.dp).clip(RoundedCornerShape(14.dp))
                .clickable { GlassNotificationService.dismiss(n.key) }, contentAlignment = Alignment.Center) {
                Icon(Glyph.close, "Buang", tint = Muted, modifier = Modifier.size(14.dp))
            }
        }
        if (n.actions.isNotEmpty()) Row(Modifier.padding(start = 46.dp, top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            n.actions.forEach { a ->
                Box(Modifier.height(34.dp).glass(17.dp, strong = false).clickable {
                    if (a.remoteInput != null) replying = !replying
                    else { GlassNotificationService.run(context, a); onClose() }
                }.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                    Text(a.title, color = Color.White, fontSize = 13.sp, maxLines = 1)
                }
            }
        }
        if (replying) Row(Modifier.padding(start = 46.dp, top = 10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(20.dp)).background(Color(0x40000000))
                .padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
                if (reply.isEmpty()) Text("Tulis balasan…", color = Muted, fontSize = 13.sp)
                BasicTextField(reply, { reply = it }, singleLine = true, cursorBrush = SolidColor(Color.White),
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp), modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.width(8.dp))
            Pill("Hantar", null) {
                val a = n.replyAction
                if (a != null && reply.isNotBlank() && GlassNotificationService.reply(context, a, reply)) {
                    replying = false; reply = ""; Actions.toast(context, "Balasan dihantar")
                }
            }
        }
    }
}

private data class Tile(val label: String, val icon: ImageVector, val on: Boolean, val onClick: () -> Unit)

@Composable
private fun QuickSettings(now: Long, battery: Int, modifier: Modifier, showNotifications: Boolean, apps: List<AppEntry>,
                          onClose: () -> Unit) {
    val context = LocalContext.current
    val wifi = rememberPolled(2000) { Sys.wifiOn(it) }
    val bt = rememberPolled(2000) { Sys.bluetoothOn(it) }
    val dnd = rememberPolled(1500) { Focus.isOn(it) }
    val dns = rememberPolled(5000) { Sys.privateDns(it) }
    val dark = rememberPolled(5000) { Sys.darkMode(it) }
    val plane = rememberPolled(3000) { Sys.airplane(it) }
    val tiles = listOf(
        Tile("Wi-Fi", Glyph.wifi, wifi) { Actions.settings(context, Settings.Panel.ACTION_WIFI) },
        Tile("Bluetooth", Glyph.bluetooth, bt) { Actions.settings(context, Settings.ACTION_BLUETOOTH_SETTINGS) },
        Tile("Fokus", Glyph.moon, dnd) { Focus.toggle(context) },
        Tile("Sekat Iklan", Glyph.shield, dns != null) {
            Actions.toast(context, if (dns != null) "Private DNS aktif: $dns" else "Hidupkan Private DNS: dns.adguard-dns.com")
            Actions.settings(context, Settings.ACTION_WIRELESS_SETTINGS)
        },
        Tile("Mod Gelap", Glyph.contrast, dark) { Actions.settings(context, Settings.ACTION_DISPLAY_SETTINGS) },
        Tile("Tangkap Skrin", Glyph.shot, false) {
            onClose(); Actions.toast(context, "Tekan butang Kuasa + Kelantangan Bawah serentak")
        },
        Tile("Rakam Skrin", Glyph.record, false) {
            val rec = apps.firstOrNull { "screenrecord" in it.packageName.lowercase() }
            if (rec != null) { Actions.launch(context, rec); onClose() }
            else Actions.toast(context, "Guna Rakam Skrin dalam Pusat Kawalan HONOR")
        },
        Tile("Mod Pesawat", Glyph.plane, plane) { Actions.settings(context, Settings.ACTION_AIRPLANE_MODE_SETTINGS) },
    )
    Column(modifier.glass(26.dp).clickable(remember { MutableInteractionSource() }, null) {}
        .verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(fmt("h:mm", now), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("${fmt("EEEE, d MMM", now)} · $battery%", color = Muted, fontSize = 13.sp)
        }
        tiles.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { t ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).clickable { t.onClick() }.padding(vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(54.dp).clip(RoundedCornerShape(18.dp))
                            .background(if (t.on) ON else Color(0x1AFFFFFF))
                            .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                            Icon(t.icon, t.label, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(t.label, color = Color.White, fontSize = 11.sp, maxLines = 2, lineHeight = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }
        Sliders()
        MediaCard()
        FocusCard()
        if (showNotifications) NotificationList(apps, Modifier.fillMaxWidth().height(420.dp), onClose)
    }
}

@Composable
private fun Sliders() {
    val context = LocalContext.current
    var bright by remember { mutableFloatStateOf(Sys.brightness(context) / 255f) }
    var vol by remember { mutableFloatStateOf(Sys.volume(context)) }
    var asked by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().glass(18.dp, strong = false).padding(horizontal = 14.dp, vertical = 6.dp)) {
        SliderRow(Glyph.sun, "Kecerahan", bright) {
            bright = it
            if (Sys.canWriteSettings(context)) Sys.setBrightness(context, (it * 255).toInt())
            else if (!asked) {
                asked = true
                Actions.toast(context, "Benarkan \"Ubah suai tetapan sistem\" untuk kawal kecerahan")
                Actions.start(context, Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}")))
            }
        }
        SliderRow(Glyph.volume, "Kelantangan", vol) { vol = it; Sys.setVolume(context, it) }
    }
}

@Composable
private fun SliderRow(icon: ImageVector, label: String, value: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = Color.White, fontSize = 12.sp, modifier = Modifier.width(78.dp))
        Slider(value, onChange, Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = Color.White,
            activeTrackColor = Color.White, inactiveTrackColor = Color(0x40FFFFFF)))
    }
}

@Composable
fun MediaCard(modifier: Modifier = Modifier.fillMaxWidth()) {
    val context = LocalContext.current
    val np = rememberPolled(1500) { Media.now(it) }
    Row(modifier.glass(18.dp, strong = false).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        val art = remember(np?.art) { np?.art?.asImageBitmap() }
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF8A4DFF)), contentAlignment = Alignment.Center) {
            if (art != null) Image(art, null, Modifier.size(48.dp))
            else Icon(Glyph.music, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(if (np == null) "Tiada media" else "Sedang dimainkan", color = Muted, fontSize = 12.sp)
            Text(np?.title ?: "Mainkan sesuatu dalam aplikasi muzik/video", color = Color.White, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!np?.artist.isNullOrBlank()) Text(np!!.artist, color = Muted, fontSize = 12.sp, maxLines = 1)
        }
        if (np != null) {
            GlassIconButton(Glyph.prev, "Sebelum", size = 38.dp) { Media.prev(context) }
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(21.dp)).background(Color.White).clickable { Media.playPause(context) },
                contentAlignment = Alignment.Center) {
                Icon(if (np.playing) Glyph.pause else Glyph.playFill, null, tint = Color(0xFF141C2B), modifier = Modifier.size(16.dp))
            }
            GlassIconButton(Glyph.next, "Seterusnya", size = 38.dp) { Media.next(context) }
        }
    }
}

@Composable
private fun FocusCard() {
    val context = LocalContext.current
    val on = rememberPolled(1500) { Focus.isOn(it) }
    Row(Modifier.fillMaxWidth().glass(18.dp, strong = false).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Glyph.moon, null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(if (on) "Mod Fokus: Aktif" else "Mod Fokus", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(if (on) "Notifikasi disenyapkan" else "Senyapkan notifikasi dengan satu ketik", color = Muted, fontSize = 12.sp)
        }
        Pill(if (on) "Tamatkan" else "Mulakan", null) { Focus.toggle(context) }
    }
}
