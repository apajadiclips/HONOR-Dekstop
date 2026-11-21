package com.honorhybrid.desktop.ui

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.honorhybrid.desktop.system.Sys

/** Virtual desktops, recently used apps ("Penukar tugas") and clipboard history. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Multitask(apps: List<AppEntry>, prefs: Prefs, onClose: () -> Unit) {
    val context = LocalContext.current
    val usage = rememberPolled(10_000) { Sys.hasUsage(it) }
    val lastUsed = rememberPolled(10_000, usage) { if (usage) Sys.lastUsed(it) else emptyMap() }
    val byPkg = remember(apps) { apps.groupBy { it.packageName }.mapValues { it.value.first() } }
    val byKey = remember(apps) { apps.associateBy { it.key } }
    val recent = remember(lastUsed, prefs.recents, apps) {
        if (lastUsed.isNotEmpty())
            lastUsed.entries.sortedByDescending { it.value }.mapNotNull { e -> byPkg[e.key]?.let { it to e.value } }.take(8)
        else prefs.recents.mapNotNull { byKey[it] }.map { it to prefs.lastLaunch(it.key) }.take(8)
    }
    var selected by remember { mutableIntStateOf(0) }
    var renameDesk by remember { mutableStateOf<Int?>(null) }

    // Android only lets the app in front read the clipboard: capture it whenever this panel opens.
    LaunchedEffect(Unit) {
        runCatching {
            context.getSystemService(ClipboardManager::class.java).primaryClip?.getItemAt(0)
                ?.coerceToText(context)?.toString()?.let { prefs.addClip(it) }
        }
    }
    renameDesk?.let { i ->
        RenameDialog("Nama desktop", prefs.desks[i], onDone = { prefs.renameDesk(i, it); renameDesk = null },
            onDismiss = { renameDesk = null })
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0x55000000))
        .clickable(remember { MutableInteractionSource() }, null) { onClose() }) {
        val wide = maxWidth > 700.dp
        Column(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 84.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
            // Virtual desktops
            Row(Modifier.glass(22.dp).clickable(remember { MutableInteractionSource() }, null) {}
                .horizontalScroll(rememberScrollState()).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                prefs.desks.forEachIndexed { i, name ->
                    var menu by remember { mutableStateOf(false) }
                    Box {
                        Column(Modifier.combinedClickable(onClick = { prefs.switchDesk(i); onClose() }, onLongClick = { menu = true }),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.width(150.dp).height(86.dp).clip(RoundedCornerShape(12.dp))
                                .border(if (i == prefs.currentDesk) 2.dp else 1.dp,
                                    if (i == prefs.currentDesk) Color.White else Color(0x40FFFFFF), RoundedCornerShape(12.dp))) {
                                PresetWallpaper(Wallpapers.byId(prefs.wallpaperFor(i).let { if (it == "custom") "aurora" else it }))
                            }
                            Text("Desktop ${i + 1} · $name", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(text = { Text("Tukar nama") }, onClick = { menu = false; renameDesk = i })
                            if (prefs.desks.size > 1) DropdownMenuItem(text = { Text("Padam desktop") },
                                onClick = { menu = false; prefs.removeDesk(i) })
                        }
                    }
                }
                Column(Modifier.clickable { prefs.addDesk() }, horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.width(150.dp).height(86.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x14FFFFFF))
                        .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Text("+", color = Color.White, fontSize = 24.sp)
                    }
                    Text("Desktop baharu", color = Color.White, fontSize = 12.sp)
                }
            }

            // Task switcher (recently used apps)
            Column(Modifier.widthIn(max = 920.dp).fillMaxWidth().glass(28.dp)
                .clickable(remember { MutableInteractionSource() }, null) {}.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Penukar tugas", Modifier.weight(1f))
                    Text("Ketik untuk tukar · seret jari dari bawah untuk Terkini sistem", color = Muted, fontSize = 12.sp)
                }
                if (!usage) Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Benarkan \"Akses penggunaan\" supaya senarai ini tepat (termasuk aplikasi yang tidak dibuka dari launcher).",
                        color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Pill("Benarkan", null) { Actions.settings(context, android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS) }
                }
                if (recent.isEmpty()) Text("Belum ada aplikasi terkini.", color = Color.White, fontSize = 14.sp)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    recent.forEachIndexed { i, (a, t) ->
                        Column(Modifier.width(if (wide) 196.dp else 160.dp).clip(RoundedCornerShape(16.dp))
                            .background(if (i == selected) Color(0x38FFFFFF) else Color.Transparent)
                            .clickable { selected = i; Actions.launch(context, a); onClose() }.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xF0F4F6FA)),
                                contentAlignment = Alignment.Center) { Image(a.icon, null, Modifier.size(56.dp)) }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(a.icon, null, Modifier.size(22.dp))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(prefs.labelOf(a), color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(ago(t), color = Muted, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Clipboard history
        Column(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 84.dp).width(360.dp)
            .glass(24.dp).clickable(remember { MutableInteractionSource() }, null) {}.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Sejarah papan klip", Modifier.weight(1f))
                Text("${prefs.clips.size}", color = Muted, fontSize = 12.sp)
            }
            if (prefs.clips.isEmpty()) Text("Salin teks dalam mana-mana aplikasi, kemudian buka panel ini.", color = Muted, fontSize = 12.sp)
            Column(Modifier.verticalScroll(rememberScrollState()).height(if (prefs.clips.size > 3) 230.dp else (prefs.clips.size * 70).dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                prefs.clips.forEach { c ->
                    Row(Modifier.fillMaxWidth().glass(14.dp, strong = false).clickable {
                        context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("clip", c.text))
                        Actions.toast(context, "Disalin")
                    }.padding(start = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text((if (c.pinned) "Disematkan · " else "") + ago(c.time), color = Muted, fontSize = 11.sp)
                            Text(c.text, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        GlassIconButton(Glyph.pin, "Sematkan", active = c.pinned, size = 40.dp) { prefs.togglePin(c.text) }
                        GlassIconButton(Glyph.close, "Padam", size = 40.dp) { prefs.removeClip(c.text) }
                    }
                }
            }
        }
    }
}
