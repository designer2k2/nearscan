package at.designer2k2.nearscan.util

import android.content.Context
import android.provider.Settings

/**
 * Detects whether the app is running under Firebase Test Lab, via the system setting Google
 * documents for this exact purpose. Used to skip launching a real Android share sheet from the
 * export flow — Robo's autonomous crawler has wandered into the chooser's Quick Share entry,
 * which demands account registration on a fresh test device and stalls the whole run since Robo
 * can't back out of that external flow.
 */
object TestLab {
    fun isRunning(context: Context): Boolean =
        Settings.System.getString(context.contentResolver, "firebase.test.lab") == "true"
}
