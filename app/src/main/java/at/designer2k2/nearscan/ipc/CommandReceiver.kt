package at.designer2k2.nearscan.ipc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import at.designer2k2.nearscan.db.BtScanDao
import at.designer2k2.nearscan.db.CellScanDao
import at.designer2k2.nearscan.db.WifiScanDao
import at.designer2k2.nearscan.export.ExportManager
import at.designer2k2.nearscan.prefs.ExportFormat
import at.designer2k2.nearscan.prefs.NearScanSettings
import at.designer2k2.nearscan.prefs.SettingsDataStore
import at.designer2k2.nearscan.service.ScanService
import at.designer2k2.nearscan.util.FlexibleExtra
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
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
    @Inject lateinit var wifiScanDao: WifiScanDao
    @Inject lateinit var btScanDao: BtScanDao
    @Inject lateinit var cellScanDao: CellScanDao

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in ALL_ACTIONS) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = settingsDataStore.settings.first()
                if (!isAllowed(action, settings)) {
                    Log.w(TAG, "Ignoring $action: disabled in Advanced Settings › Automation")
                    return@launch
                }
                when (action) {
                    ACTION_START -> ScanService.start(context)
                    ACTION_STOP -> ScanService.stop(context)
                    ACTION_TOGGLE -> {
                        if (ScanService.statusFlow.value.isRunning) ScanService.stop(context)
                        else ScanService.start(context)
                    }
                    ACTION_EXPORT -> handleExport(intent, settings)
                    ACTION_SET_LOCATION -> handleSetLocation(intent)
                    ACTION_SET_INTERVAL -> handleSetInterval(intent, settings)
                    ACTION_CLEAR_DATA -> handleClearData()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // This scope has no exception handler, so an escaping exception (e.g. an export
                // hitting a full disk) would reach the default handler and kill the whole process
                // — taking a running ScanService session down with it.
                Log.e(TAG, "Command $action failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handleExport(intent: Intent, settings: NearScanSettings) {
        val format = ExportFormat.fromKey(intent.getStringExtra("format"))
        val outputDir = exportManager.resolveOutputDir(settings.outputFolder)
        val file = exportManager.export(format, outputDir)
        val count = exportManager.totalRecordCount()
        taskerBroadcaster.onExportComplete(file, format.key, count)
    }

    private suspend fun handleSetLocation(intent: Intent) {
        if (!intent.hasExtra("lat") || !intent.hasExtra("lon")) return
        val lat = intent.getFlexibleDouble("lat") ?: return
        val lon = intent.getFlexibleDouble("lon") ?: return
        val alt = intent.getFlexibleDouble("alt") ?: 0.0
        settingsDataStore.updateLocation(lat, lon, alt)
    }

    private suspend fun handleSetInterval(intent: Intent, current: NearScanSettings) {
        val type = intent.getStringExtra("type") ?: return
        val intervalSec = intent.getFlexibleInt("interval_sec") ?: return
        if (intervalSec <= 0) return
        val updated = when (type.lowercase()) {
            "wifi" -> current.copy(intervalWifiSec = intervalSec)
            "bt" -> current.copy(intervalBtSec = intervalSec)
            "ble" -> current.copy(intervalBleSec = intervalSec)
            "cell" -> current.copy(intervalCellSec = intervalSec)
            else -> return
        }
        settingsDataStore.update(updated)
    }

    private suspend fun handleClearData() {
        wifiScanDao.clearAll()
        btScanDao.clearAll()
        cellScanDao.clearAll()
        ScanService.resetCounts()
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
        const val ACTION_CLEAR_DATA = "at.designer2k2.nearscan.CMD_CLEAR_DATA"

        private const val TAG = "CommandReceiver"

        private val ALL_ACTIONS = setOf(
            ACTION_START, ACTION_STOP, ACTION_TOGGLE, ACTION_EXPORT,
            ACTION_SET_LOCATION, ACTION_SET_INTERVAL, ACTION_CLEAR_DATA,
        )

        /**
         * The receiver is exported without a permission (Tasker can't hold a custom one), so this
         * user-controlled gate is the only thing between an arbitrary installed app and these
         * commands. CMD_CLEAR_DATA additionally needs its own opt-in since it's irreversible.
         */
        fun isAllowed(action: String, settings: NearScanSettings): Boolean =
            settings.automationCommandsEnabled &&
                (action != ACTION_CLEAR_DATA || settings.automationClearDataEnabled)
    }
}
