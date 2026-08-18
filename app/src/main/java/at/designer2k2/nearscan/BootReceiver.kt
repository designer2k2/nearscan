package at.designer2k2.nearscan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import at.designer2k2.nearscan.prefs.SettingsDataStore
import at.designer2k2.nearscan.service.ScanService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A routine reboot (OTA update, battery pull, OEM scheduled restart) otherwise ends a
 * multi-hour/multi-day scan session with no Tasker `SCAN_STOPPED` event and no user-visible
 * signal — the app just looks like it's silently not scanning anymore. This can't restart
 * scanning directly: Android 15+ forbids launching a `location`-type foreground service from a
 * BOOT_COMPLETED receiver, so it only prompts the user to reopen the app.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsDataStore: SettingsDataStore

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val (wasActive, _) = settingsDataStore.sessionActive.first()
                if (wasActive) {
                    ScanService.notifySessionInterruptedByReboot(context)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
