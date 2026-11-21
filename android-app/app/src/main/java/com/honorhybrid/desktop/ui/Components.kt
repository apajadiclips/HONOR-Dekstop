package com.honorhybrid.desktop.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.honorhybrid.desktop.data.AppEntry
import com.honorhybrid.desktop.data.Prefs
import com.honorhybrid.desktop.data.Zone

/** Long-press menu shared by every app icon. [extra] adds context items (e.g. "Keluarkan dari folder"). */
@Composable
fun AppMenu(app: AppEntry, prefs: Prefs, expanded: Boolean, onDismiss: () -> Unit,
            extra: List<Pair<String, () -> Unit>> = emptyList()) {
    val context = LocalContext.current
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        extra.forEach { (label, action) -> DropdownMenuItem(text = { Text(label) }, onClick = { action(); onDismiss() }) }
        DropdownMenuItem(
            text = { Text(if (app.key in prefs.taskbar) "Buang dari taskbar" else "Sematkan ke taskbar") },
            onClick = { prefs.toggleTaskbar(app.key); onDismiss() })
        DropdownMenuItem(
            text = { Text(if (prefs.onDesktop(app.key)) "Buang dari desktop" else "Tambah ke desktop") },
            onClick = { prefs.toggleDesktop(app.key); onDismiss() })
        val current = prefs.zoneOf(app)
        Zone.entries.forEach { z ->
            DropdownMenuItem(
                text = { Text(if (current == z) "✓ Ruangan ${z.label}" else "Pindah ke ruangan ${z.label}") },
                onClick = { prefs.setZone(app.packageName, z.name); onDismiss() })
        }
        if (current != null) DropdownMenuItem(text = { Text("Keluarkan dari ruangan") },
            onClick = { prefs.setZone(app.packageName, "NONE"); onDismiss() })
        DropdownMenuItem(text = { Text("Maklumat aplikasi") },
            onClick = { Actions.appInfo(context, app.packageName); onDismiss() })
        if (!app.isSystem) DropdownMenuItem(text = { Text("Nyahpasang") },
            onClick = { Actions.uninstall(context, app.packageName); onDismiss() })
    }
}

/** App icon with label. Tap opens it, long-press opens the menu. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIcon(
    app: AppEntry,
    prefs: Prefs,
    iconSize: Dp = 52.dp,
    showLabel: Boolean = true,
    labelColor: Color = Color.White,
    extraMenu: List<Pair<String, () -> Unit>> = emptyList(),
    onLaunched: () -> Unit = {},
) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    Box {
        Column(
            Modifier
                .width(if (showLabel) iconSize + 34.dp else iconSize + 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClick = { Actions.launch(context, app); onLaunched() },
                    onLongClick = { menu = true },
                )
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            AppGlyph(app, iconSize)
            if (showLabel) IconLabel(app.label, labelColor)
        }
        AppMenu(app, prefs, menu, { menu = false }, extraMenu)
    }
}

@Composable
fun AppGlyph(app: AppEntry, size: Dp) =
    Image(app.icon, app.label, Modifier.size(size).shadow(6.dp, RoundedCornerShape(size / 4)))

@Composable
fun IconLabel(text: String, color: Color = Color.White) = Text(
    text, color = color, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
    textAlign = TextAlign.Center, lineHeight = 14.sp,
)

@Composable
fun GlassIconButton(icon: ImageVector, label: String, active: Boolean = false, size: Dp = 48.dp, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) Color(0x40FFFFFF) else Color.Transparent)
            .combinedClickableCompat(onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = Color.White, modifier = Modifier.size(22.dp)) }
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.combinedClickableCompat(onClick: () -> Unit) = this.combinedClickable(onClick = onClick)
