package at.designer2k2.nearscan.ipc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import at.designer2k2.nearscan.export.ExportManager
import at.designer2k2.nearscan.prefs.ExportFormat
import at.designer2k2.nearscan.prefs.SettingsDataStore
import at.designer2k2.nearscan.service.ScanService
import at.designer2k2.nearscan.util.FlexibleExtra
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Manifest-declared receiver that lets automation apps (Tasker, MacroDroid, Automate) control
 * NearScan via standard Android broadcasts.
 *
 * Tasker setup: Action › Send Intent, fill in action and package "at.designer2k2.nearscan".
 * No plugin or Tasker plug-in APK required.
 */
@AndroidEntryPoint
class CommandReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsDataStore: SettingsDataStore
    @Inject lateinit var exportManager: ExportManager
    @Inject lateinit var taskerBroadcaster: TaskerBroadcaster

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_START -> ScanService.start(context)
            ACTION_STOP -> ScanService.stop(context)
            ACTION_TOGGLE -> {
                if (ScanService.statusFlow.value.isRunning) ScanService.stop(context)
                else ScanService.start(context)
            }
            ACTION_EXPORT -> handleExport(context, intent)
            ACTION_SET_LOCATION -> handleSetLocation(intent)
            ACTION_SET_INTERVAL -> handleSetInterval(intent)
        }
    }

    private fun handleExport(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = settingsDataStore.settings.first()
                val formatKey = intent.getStringExtra("format")
                val format = ExportFormat.fromKey(formatKey)
                val outputDir = exportManager.resolveOutputDir(settings.outputFolder)
                val file = exportManager.export(format, outputDir)
                val count = exportManager.totalRecordCount()
                taskerBroadcaster.onExportComplete(file, format.key, count)
            } finally {
                pending.finish()
            }
        }
    }

    private fun handleSetLocation(intent: Intent) {
        if (!intent.hasExtra("lat") || !intent.hasExtra("lon")) return
        val lat = intent.getFlexibleDouble("lat") ?: return
        val lon = intent.getFlexibleDouble("lon") ?: return
        val alt = intent.getFlexibleDouble("alt") ?: 0.0
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                settingsDataStore.updateLocation(lat, lon, alt)
            } finally {
                pending.finish()
            }
        }
    }

    private fun handleSetInterval(intent: Intent) {
        val type = intent.getStringExtra("type") ?: return
        val intervalSec = intent.getFlexibleInt("interval_sec") ?: return
        if (intervalSec <= 0) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val current = settingsDataStore.settings.first()
                val updated = when (type.lowercase()) {
                    "wifi" -> current.copy(intervalWifiSec = intervalSec)
                    "bt" -> current.copy(intervalBtSec = intervalSec)
                    "ble" -> current.copy(intervalBleSec = intervalSec)
                    "cell" -> current.copy(intervalCellSec = intervalSec)
                    else -> return@launch
                }
                settingsDataStore.update(updated)
            } finally {
                pending.finish()
            }
        }
    }

    private fun Intent.getFlexibleDouble(key: String): Double? =
        FlexibleExtra.parseDouble(getStringExtra(key), getDoubleExtra(key, Double.NaN))

    private fun Intent.getFlexibleInt(key: String): Int? =
        FlexibleExtra.parseInt(getStringExtra(key), getIntExtra(key, Int.MIN_VALUE))

    companion object {
        const val ACTION_START = "at.designer2k2.nearscan.CMD_START"
        const val ACTION_STOP = "at.designer2k2.nearscan.CMD_STOP"
        const val ACTION_TOGGLE = "at.designer2k2.nearscan.CMD_TOGGLE"
        const val ACTION_EXPORT = "at.designer2k2.nearscan.CMD_EXPORT"
        const val ACTION_SET_LOCATION = "at.designer2k2.nearscan.CMD_SET_LOCATION"
        const val ACTION_SET_INTERVAL = "at.designer2k2.nearscan.CMD_SET_INTERVAL"
    }
}
