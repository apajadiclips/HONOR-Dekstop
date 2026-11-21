package com.honorhybrid.desktop.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.honorhybrid.desktop.data.AppEntry
import com.honorhybrid.desktop.data.DesktopItem
import com.honorhybrid.desktop.data.Prefs
import kotlin.math.roundToInt

/** Grid size of the desktop, shared with "add to desktop" so new icons land on a visible cell. */
data class GridSpec(val cols: Int, val rows: Int)

/**
 * Free-form desktop: every icon sits on a grid cell. Long-press and drag to move it; drop it on another
 * icon to make a folder; long-press without moving for the menu.
 */
@Composable
fun DesktopGrid(
    byKey: Map<String, AppEntry>,
    prefs: Prefs,
    modifier: Modifier,
    onGrid: (GridSpec) -> Unit,
    onOpenFolder: (DesktopItem) -> Unit,
) {
    BoxWithConstraints(modifier) {
        val icon = prefs.iconSize.dp.dp
        val cellW = icon + 40.dp
        val cellH = icon + 48.dp
        val cols = (maxWidth / cellW).toInt().coerceAtLeast(1)
        val rows = (maxHeight / cellH).toInt().coerceAtLeast(1)
        androidx.compose.runtime.SideEffect {
            prefs.gridCols = cols; prefs.gridRows = rows; onGrid(GridSpec(cols, rows))
        }
        prefs.deskItems.forEach { item ->
            key(item.id) {
                val app = item.appKey?.let { byKey[it] }
                if (item.isFolder || app != null) DesktopCell(item, app, byKey, prefs, icon, cellW, cellH, cols, rows, onOpenFolder)
            }
        }
    }
}

@Composable
private fun DesktopCell(
    item: DesktopItem, app: AppEntry?, byKey: Map<String, AppEntry>, prefs: Prefs,
    icon: Dp, cellW: Dp, cellH: Dp, cols: Int, rows: Int, onOpenFolder: (DesktopItem) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val cw = with(density) { cellW.toPx() }
    val ch = with(density) { cellH.toPx() }
    val slop = with(density) { 18.dp.toPx() }
    var drag by remember { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val current by rememberUpdatedState(item)
    val onTap by rememberUpdatedState {
        if (current.isFolder) onOpenFolder(current) else app?.let { Actions.launch(context, it) }
    }

    Box(
        Modifier
            .offset { IntOffset((item.col * cw + drag.x).roundToInt(), (item.row * ch + drag.y).roundToInt()) }
            .zIndex(if (dragging) 10f else 0f)
            .scale(if (dragging) 1.12f else 1f)
            .width(cellW)
            .pointerInput(item.id) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val long = awaitLongPressOrCancellation(down.id)
                    if (long == null) {
                        if (currentEvent.changes.all { it.changedToUp() }) onTap()
                        return@awaitEachGesture
                    }
                    dragging = true
                    var total = Offset.Zero
                    drag(long.id) { change -> total += change.positionChange(); drag = total; change.consume() }
                    dragging = false
                    if (total.getDistance() < slop) {
                        menu = true
                    } else {
                        val it = current
                        val c = ((it.col * cw + total.x) / cw).roundToInt().coerceIn(0, cols - 1)
                        val r = ((it.row * ch + total.y) / ch).roundToInt().coerceIn(0, rows - 1)
                        prefs.move(it.id, c, r)
                    }
                    drag = Offset.Zero
                }
            },
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)) {
            if (item.isFolder) FolderGlyph(item.apps.mapNotNull { byKey[it] }, icon) else AppGlyph(app!!, icon)
            IconLabel(if (item.isFolder) item.name.ifBlank { "Folder" } else prefs.labelOf(app!!))
        }
        if (app != null) AppMenu(app, prefs, menu, { menu = false })
        else if (menu) androidx.compose.material3.DropdownMenu(expanded = true, onDismissRequest = { menu = false }) {
            androidx.compose.material3.DropdownMenuItem(text = { Text("Buka & namakan semula") },
                onClick = { menu = false; onOpenFolder(item) })
        }
    }
}

@Composable
private fun FolderGlyph(apps: List<AppEntry>, size: Dp) {
    Box(Modifier.size(size).glass(size / 4, strong = false).padding(size / 9)) {
        val mini = (size - size / 9 * 2 - 4.dp) / 2
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            apps.take(4).chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { Image(it.icon, null, Modifier.size(mini)) }
                }
            }
        }
    }
}

/** Opened folder: editable name and its apps. */
@Composable
fun FolderPopup(folder: DesktopItem, byKey: Map<String, AppEntry>, prefs: Prefs, grid: GridSpec, onClose: () -> Unit) {
    val live = prefs.items.firstOrNull { it.id == folder.id }
    if (live == null) { onClose(); return }
    var name by remember(folder.id) { mutableStateOf(live.name) }
    Box(Modifier.fillMaxSize().background(Color(0x66000000))
        .clickable(remember { MutableInteractionSource() }, null) { onClose() }, contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(24.dp).widthIn(max = 560.dp).glass(26.dp)
                .clickable(remember { MutableInteractionSource() }, null) {}.padding(20.dp),
        ) {
            Text("Nama folder", color = Muted, fontSize = 12.sp)
            BasicTextField(
                name, { name = it; prefs.renameFolder(folder.id, it) }, singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(10.dp)).padding(10.dp),
            )
            Text("Seret ikon lain ke folder ini di desktop untuk menambah. Tekan lama ikon untuk keluarkan.",
                color = Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
            LazyVerticalGrid(GridCells.Adaptive(96.dp), Modifier.heightIn(max = 420.dp)) {
                items(live.apps.mapNotNull { byKey[it] }, key = { it.key }) { a ->
                    AppIcon(a, prefs, iconSize = 54.dp, onLaunched = onClose,
                        extraMenu = listOf("Keluarkan dari folder" to {
                            prefs.removeFromFolder(folder.id, a.key, grid.cols, grid.rows)
                        }))
                }
            }
        }
    }
}
