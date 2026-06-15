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
import at.designer2k2.nearscan.extra.ExtraFields
import at.designer2k2.nearscan.extra.ExtraFieldsCollector
import at.designer2k2.nearscan.ipc.TaskerBroadcaster
import at.designer2k2.nearscan.mqtt.MqttClient
import at.designer2k2.nearscan.mqtt.MqttPublisher
import at.designer2k2.nearscan.prefs.NearScanSettings
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
    @Inject lateinit var mqttClient: MqttClient
    @Inject lateinit var mqttPublisher: MqttPublisher
    @Inject lateinit var extraFieldsCollector: ExtraFieldsCollector
    @Inject lateinit var taskerBroadcaster: TaskerBroadcaster

    private val scope = CoroutineScope(SupervisorJob())
    private val jobs = mutableListOf<Job>()

    // Live snapshot of settings, kept up to date by a collector so location and MQTT
    // can be toggled mid-session.
    @Volatile private var currentSettings: NearScanSettings = NearScanSettings()
    private var currentLat: Double? = null
    private var currentLon: Double? = null
    private var currentAlt: Double? = null

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
        taskerBroadcaster.reset()
        status.update { it.copy(isRunning = true, sessionStartMs = System.currentTimeMillis()) }
        taskerBroadcaster.onScanStarted()

        jobs += scope.launch {
            val settings = settingsDataStore.settings.first()
            currentSettings = settings
            currentLat = settings.latitude
            currentLon = settings.longitude
            currentAlt = settings.altitude

            // Keep settings + location live so changes mid-session are picked up.
            jobs += launch {
                settingsDataStore.settings.collect { s ->
                    currentSettings = s
                    currentLat = s.latitude
                    currentLon = s.longitude
                    currentAlt = s.altitude
                }
            }

            if (settings.mqttEnabled && settings.mqttBroker.isNotBlank()) {
                runCatching { mqttClient.connect(settings.mqttBroker) }
            }

            if (settings.scanWifiEnabled) {
                jobs += launch {
                    loop(settings.intervalWifiSec) {
                        val rows = wifiScanner.scan()
                        val stamped = rows.map {
                            it.copy(latitude = currentLat, longitude = currentLon, altitude = currentAlt)
                        }
                        if (stamped.isNotEmpty()) wifiScanDao.insertAll(stamped)
                        status.update { it.copy(wifiCount = it.wifiCount + stamped.size) }
                        publishBatch(stamped)
                        updateNotification()
                        taskerBroadcaster.onNewWifi(stamped)
                        val s = status.value
                        taskerBroadcaster.onRoundComplete(s.wifiCount, s.btCount, s.cellCount)
                    }
                }
            }
            if (settings.scanBtEnabled) {
                jobs += launch {
                    loop(settings.intervalBtSec) {
                        val rows = bluetoothScanner.scan()
                        val stamped = rows.map {
                            it.copy(latitude = currentLat, longitude = currentLon, altitude = currentAlt)
                        }
                        if (stamped.isNotEmpty()) btScanDao.insertAll(stamped)
                        status.update { it.copy(btCount = it.btCount + stamped.size) }
                        publishBatch(stamped)
                        updateNotification()
                        taskerBroadcaster.onNewBt(stamped)
                        val s = status.value
                        taskerBroadcaster.onRoundComplete(s.wifiCount, s.btCount, s.cellCount)
                    }
                }
            }
            if (settings.scanBleEnabled) {
                jobs += launch {
                    loop(settings.intervalBleSec) {
                        val rows = bleScanner.scan()
                        val stamped = rows.map {
                            it.copy(latitude = currentLat, longitude = currentLon, altitude = currentAlt)
                        }
                        if (stamped.isNotEmpty()) btScanDao.insertAll(stamped)
                        status.update { it.copy(btCount = it.btCount + stamped.size) }
                        publishBatch(stamped)
                        updateNotification()
                        taskerBroadcaster.onNewBle(stamped)
                        val s = status.value
                        taskerBroadcaster.onRoundComplete(s.wifiCount, s.btCount, s.cellCount)
                    }
                }
            }
            if (settings.scanCellEnabled) {
                jobs += launch {
                    loop(settings.intervalCellSec) {
                        val rows = cellScanner.scan()
                        val stamped = rows.map {
                            it.copy(latitude = currentLat, longitude = currentLon, altitude = currentAlt)
                        }
                        if (stamped.isNotEmpty()) cellScanDao.insertAll(stamped)
                        status.update { it.copy(cellCount = it.cellCount + stamped.size) }
                        publishBatch(stamped)
                        updateNotification()
                        taskerBroadcaster.onNewCell(stamped)
                        val s = status.value
                        taskerBroadcaster.onRoundComplete(s.wifiCount, s.btCount, s.cellCount)
                    }
                }
            }
        }
    }

    /** Publishes a stamped batch over MQTT when enabled, including any enabled extra fields. */
    private fun publishBatch(entities: List<Any>) {
        if (entities.isEmpty()) return
        val settings = currentSettings
        if (!settings.mqttEnabled) return
        val extras: ExtraFields? =
            if (settings.anyExtraFieldEnabled) runCatching { extraFieldsCollector.collect(settings) }.getOrNull()
            else null
        entities.forEach { entity ->
            mqttPublisher.publish(settings.mqttTopic, entity, currentLat, currentLon, currentAlt, extras)
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
        val s = status.value
        val durationS = if (s.sessionStartMs > 0L) (System.currentTimeMillis() - s.sessionStartMs) / 1000L else 0L
        jobs.forEach { it.cancel() }
        jobs.clear()
        runCatching { mqttClient.disconnect() }
        taskerBroadcaster.onScanStopped(s.wifiCount, s.btCount, s.cellCount, durationS)
        taskerBroadcaster.reset()
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
        val notification = buildNotification(getString(R.string.notification_running))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(contentText: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

    /** Refreshes the foreground notification with live per-type counts. */
    private fun updateNotification() {
        val s = status.value
        if (!s.isRunning) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = buildNotification(
            getString(R.string.notification_counts, s.wifiCount, s.btCount, s.cellCount),
        )
        nm.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Fire SCAN_STOPPED if the OS kills the service unexpectedly.
        if (status.value.isRunning) {
            val s = status.value
            val durationS = if (s.sessionStartMs > 0L) (System.currentTimeMillis() - s.sessionStartMs) / 1000L else 0L
            taskerBroadcaster.onScanStopped(s.wifiCount, s.btCount, s.cellCount, durationS)
            taskerBroadcaster.reset()
        }
        jobs.forEach { it.cancel() }
        scope.coroutineContext[Job]?.cancel()
        runCatching { mqttClient.disconnect() }
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
