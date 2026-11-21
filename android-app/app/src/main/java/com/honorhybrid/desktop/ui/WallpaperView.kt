package com.honorhybrid.desktop.ui

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.honorhybrid.desktop.data.WallpaperPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Preset wallpapers are soft colour blobs (radial gradients - cheap on a Snapdragon 685). */
@Composable
fun PresetWallpaper(preset: WallpaperPreset, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize().background(preset.base)) {
        val w = size.width; val h = size.height; val r = maxOf(w, h)
        val spots = listOf(Offset(0.2f, 0.25f), Offset(0.58f, 0.8f), Offset(0.86f, 0.18f), Offset(0.88f, 0.82f))
        val radii = listOf(0.42f, 0.45f, 0.36f, 0.30f)
        preset.blobs.forEachIndexed { i, c ->
            val center = Offset(spots[i].x * w, spots[i].y * h)
            drawCircle(Brush.radialGradient(listOf(c.copy(alpha = 0.95f), c.copy(alpha = 0f)), center, radii[i] * r),
                radii[i] * r, center)
        }
    }
}

@Composable
fun Wallpaper(preset: WallpaperPreset, customUri: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bmp by remember(customUri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(customUri) {
        bmp = customUri?.let { withContext(Dispatchers.IO) { decode(context, it) } }
    }
    Box(modifier.fillMaxSize()) {
        val b = bmp
        if (b != null) {
            Image(b, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Color(0x22000000)))
        } else {
            PresetWallpaper(preset)
        }
    }
}

/** [source] is a file path (custom wallpapers are copied into app storage) or a content URI. */
private fun decode(context: Context, source: String): ImageBitmap? = runCatching {
    val metrics = context.resources.displayMetrics
    val target = maxOf(metrics.widthPixels, metrics.heightPixels)
    val src = if (source.startsWith("/")) ImageDecoder.createSource(java.io.File(source))
              else ImageDecoder.createSource(context.contentResolver, Uri.parse(source))
    ImageDecoder.decodeBitmap(src) { dec, info, _ ->
        val s = info.size
        val scale = target.toFloat() / maxOf(s.width, s.height)
        if (scale < 1f) dec.setTargetSize((s.width * scale).toInt(), (s.height * scale).toInt())
        dec.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }.asImageBitmap()
}.getOrNull()
