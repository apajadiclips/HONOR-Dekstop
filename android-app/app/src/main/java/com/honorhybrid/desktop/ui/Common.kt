package com.honorhybrid.desktop.ui

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val MS: Locale = Locale("ms", "MY")
fun fmt(pattern: String, t: Long): String = SimpleDateFormat(pattern, MS).format(Date(t))

fun ago(t: Long): String {
    if (t <= 0) return ""
    val m = (System.currentTimeMillis() - t) / 60_000
    return when {
        m < 1 -> "kini"
        m < 60 -> "$m min lalu"
        m < 24 * 60 -> "${m / 60} j lalu"
        m < 48 * 60 -> "Semalam"
        else -> "${m / 1440} hari lalu"
    }
}

fun minutes(m: Long): String = if (m >= 60) "${m / 60} j ${m % 60} min" else "$m min"

@Composable
fun Pill(label: String, icon: ImageVector? = null, modifier: Modifier = Modifier, active: Boolean = false,
         onClick: () -> Unit) {
    Row(
        modifier.height(44.dp).glass(14.dp, strong = false).let { if (active) it.then(Modifier) else it }
            .clickable(onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
        }
        Text(label, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) =
    Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = modifier)

/** Re-reads [read] every [ms] while on screen. */
@Composable
fun <T> rememberPolled(ms: Long, key: Any? = Unit, read: (Context) -> T): T {
    val c = LocalContext.current
    var v by remember(key) { mutableStateOf(read(c)) }
    LaunchedEffect(key) { while (true) { v = read(c); delay(ms) } }
    return v
}

@Composable
fun rememberNow(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(15_000) } }
    return now
}

@Composable
fun rememberBattery(): Int {
    val context = LocalContext.current
    var level by remember { mutableIntStateOf(-1) }
    DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                val l = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val s = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                if (l >= 0) level = l * 100 / s
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.let { receiver.onReceive(context, it) }
        onDispose { context.unregisterReceiver(receiver) }
    }
    return level
}
