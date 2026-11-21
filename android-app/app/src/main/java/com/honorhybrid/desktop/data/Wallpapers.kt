package com.honorhybrid.desktop.data

import androidx.compose.ui.graphics.Color

data class WallpaperPreset(val id: String, val name: String, val base: Color, val blobs: List<Color>, val accent: Color)

object Wallpapers {
    val presets = listOf(
        WallpaperPreset("aurora", "Aurora", Color(0xFF0B1426),
            listOf(Color(0xFF2F5BFF), Color(0xFF8A4DFF), Color(0xFF12A99B), Color(0xFFFF7A45)), Color(0xFF5B9BFF)),
        WallpaperPreset("senja", "Senja", Color(0xFF2A1020),
            listOf(Color(0xFFFF6B4A), Color(0xFFC2417A), Color(0xFFFFB347), Color(0xFF6B2FBF)), Color(0xFFFF8A5C)),
        WallpaperPreset("lautan", "Lautan", Color(0xFF04202B),
            listOf(Color(0xFF0E7C9A), Color(0xFF2F5BFF), Color(0xFF19C3B4), Color(0xFF0B4F6C)), Color(0xFF3CC8D6)),
        WallpaperPreset("hutan", "Hutan", Color(0xFF0C1E14),
            listOf(Color(0xFF2E8B4E), Color(0xFF13695A), Color(0xFF7FD46A), Color(0xFFC9B44A)), Color(0xFF7FD46A)),
        WallpaperPreset("neon", "Neon", Color(0xFF0A0A18),
            listOf(Color(0xFFFF2E88), Color(0xFF7A2CFF), Color(0xFF00D1FF), Color(0xFFFFD000)), Color(0xFFFF5FA8)),
    )
    fun byId(id: String) = presets.firstOrNull { it.id == id } ?: presets.first()

    /** Each zone gets its own wallpaper, as in the design. */
    fun forZone(zone: Zone) = when (zone) {
        Zone.GAME -> byId("neon")
        Zone.SOSIAL -> byId("lautan")
        Zone.HIBURAN -> byId("senja")
        Zone.KERJA -> byId("aurora")
        Zone.KREATIF -> byId("hutan")
        Zone.ALAT -> byId("lautan")
    }
}
