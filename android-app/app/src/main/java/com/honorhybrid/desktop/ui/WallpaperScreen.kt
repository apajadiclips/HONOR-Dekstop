package com.honorhybrid.desktop.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.honorhybrid.desktop.data.AppEntry
import com.honorhybrid.desktop.data.Prefs
import com.honorhybrid.desktop.data.WallpaperPreset
import com.honorhybrid.desktop.data.Wallpapers
import com.honorhybrid.desktop.system.Sys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class Target(val label: String) { DESKTOP("Desktop"), KUNCI("Skrin kunci"), KEDUA("Kedua-dua") }

/** Full wallpaper page from the design: live preview, presets, gallery/camera, glass & dim, per-desk. */
@Composable
fun WallpaperScreen(prefs: Prefs, byKey: Map<String, AppEntry>, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var desk by remember { mutableIntStateOf(prefs.currentDesk) }
    var pick by remember(desk) { mutableStateOf(prefs.wallpaperFor(desk)) }
    var custom by remember { mutableStateOf(prefs.customWallpaper) }
    var target by remember { mutableStateOf(Target.DESKTOP) }
    var busy by remember { mutableStateOf(false) }
    val camFile = remember { File(context.filesDir, "camera_tmp.jpg") }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) scope.launch {
            withContext(Dispatchers.IO) { copyWallpaper(context, uri) }?.let { custom = it; pick = "custom" }
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) scope.launch {
            withContext(Dispatchers.IO) { copyWallpaper(context, Uri.fromFile(camFile)) }?.let { custom = it; pick = "custom" }
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF0E1626))) {
        BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp)) {
            val wide = maxWidth > 760.dp
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Wallpaper", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text("Pratonton langsung · ${if (pick == "custom") "Kustom" else Wallpapers.byId(pick).name}", color = Muted, fontSize = 13.sp)
                        Spacer(Modifier.width(10.dp))
                        GlassIconButton(Glyph.close, "Tutup") { onClose() }
                    }
                    Preview(pick, custom, prefs, byKey, Modifier.fillMaxWidth().aspectRatio(16f / 10f))
                    SectionTitle("Wallpaper setiap desktop maya")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        prefs.desks.forEachIndexed { i, name ->
                            val id = prefs.wallpaperFor(i)
                            Box(Modifier.weight(1f).aspectRatio(16f / 7f).clip(RoundedCornerShape(12.dp))
                                .border(if (i == desk) 2.dp else 1.dp, if (i == desk) Color.White else Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                                .clickable { desk = i; pick = prefs.wallpaperFor(i) }) {
                                if (id == "custom") Wallpaper(Wallpapers.byId("aurora"), prefs.customWallpaper) else PresetWallpaper(Wallpapers.byId(id))
                                Text("Desktop ${i + 1} · $name", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.align(Alignment.BottomStart).padding(10.dp))
                            }
                        }
                    }
                    if (!wide) Controls(prefs, pick, { pick = it }, target, { target = it }, busy,
                        onGallery = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        onCamera = { camera.launch(FileProvider.getUriForFile(context, "${context.packageName}.files", camFile)) },
                        onApply = { apply(context, prefs, desk, pick, custom, target, scope, { busy = it }, onClose) })
                }
                if (wide) Column(Modifier.width(400.dp).fillMaxHeight()) {
                    Controls(prefs, pick, { pick = it }, target, { target = it }, busy,
                        onGallery = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        onCamera = { camera.launch(FileProvider.getUriForFile(context, "${context.packageName}.files", camFile)) },
                        onApply = { apply(context, prefs, desk, pick, custom, target, scope, { busy = it }, onClose) })
                }
            }
        }
    }
}

private fun apply(context: Context, prefs: Prefs, desk: Int, pick: String, custom: String?, target: Target,
                  scope: kotlinx.coroutines.CoroutineScope, setBusy: (Boolean) -> Unit, onClose: () -> Unit) {
    if (target != Target.KUNCI) prefs.setWallpaperForDesk(desk, pick, if (pick == "custom") custom else null)
    if (target == Target.DESKTOP) { onClose(); return }
    setBusy(true)
    scope.launch {
        val ok = withContext(Dispatchers.IO) {
            val bmp = if (pick == "custom" && custom != null) BitmapFactory.decodeFile(custom) else renderPreset(context, Wallpapers.byId(pick))
            bmp != null && Sys.setLockWallpaper(context, bmp)
        }
        setBusy(false)
        Actions.toast(context, if (ok) "Wallpaper skrin kunci ditukar" else "Tidak dapat menukar wallpaper skrin kunci")
        onClose()
    }
}

/** Draws a preset at screen size, for the system lock screen. */
private fun renderPreset(context: Context, p: WallpaperPreset): Bitmap {
    val m = context.resources.displayMetrics
    val w = maxOf(m.widthPixels, m.heightPixels); val h = minOf(m.widthPixels, m.heightPixels)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    c.drawColor(p.base.toArgb())
    val spots = listOf(0.2f to 0.25f, 0.58f to 0.8f, 0.86f to 0.18f, 0.88f to 0.82f)
    val radii = listOf(0.42f, 0.45f, 0.36f, 0.30f)
    p.blobs.forEachIndexed { i, col ->
        val cx = spots[i].first * w; val cy = spots[i].second * h; val r = radii[i] * w
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy, r, col.copy(alpha = 0.95f).toArgb(), col.copy(alpha = 0f).toArgb(), Shader.TileMode.CLAMP)
        }
        c.drawCircle(cx, cy, r, paint)
    }
    return bmp
}

/** Copy an image into app storage so it survives without long-lived URI permissions. */
fun copyWallpaper(context: Context, uri: Uri): String? = runCatching {
    val out = File(context.filesDir, "wallpaper_${System.currentTimeMillis()}.img")
    context.contentResolver.openInputStream(uri)!!.use { input -> out.outputStream().use { input.copyTo(it) } }
    context.filesDir.listFiles { f -> f.name.startsWith("wallpaper_") && f != out }?.forEach { it.delete() }
    out.absolutePath
}.getOrNull()

@Composable
private fun Preview(pick: String, custom: String?, prefs: Prefs, byKey: Map<String, AppEntry>, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(18.dp))) {
        if (pick == "custom") Wallpaper(Wallpapers.byId("aurora"), custom) else PresetWallpaper(Wallpapers.byId(pick))
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = prefs.dim / 100f * 0.6f)))
        Row(Modifier.padding(16.dp).width(240.dp).glass(16.dp).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF2E8B4E)))
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Contoh notifikasi", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Kaca mengikut warna wallpaper", color = Muted, fontSize = 11.sp)
            }
        }
        Column(Modifier.align(Alignment.TopEnd).padding(16.dp).width(200.dp).glass(16.dp).padding(14.dp)) {
            Text(fmt("h:mm", System.currentTimeMillis()), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
            Text(fmt("EEEE, d MMMM", System.currentTimeMillis()), color = Muted, fontSize = 12.sp)
        }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(44.dp).glass(0.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center) {
            prefs.taskbar.mapNotNull { byKey[it] }.take(6).forEach {
                androidx.compose.foundation.Image(it.icon, null, Modifier.padding(horizontal = 5.dp).size(28.dp))
            }
        }
    }
}

@Composable
private fun Controls(prefs: Prefs, pick: String, onPick: (String) -> Unit, target: Target, onTarget: (Target) -> Unit,
                     busy: Boolean, onGallery: () -> Unit, onCamera: () -> Unit, onApply: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFF162033))
        .border(1.dp, Color(0xFF26354F), RoundedCornerShape(20.dp)).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle("Pilih wallpaper")
        (Wallpapers.presets.map { it.id to it.name } + ("custom" to "Kustom")).chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (id, name) ->
                    Column(Modifier.weight(1f).clickable { if (id == "custom" && prefs.customWallpaper == null) onGallery() else onPick(id) },
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(12.dp))
                            .border(if (pick == id) 2.dp else 1.dp, if (pick == id) Color.White else Color(0x33FFFFFF), RoundedCornerShape(12.dp))) {
                            if (id == "custom") Wallpaper(Wallpapers.byId("aurora"), prefs.customWallpaper ?: "")
                            else PresetWallpaper(Wallpapers.byId(id))
                        }
                        Text(name, color = Color(0xFFE3E9F2), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF2F6FE0)).clickable(onClick = onGallery),
                contentAlignment = Alignment.Center) { Text("Pilih dari Galeri", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
            Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Color(0xFF33496B), RoundedCornerShape(12.dp))
                .clickable(onClick = onCamera), contentAlignment = Alignment.Center) { Text("Ambil gambar", color = Color.White, fontSize = 14.sp) }
        }
        LabeledSlider("Kabur kaca", prefs.glass) { prefs.changeGlass(it) }
        LabeledSlider("Kegelapan", prefs.dim) { prefs.changeDim(it) }
        Check("Warna aksen ikut wallpaper", prefs.accentFollow) { prefs.changeAccentFollow(it) }
        Check("Berbeza setiap desktop maya", prefs.perDeskWallpaper) { prefs.setPerDesk(it) }
        Check("Tukar automatik setiap hari", prefs.dailyWallpaper) { prefs.setDaily(it) }
        Text("Guna pada", color = Muted, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF0F1828)).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Target.entries.forEach { t ->
                Box(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(9.dp))
                    .background(if (t == target) Color(0xFF2A3B58) else Color.Transparent).clickable { onTarget(t) },
                    contentAlignment = Alignment.Center) {
                    Text(t.label, color = Color.White, fontSize = 13.sp, fontWeight = if (t == target) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).background(Color.White)
            .clickable(enabled = !busy, onClick = onApply), contentAlignment = Alignment.Center) {
            Text(if (busy) "Menukar…" else "Guna wallpaper", color = Color(0xFF0E1626), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LabeledSlider(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.width(110.dp))
        Slider(value / 100f, { onChange((it * 100).toInt()) }, Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color(0x40FFFFFF)))
    }
}

@Composable
private fun Check(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!value) }, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Checkbox(value, onChange)
    }
}
