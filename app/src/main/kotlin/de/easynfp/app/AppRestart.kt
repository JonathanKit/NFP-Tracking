package de.easynfp.app

import android.content.Context
import android.content.Intent

/** Restarts the app process, e.g. after switching the demo mode, which selects another database. */
object AppRestart {
    fun restart(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}
