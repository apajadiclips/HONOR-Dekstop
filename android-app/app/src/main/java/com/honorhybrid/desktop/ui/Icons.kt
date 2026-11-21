package com.honorhybrid.desktop.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Stroke icons from the design, built from SVG path strings (24x24 viewport). */
object Glyph {
    private fun stroke(path: String) = ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f)
        .addPath(addPathNodes(path), fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 1.9f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()

    val wifi = stroke("M2 9a15 15 0 0 1 20 0M5 13a10 10 0 0 1 14 0M8.5 16.5a5 5 0 0 1 7 0M12 20h.01")
    val bluetooth = stroke("M7 7l10 10l-5 5V2l5 5L7 17")
    val moon = stroke("M20 14a8 8 0 1 1-10-10a7 7 0 0 0 10 10z")
    val shield = stroke("M12 3l8 3v6c0 5-4 8-8 9c-4-1-8-4-8-9V6zM9 12l2 2l4-4")
    val sun = stroke("M12 8a4 4 0 1 0 0 8a4 4 0 1 0 0-8M12 2v2M12 20v2M4 12H2M22 12h-2M5 5l1.5 1.5M17.5 17.5L19 19M5 19l1.5-1.5M17.5 6.5L19 5")
    val volume = stroke("M4 9h4l5-4v14l-5-4H4zM16 9a4 4 0 0 1 0 6")
    val image = stroke("M4 5h16v14H4zM4 16l5-5l4 4l3-3l4 4M15 9h.01")
    val gear = stroke("M12 9a3 3 0 1 0 0 6a3 3 0 1 0 0-6M12 2v3M12 19v3M2 12h3M19 12h3M5 5l2 2M17 17l2 2M5 19l2-2M17 7l2-2")
    val grid = stroke("M5 5h.01M12 5h.01M19 5h.01M5 12h.01M12 12h.01M19 12h.01M5 19h.01M12 19h.01M19 19h.01")
    val search = stroke("M11 4a7 7 0 1 0 0 14a7 7 0 1 0 0-14M21 21l-5-5")
    val bell = stroke("M6 16V11a6 6 0 0 1 12 0v5l2 2H4zM10 21h4")
    val pad = stroke("M6 9h12a4 4 0 0 1 0 8c-2 0-3-2-4-2h-4c-1 0-2 2-4 2a4 4 0 0 1 0-8zM8 11v4M6 13h4M16 12h.01M18 14h.01")
    val chat = stroke("M4 5h16v11H9l-5 4z")
    val play = stroke("M4 5h16v14H4zM10 9l5 3l-5 3z")
    val home = stroke("M3 11l9-7l9 7M5 10v10h14V10")
    val globe = stroke("M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18M3 12h18M12 3c3 3 3 15 0 18M12 3c-3 3-3 15 0 18")
    val close = stroke("M6 6l12 12M18 6L6 18")
}
