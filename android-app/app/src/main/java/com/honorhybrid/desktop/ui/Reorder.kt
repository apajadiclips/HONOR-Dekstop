package com.honorhybrid.desktop.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

/**
 * A grid whose items can be re-ordered: long-press and drag to a new place.
 * Tap -> [onTap]; long-press without moving -> [onLongPress].
 */
@Composable
fun <T> ReorderGrid(
    items: List<T>, key: (T) -> String, cellW: Dp, cellH: Dp, modifier: Modifier = Modifier,
    onMove: (from: Int, to: Int) -> Unit, onTap: (T) -> Unit, onLongPress: (T) -> Unit,
    content: @Composable (T, Boolean) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val d = LocalDensity.current
        val cols = (maxWidth / cellW).toInt().coerceAtLeast(1)
        val cw = with(d) { cellW.toPx() }; val ch = with(d) { cellH.toPx() }
        val slop = with(d) { with(androidx.compose.ui.unit.Dp(18f)) { toPx() } }
        val rows = (items.size + cols - 1) / cols
        Box(Modifier.fillMaxWidth().height(cellH * rows.coerceAtLeast(1))) {
            items.forEachIndexed { index, item ->
                key(key(item)) {
                    var drag by remember { mutableStateOf(Offset.Zero) }
                    var dragging by remember { mutableStateOf(false) }
                    val idx by rememberUpdatedState(index)
                    val it by rememberUpdatedState(item)
                    val x = (index % cols) * cw; val y = (index / cols) * ch
                    Box(Modifier
                        .offset { IntOffset((x + drag.x).roundToInt(), (y + drag.y).roundToInt()) }
                        .zIndex(if (dragging) 10f else 0f).scale(if (dragging) 1.1f else 1f).width(cellW)
                        .pointerInput(key(item)) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val long = awaitLongPressOrCancellation(down.id)
                                if (long == null) {
                                    if (currentEvent.changes.all { c -> c.changedToUp() }) onTap(it)
                                    return@awaitEachGesture
                                }
                                dragging = true
                                var total = Offset.Zero
                                drag(long.id) { c -> total += c.positionChange(); drag = total; c.consume() }
                                dragging = false
                                if (total.getDistance() < slop) onLongPress(it) else {
                                    val col = (((idx % cols) * cw + total.x) / cw).roundToInt().coerceIn(0, cols - 1)
                                    val row = (((idx / cols) * ch + total.y) / ch).roundToInt().coerceAtLeast(0)
                                    onMove(idx, (row * cols + col).coerceIn(0, items.size - 1))
                                }
                                drag = Offset.Zero
                            }
                        }) { content(item, dragging) }
                }
            }
        }
    }
}
