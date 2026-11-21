package com.honorhybrid.desktop.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.honorhybrid.desktop.data.AppEntry

object Actions {
    /** Set by the launcher to record "Terkini" (recently opened). */
    var onLaunch: ((AppEntry) -> Unit)? = null

    fun launch(context: Context, app: AppEntry) = onLaunch?.invoke(app).let { start(context,
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(app.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)) }

    fun launchPackage(context: Context, pkg: String) =
        context.packageManager.getLaunchIntentForPackage(pkg)?.let { start(context, it) } ?: false

    fun toast(context: Context, msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

    fun appInfo(context: Context, pkg: String) =
        start(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")))

    fun uninstall(context: Context, pkg: String) =
        start(context, Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")))

    fun settings(context: Context, action: String, fallback: String = Settings.ACTION_SETTINGS) {
        if (!start(context, Intent(action), quiet = true)) start(context, Intent(fallback))
    }

    /** Opens HONOR Home (MagicOS launcher) without changing the default launcher. */
    fun openHonorHome(context: Context) {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage("com.hihonor.android.launcher")
        if (!start(context, i, quiet = true)) settings(context, Settings.ACTION_HOME_SETTINGS)
    }

    fun start(context: Context, intent: Intent, quiet: Boolean = false): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
    } catch (e: ActivityNotFoundException) {
        if (!quiet) Toast.makeText(context, "Tidak dapat dibuka pada peranti ini", Toast.LENGTH_SHORT).show(); false
    } catch (e: SecurityException) {
        if (!quiet) Toast.makeText(context, "Tiada kebenaran", Toast.LENGTH_SHORT).show(); false
    }
}
