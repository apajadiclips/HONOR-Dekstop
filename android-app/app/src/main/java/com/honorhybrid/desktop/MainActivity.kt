package com.honorhybrid.desktop

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.honorhybrid.desktop.data.AppEntry
import com.honorhybrid.desktop.data.AppRepository
import com.honorhybrid.desktop.data.Prefs
import com.honorhybrid.desktop.ui.Launcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val apps = mutableStateOf<List<AppEntry>>(emptyList())
    private val homeTick = mutableIntStateOf(0)
    private lateinit var prefs: Prefs

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = reload()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        prefs = Prefs(this)
        registerReceiver(packageReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        })
        reload()
        lifecycleScope.launch { snapshotFlow { prefs.hideStatusBar }.collect { applyBars() } }
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Launcher(apps.value, prefs, homeTick.intValue)
            }
        }
    }

    private fun reload() {
        lifecycleScope.launch {
            val list = withContext(Dispatchers.IO) { AppRepository.load(this@MainActivity) }
            prefs.seedDefaults(list)
            prefs.ensureStartPins(list)
            apps.value = list
        }
    }

    /** Optional: hide the system status bar on the desktop (swipe from the top shows it briefly). */
    private fun applyBars() {
        val c = WindowCompat.getInsetsController(window, window.decorView)
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (prefs.hideStatusBar) c.hide(WindowInsetsCompat.Type.statusBars()) else c.show(WindowInsetsCompat.Type.statusBars())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && ::prefs.isInitialized) applyBars()
    }

    /** Pressing Home while the launcher is showing closes any open panel. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        homeTick.intValue++
    }

    override fun onDestroy() {
        unregisterReceiver(packageReceiver)
        super.onDestroy()
    }
}
