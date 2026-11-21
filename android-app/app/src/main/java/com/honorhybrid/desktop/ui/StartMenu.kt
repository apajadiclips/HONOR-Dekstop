package com.honorhybrid.desktop.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.honorhybrid.desktop.data.AppEntry
import com.honorhybrid.desktop.data.Prefs
import com.honorhybrid.desktop.system.Sys

/** Start menu as in the design: search, Disematkan, Terkini, user row with power menu. */
@Composable
fun StartMenu(apps: List<AppEntry>, prefs: Prefs, onClose: () -> Unit, onSettings: () -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var showAll by remember { mutableStateOf(false) }
    var power by remember { mutableStateOf(false) }
    val byKey = remember(apps) { apps.associateBy { it.key } }
    val searching = query.isNotBlank()
    val shown = remember(apps, query, showAll, prefs.startPins, prefs.labels) {
        when {
            searching -> apps.filter { prefs.labelOf(it).contains(query, true) || it.packageName.contains(query, true) }
            showAll -> apps.sortedBy { prefs.labelOf(it).lowercase() }
            else -> prefs.startPins.mapNotNull { byKey[it] }
        }
    }
    val recent = prefs.recents.mapNotNull { byKey[it] }.take(4)

    Column(modifier.glass(22.dp).clickable(remember { MutableInteractionSource() }, null) {}) {
        Column(Modifier.weight(1f, fill = false).padding(start = 22.dp, end = 22.dp, top = 20.dp)) {
            Row(
                Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(23.dp)).background(Color(0x40000000))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(23.dp)).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Glyph.search, null, tint = Muted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Cari aplikasi, tetapan, fail", color = Muted, fontSize = 14.sp)
                    BasicTextField(query, { query = it }, singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp), cursorBrush = SolidColor(Color.White),
                        modifier = Modifier.fillMaxWidth())
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                SectionTitle(when { searching -> "${shown.size} hasil"; showAll -> "Semua aplikasi (${apps.size})"; else -> "Disematkan" },
                    Modifier.weight(1f))
                if (!searching) Text(if (showAll) "Kembali" else "Semua aplikasi  ›", color = Color(0xFF9DC1FF), fontSize = 13.sp,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { showAll = !showAll }.padding(4.dp))
            }
            LazyVerticalGrid(GridCells.Fixed(6), Modifier.heightIn(min = 100.dp, max = 420.dp)) {
                items(shown, key = { it.key }) { AppIcon(it, prefs, iconSize = 44.dp, onLaunched = onClose) }
            }
            if (!searching && !showAll && recent.isNotEmpty()) {
                SectionTitle("Terkini", Modifier.padding(top = 14.dp, bottom = 6.dp))
                recent.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { a ->
                            Row(Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                .clickable { Actions.launch(context, a); onClose() }.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)).background(Color(0x22FFFFFF)),
                                    contentAlignment = Alignment.Center) { Image(a.icon, null, Modifier.size(26.dp)) }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(prefs.labelOf(a), color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(ago(prefs.lastLaunch(a.key)), color = Muted, fontSize = 12.sp)
                                }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Row(Modifier.fillMaxWidth().background(Color(0x22000000)).padding(horizontal = 22.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            val name = remember { Sys.deviceName(context) }
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF7E5BD6)), contentAlignment = Alignment.Center) {
                Text(name.take(2).uppercase(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(10.dp))
            Text(name, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Box {
                GlassIconButton(Glyph.power, "Kuasa") { power = true }
                DropdownMenu(expanded = power, onDismissRequest = { power = false }) {
                    DropdownMenuItem(text = { Text("Tetapan desktop") }, onClick = { power = false; onSettings() })
                    DropdownMenuItem(text = { Text("Buka HONOR Home") }, onClick = { power = false; Actions.openHonorHome(context); onClose() })
                    DropdownMenuItem(text = { Text("Tetapan sistem") }, onClick = {
                        power = false; Actions.settings(context, android.provider.Settings.ACTION_SETTINGS); onClose() })
                    DropdownMenuItem(text = { Text("Tukar launcher lalai") }, onClick = {
                        power = false; Actions.settings(context, android.provider.Settings.ACTION_HOME_SETTINGS); onClose() })
                }
            }
        }
    }
}
