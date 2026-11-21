package com.honorhybrid.desktop.ui

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.honorhybrid.desktop.data.ClockPos
import com.honorhybrid.desktop.data.HomeMode
import com.honorhybrid.desktop.data.IconSize
import com.honorhybrid.desktop.data.Prefs

@Composable
fun SettingsPanel(prefs: Prefs, onWallpaper: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    Box(Modifier.fillMaxSize().background(Color(0x88000000))
        .clickable(remember { MutableInteractionSource() }, null) { onClose() }, contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(24.dp).widthIn(max = 560.dp).glass(26.dp)
                .clickable(remember { MutableInteractionSource() }, null) {}
                .verticalScroll(rememberScrollState()).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Tetapan desktop", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)

            Choice("Mod paparan", HomeMode.entries, prefs.mode, { it.label }) { prefs.setHomeMode(it) }
            Choice("Kedudukan jam", ClockPos.entries, prefs.clockPos,
                { if (it == ClockPos.ATAS) "Atas (widget)" else "Bawah (taskbar)" }) { prefs.setClock(it) }
            Choice("Saiz ikon", IconSize.entries, prefs.iconSize, { it.label }) { prefs.setIcon(it) }

            Toggle("Bar ruangan di tengah atas", prefs.showZoneBar) { prefs.setZoneBar(it) }
            Toggle("Widget Debloat Manager", prefs.showDebloatWidget) { prefs.setDebloatWidget(it) }
            Toggle("Leret ke bawah untuk panel Liquid Glass", prefs.swipePanel) { prefs.setSwipe(it) }
            Toggle("Sembunyikan bar status sistem", prefs.hideStatusBar) { prefs.setHideStatus(it) }

            Text("Tip: tekan lama ikon dan seret untuk ubah kedudukan. Lepaskan di atas ikon lain untuk buat folder. " +
                "Tekan lama ruang kosong di desktop untuk buka tetapan ini.", color = Muted, fontSize = 12.sp)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton("Wallpaper", Modifier.weight(1f)) { onWallpaper() }
                PillButton("Buka HONOR Home", Modifier.weight(1f)) { Actions.openHonorHome(context); onClose() }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton("Tukar launcher lalai", Modifier.weight(1f)) {
                    Actions.settings(context, Settings.ACTION_HOME_SETTINGS); onClose()
                }
                PillButton("Selesai", Modifier.weight(1f)) { onClose() }
            }
        }
    }
}

@Composable
private fun <T> Choice(title: String, options: List<T>, selected: T, label: (T) -> String, onPick: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = Muted, fontSize = 13.sp)
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0x33000000)).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEach { o ->
                Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(10.dp))
                    .background(if (o == selected) Color(0x40FFFFFF) else Color.Transparent)
                    .clickable { onPick(o) }, contentAlignment = Alignment.Center) {
                    Text(label(o), color = Color.White, fontSize = 13.sp,
                        fontWeight = if (o == selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun Toggle(title: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!value) }, verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(value, onChange)
    }
}

@Composable
fun PillButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.height(44.dp).glass(14.dp, strong = false).clickable(onClick = onClick).padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center) {
        Text(label, color = Color.White, fontSize = 13.sp, maxLines = 1)
    }
}
