package com.honorhybrid.desktop.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.honorhybrid.desktop.data.AppEntry

object Actions {
    fun launch(context: Context, app: AppEntry) = start(context,
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(app.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED))

    fun appInfo(context: Context, pkg: String) =
        start(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")))

    fun uninstall(context: Context, pkg: String) =
        start(context, Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")))

    fun settings(context: Context, action: String, fallback: String = Settings.ACTION_SETTINGS) {
        if (!start(context, Intent(action), quiet = true)) start(context, Intent(fallback))
    }

    fun start(context: Context, intent: Intent, quiet: Boolean = false): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
    } catch (e: ActivityNotFoundException) {
        if (!quiet) Toast.makeText(context, "Tidak dapat dibuka pada peranti ini", Toast.LENGTH_SHORT).show(); false
    } catch (e: SecurityException) {
        if (!quiet) Toast.makeText(context, "Tiada kebenaran", Toast.LENGTH_SHORT).show(); false
    }
}
