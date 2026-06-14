package at.designer2k2.nearscan.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import at.designer2k2.nearscan.R
import at.designer2k2.nearscan.db.BtScanDao
import at.designer2k2.nearscan.db.CellScanDao
import at.designer2k2.nearscan.db.WifiScanDao
import at.designer2k2.nearscan.prefs.SettingsDataStore
import at.designer2k2.nearscan.scanner.BleScanner
import at.designer2k2.nearscan.scanner.BluetoothScanner
import at.designer2k2.nearscan.scanner.CellScanner
import at.designer2k2.nearscan.scanner.WifiScanner
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that keeps the scan loops alive. Exposes its live status via a
 * companion [MutableStateFlow] that [at.designer2k2.nearscan.ui.MainViewModel] collects.
 */
@AndroidEntryPoint
class ScanService : Service() {

    @Inject lateinit var settingsDataStore: SettingsDataStore
    @Inject lateinit var wifiScanner: WifiScanner
    @Inject lateinit var bluetoothScanner: BluetoothScanner
    @Inject lateinit var bleScanner: BleScanner
    @Inject lateinit var cellScanner: CellScanner
    @Inject lateinit var wifiScanDao: WifiScanDao
    @Inject lateinit var btScanDao: BtScanDao
    @Inject lateinit var cellScanDao: CellScanDao

    private val scope = CoroutineScope(SupervisorJob())
    private val jobs = mutableListOf<Job>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopScanning()
                return START_NOT_STICKY
            }
        }
        startForegroundNotification()
        startScanning()
        return START_STICKY
    }

    private fun startScanning() {
        if (status.value.isRunning) return
        status.update { it.copy(isRunning = true, sessionStartMs = System.currentTimeMillis()) }

        jobs += scope.launch {
            val settings = settingsDataStore.settings.first()

            if (settings.scanWifiEnabled) {
                jobs += launch {
                    loop(settings.intervalWifiSec) {
                        val rows = wifiScanner.scan()
                        if (rows.isNotEmpty()) wifiScanDao.insertAll(rows)
                        status.update { it.copy(wifiCount = it.wifiCount + rows.size) }
                    }
                }
            }
            if (settings.scanBtEnabled) {
                jobs += launch {
                    loop(settings.intervalBtSec) {
                        val rows = bluetoothScanner.scan()
                        if (rows.isNotEmpty()) btScanDao.insertAll(rows)
                        status.update { it.copy(btCount = it.btCount + rows.size) }
                    }
                }
            }
            if (settings.scanBleEnabled) {
                jobs += launch {
                    loop(settings.intervalBleSec) {
                        val rows = bleScanner.scan()
                        if (rows.isNotEmpty()) btScanDao.insertAll(rows)
                        status.update { it.copy(btCount = it.btCount + rows.size) }
                    }
                }
            }
            if (settings.scanCellEnabled) {
                jobs += launch {
                    loop(settings.intervalCellSec) {
                        val rows = cellScanner.scan()
                        if (rows.isNotEmpty()) cellScanDao.insertAll(rows)
                        status.update { it.copy(cellCount = it.cellCount + rows.size) }
                    }
                }
            }
        }
    }

    private suspend fun loop(intervalSec: Int, block: suspend () -> Unit) {
        val intervalMs = (intervalSec.coerceAtLeast(1)) * 1000L
        while (scope.isActive) {
            runCatching { block() }
            delay(intervalMs)
        }
    }

    private fun stopScanning() {
        jobs.forEach { it.cancel() }
        jobs.clear()
        status.update { ScanStatus() }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            )
            nm.createNotificationChannel(channel)
        }
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_running))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        jobs.forEach { it.cancel() }
        scope.coroutineContext[Job]?.cancel()
        status.update { ScanStatus() }
    }

    /** Live scan status snapshot, shared with the UI layer. */
    data class ScanStatus(
        val isRunning: Boolean = false,
        val sessionStartMs: Long = 0L,
        val wifiCount: Int = 0,
        val btCount: Int = 0,
        val cellCount: Int = 0,
    )

    companion object {
        private const val CHANNEL_ID = "nearscan_scanning"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "at.designer2k2.nearscan.action.STOP"

        private val status = MutableStateFlow(ScanStatus())
        val statusFlow: StateFlow<ScanStatus> = status.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, ScanService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, ScanService::class.java).apply { action = ACTION_STOP }
            context.startService(intent)
        }
    }
}
