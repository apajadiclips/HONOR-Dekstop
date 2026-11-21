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
    private fun filled(path: String) = ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f)
        .addPath(addPathNodes(path), fill = SolidColor(Color.White)).build()

    val playFill = filled("M7 5l12 7l-12 7z")
    val pause = filled("M7 5h4v14H7zM13 5h4v14h-4z")
    val prev = filled("M18 6l-8 6l8 6zM6 6h2v12H6z")
    val next = filled("M6 6l8 6l-8 6zM16 6h2v12h-2z")

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
    val swap = stroke("M7 4v16M7 4L3 8M7 4l4 4M17 20V4M17 20l-4-4M17 20l4-4")
    val sliders = stroke("M4 6h10M18 6h2M4 12h4M12 12h8M4 18h12M20 18h0M14 4v4M8 10v4M16 16v4")
    val brush = stroke("M14 4l6 6l-9 9H5v-6z")
    val work = stroke("M4 8h16v11H4zM9 8V5h6v3M4 13h16")
    val tool = stroke("M14 6a4 4 0 0 0 5 5l-8 8a2 2 0 0 1-3-3l8-8a4 4 0 0 0-2-2z")
    val tablet = stroke("M6 3h12v18H6zM11 18h2")
    val windows = stroke("M3 5h11v9H3zM10 10h11v9H10z")
    val power = stroke("M12 3v9M6.3 6.3a8 8 0 1 0 11.4 0")
    val pin = stroke("M9 4h6l-1 6l3 3H7l3-3zM12 13v7")
    val timer = stroke("M12 8v5l3 2M9 2h6M12 4a9 9 0 1 0 0 18a9 9 0 1 0 0-18")
    val cast = stroke("M3 8V6h18v12h-6M3 12a6 6 0 0 1 6 6M3 15a3 3 0 0 1 3 3M3 18h.01")
    val camera = stroke("M4 8h4l2-3h4l2 3h4v11H4zM12 10a3.5 3.5 0 1 0 0 7a3.5 3.5 0 1 0 0-7")
    val record = stroke("M12 9a3 3 0 1 0 0 6a3 3 0 1 0 0-6M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18")
    val shot = stroke("M4 8V4h4M16 4h4v4M20 16v4h-4M8 20H4v-4")
    val plane = stroke("M3 11l18-7l-7 18l-2-8z")
    val contrast = stroke("M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18M12 3v18")
    val pip = stroke("M3 5h18v14H3zM12 12h7v5h-7z")
    val music = stroke("M9 18V5l11-2v13M9 18a3 3 0 1 1-6 0a3 3 0 1 1 6 0M20 16a3 3 0 1 1-6 0a3 3 0 1 1 6 0")
    val clip = stroke("M8 4h8v3H8zM6 6H5v15h14V6h-1")
}
