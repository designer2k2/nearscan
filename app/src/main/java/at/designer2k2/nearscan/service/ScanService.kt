package at.designer2k2.nearscan.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.text.format.DateUtils
import android.util.Log
import androidx.core.app.NotificationCompat
import at.designer2k2.nearscan.R
import at.designer2k2.nearscan.db.BtScanDao
import at.designer2k2.nearscan.db.CellScanDao
import at.designer2k2.nearscan.db.WifiScanDao
import at.designer2k2.nearscan.export.ExportManager
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Foreground service that keeps the scan loops alive. Exposes its live status via a
 * companion [MutableStateFlow] that [at.designer2k2.nearscan.ui.MainViewModel] collects.
 *
 * Each scan type is supervised independently: a small coroutine watches
 * `(enabled, intervalSec)` for that type and cancels/relaunches the inner loop whenever either
 * changes, so toggling a scan type or dragging its interval slider takes effect immediately
 * without needing to stop/restart the whole session. MQTT connect/disconnect is supervised the
 * same way, so enabling MQTT mid-session actually connects instead of silently no-op'ing.
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
    @Inject lateinit var exportManager: ExportManager

    private val scope = CoroutineScope(SupervisorJob())
    private val jobs = mutableListOf<Job>()
    private val dedupTracker = DedupTracker()

    // A foreground service alone does not keep the CPU awake once the screen turns off — only an
    // actual PowerManager wake lock does. Without this, the scan loops' delay() timers stall
    // whenever the screen is off, which is exactly the "scanning stops" symptom this fixes.
    private var wakeLock: PowerManager.WakeLock? = null

    // Live snapshot of settings, kept up to date by a collector so location, MQTT, and dedup
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
        try {
            startForegroundNotification()
        } catch (e: SecurityException) {
            // API 34+ throws SecurityException from startForeground() itself when the declared
            // foregroundServiceType="location" is called without a currently-granted location
            // permission (e.g. the user revoked it, or the broken pre-Android-12 COARSE+FINE
            // runtime dialog left it ungranted). Bail out cleanly instead of crash-looping via
            // START_STICKY.
            Log.w("ScanService", "startForeground blocked, missing location permission", e)
            stopSelf()
            return START_NOT_STICKY
        }
        acquireWakeLock()
        startScanning()
        return START_STICKY
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NearScan:ScanService").apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun startScanning() {
        if (status.value.isRunning) return

        jobs += scope.launch {
            val settings = settingsDataStore.settings.first()
            currentSettings = settings
            currentLat = settings.latitude
            currentLon = settings.longitude
            currentAlt = settings.altitude

            // Tells an OS-triggered START_STICKY restart (process died mid-session, system
            // relaunches with no ACTION_STOP) apart from a genuine fresh start: a clean stop
            // (stopScanning()) always clears this flag first, so if it's still set here, this is
            // the same logical session resuming, not a new one. Without this, a restart would
            // silently reset the notification duration, WiFi/BT/Cell counters, and Tasker
            // dedup/first-sighting state back to zero mid-session.
            val (resuming, persistedStart) = settingsDataStore.sessionActive.first()
            val sessionStart = if (resuming) persistedStart else System.currentTimeMillis()
            if (!resuming) {
                taskerBroadcaster.reset()
                dedupTracker.reset()
            }
            val restoredCounts = if (resuming) {
                withContext(Dispatchers.IO) {
                    Triple(
                        runCatching { wifiScanDao.countSince(sessionStart) }.getOrDefault(0),
                        runCatching { btScanDao.countSince(sessionStart) }.getOrDefault(0),
                        runCatching { cellScanDao.countSince(sessionStart) }.getOrDefault(0),
                    )
                }
            } else {
                Triple(0, 0, 0)
            }
            status.update {
                it.copy(
                    isRunning = true,
                    sessionStartMs = sessionStart,
                    wifiCount = restoredCounts.first,
                    btCount = restoredCounts.second,
                    cellCount = restoredCounts.third,
                )
            }
            if (!resuming) taskerBroadcaster.onScanStarted()
            settingsDataStore.markSessionActive(sessionStart)

            withContext(Dispatchers.IO) {
                val cutoff = System.currentTimeMillis() - RETENTION_DAYS * MS_PER_DAY
                runCatching { wifiScanDao.deleteOlderThan(cutoff) }
                runCatching { btScanDao.deleteOlderThan(cutoff) }
                runCatching { cellScanDao.deleteOlderThan(cutoff) }
            }

            // Refreshes the notification's elapsed-time stat on its own cadence, independent of
            // scan rounds — otherwise a session running only a slow scan type (e.g. Cell every
            // 300s) would show a stale duration between rounds, which reads as "did it stop?".
            jobs += launch {
                while (isActive) {
                    delay(10_000)
                    updateNotification()
                }
            }

            // Keep settings + location live so changes mid-session are picked up.
            jobs += launch {
                settingsDataStore.settings.collect { s ->
                    currentSettings = s
                    currentLat = s.latitude
                    currentLon = s.longitude
                    currentAlt = s.altitude
                }
            }

            // MQTT connect/disconnect reacts live to the enabled flag and broker URL.
            jobs += launch(Dispatchers.IO) {
                settingsDataStore.settings
                    .map { it.mqttEnabled to it.mqttBroker }
                    .distinctUntilChanged()
                    .collect { (enabled, broker) ->
                        if (enabled && broker.isNotBlank()) {
                            runCatching { mqttClient.connect(broker) }
                        } else {
                            runCatching { mqttClient.disconnect() }
                        }
                    }
            }

            jobs += superviseScanType(
                enabledSelector = { it.scanWifiEnabled },
                intervalSelector = { it.intervalWifiSec },
            ) {
                val rows = wifiScanner.scan().filter {
                    it.rssi >= currentSettings.wifiMinRssi && (it.band == null || it.band in currentSettings.wifiBands)
                }
                val stamped = rows.map {
                    it.copy(latitude = currentLat, longitude = currentLon, altitude = currentAlt)
                }
                val toStore = if (currentSettings.dedupEnabled) {
                    stamped.filter { dedupTracker.shouldLogWifi(it.bssid, it.rssi) }
                } else {
                    stamped
                }
                if (toStore.isNotEmpty()) wifiScanDao.insertAll(toStore)
                status.update { it.copy(wifiCount = it.wifiCount + stamped.size) }
                publishBatch(toStore)
                updateNotification()
                taskerBroadcaster.onNewWifi(stamped)
                val s = status.value
                taskerBroadcaster.onRoundComplete(s.wifiCount, s.btCount, s.cellCount)
            }

            jobs += superviseScanType(
                enabledSelector = { it.scanBtEnabled },
                intervalSelector = { it.intervalBtSec },
            ) {
                val rows = bluetoothScanner.scan()
                val stamped = rows.map {
                    it.copy(latitude = currentLat, longitude = currentLon, altitude = currentAlt)
                }
                val toStore = if (currentSettings.dedupEnabled) {
                    stamped.filter { dedupTracker.shouldLogBt(it.address, it.rssi) }
                } else {
                    stamped
                }
                if (toStore.isNotEmpty()) btScanDao.insertAll(toStore)
                status.update { it.copy(btCount = it.btCount + stamped.size) }
                publishBatch(toStore)
                updateNotification()
                taskerBroadcaster.onNewBt(stamped)
                val s = status.value
                taskerBroadcaster.onRoundComplete(s.wifiCount, s.btCount, s.cellCount)
            }

            jobs += superviseScanType(
                enabledSelector = { it.scanBleEnabled },
                intervalSelector = { it.intervalBleSec },
            ) {
                val rows = bleScanner.scan(currentSettings.captureBleAdvertisingData)
                val stamped = rows.map {
                    it.copy(latitude = currentLat, longitude = currentLon, altitude = currentAlt)
                }
                val toStore = if (currentSettings.dedupEnabled) {
                    stamped.filter { dedupTracker.shouldLogBt(it.address, it.rssi) }
                } else {
                    stamped
                }
                if (toStore.isNotEmpty()) btScanDao.insertAll(toStore)
                status.update { it.copy(btCount = it.btCount + stamped.size) }
                publishBatch(toStore)
                updateNotification()
                taskerBroadcaster.onNewBle(stamped)
                val s = status.value
                taskerBroadcaster.onRoundComplete(s.wifiCount, s.btCount, s.cellCount)
            }

            jobs += superviseScanType(
                enabledSelector = { it.scanCellEnabled },
                intervalSelector = { it.intervalCellSec },
            ) {
                val rows = cellScanner.scan()
                val stamped = rows.map {
                    it.copy(latitude = currentLat, longitude = currentLon, altitude = currentAlt)
                }
                // Cell records are never deduplicated — cell info changes are always significant.
                if (stamped.isNotEmpty()) cellScanDao.insertAll(stamped)
                status.update { it.copy(cellCount = it.cellCount + stamped.size) }
                publishBatch(stamped)
                updateNotification()
                taskerBroadcaster.onNewCell(stamped)
                val s = status.value
                taskerBroadcaster.onRoundComplete(s.wifiCount, s.btCount, s.cellCount)
            }

            // Auto-export reuses the same enabled/interval supervision pattern as the scan
            // types: autoExportIntervalMin > 0 enables it, and changing the interval mid-session
            // cancels/relaunches the loop like any other supervised type (minutes -> seconds for
            // the shared loop() helper).
            jobs += superviseScanType(
                enabledSelector = { it.autoExportIntervalMin > 0 },
                intervalSelector = { it.autoExportIntervalMin * 60 },
            ) {
                val settings = currentSettings
                val outputDir = exportManager.resolveOutputDir(settings.outputFolder)
                val file = exportManager.export(settings.exportFormat, outputDir)
                val count = exportManager.totalRecordCount()
                taskerBroadcaster.onExportComplete(file, settings.exportFormat.key, count)
            }
        }
    }

    /**
     * Watches `(enabled, intervalSec)` selected from live settings and cancels/relaunches the
     * inner loop whenever either changes, so a toggle or slider drag takes effect immediately.
     */
    private fun superviseScanType(
        enabledSelector: (NearScanSettings) -> Boolean,
        intervalSelector: (NearScanSettings) -> Int,
        block: suspend () -> Unit,
    ): Job = scope.launch {
        var innerJob: Job? = null
        settingsDataStore.settings
            .map { enabledSelector(it) to intervalSelector(it) }
            .distinctUntilChanged()
            .collect { (enabled, intervalSec) ->
                innerJob?.cancel()
                innerJob = if (enabled) launch { loop(intervalSec, block) } else null
            }
    }

    /** Publishes a stamped batch over MQTT when enabled, including any enabled extra fields. */
    private suspend fun publishBatch(entities: List<Any>) = withContext(Dispatchers.IO) {
        if (entities.isEmpty()) return@withContext
        val settings = currentSettings
        if (!settings.mqttEnabled) return@withContext
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
            runCatching { block() }.onFailure {
                // A round failing (e.g. SQLiteFullException on a full disk) must never silently
                // stop the whole session — but it must also not vanish without a trace, or a
                // persistently failing round looks identical to a healthy one from the outside.
                Log.w("ScanService", "scan round failed", it)
            }
            delay(intervalMs)
        }
    }

    private fun stopScanning() {
        val s = status.value
        val durationS = if (s.sessionStartMs > 0L) (System.currentTimeMillis() - s.sessionStartMs) / 1000L else 0L
        jobs.forEach { it.cancel() }
        jobs.clear()
        // Fire-and-forget on the service's own scope (not cancelled by the jobs.clear() above):
        // this is the only place a session is marked inactive, so a later START_STICKY restart
        // correctly treats itself as fresh rather than resuming a session the user already ended.
        scope.launch(Dispatchers.IO) { runCatching { settingsDataStore.markSessionInactive() } }
        runCatching { mqttClient.disconnect() }
        taskerBroadcaster.onScanStopped(s.wifiCount, s.btCount, s.cellCount, durationS)
        taskerBroadcaster.reset()
        dedupTracker.reset()
        status.update { ScanStatus() }
        stopForeground(STOP_FOREGROUND_REMOVE)
        releaseWakeLock()
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

    /** Refreshes the foreground notification with live per-type counts and elapsed session time. */
    private fun updateNotification() {
        val s = status.value
        if (!s.isRunning) return
        val elapsedS = if (s.sessionStartMs > 0L) (System.currentTimeMillis() - s.sessionStartMs) / 1000L else 0L
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = buildNotification(
            getString(
                R.string.notification_counts,
                s.wifiCount,
                s.btCount,
                s.cellCount,
                DateUtils.formatElapsedTime(elapsedS),
            ),
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
        releaseWakeLock()
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
        private const val NOTIFICATION_ID_BLOCKED = 1002
        private const val NOTIFICATION_ID_REBOOT = 1003
        private const val RETENTION_DAYS = 30L
        private const val MS_PER_DAY = 24L * 60 * 60 * 1000
        const val ACTION_STOP = "at.designer2k2.nearscan.action.STOP"

        private val status = MutableStateFlow(ScanStatus())
        val statusFlow: StateFlow<ScanStatus> = status.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, ScanService::class.java)
            try {
                context.startForegroundService(intent)
            } catch (e: IllegalStateException) {
                // Android 12+ blocks starting a foreground service from the background (e.g. a
                // Tasker CMD_START firing while NearScan isn't in the foreground) by throwing
                // ForegroundServiceStartNotAllowedException, a subclass of IllegalStateException
                // only present on API 31+. Catching the (always-available) parent type keeps this
                // safe to compile/run down to minSdk 26. Surface it instead of crashing the caller.
                Log.w("ScanService", "startForegroundService blocked by background restriction", e)
                notifyStartBlocked(context)
            }
        }

        /**
         * Called after the database is cleared (from the UI or the CMD_CLEAR_DATA Tasker
         * command) so a currently-running session's in-memory counters — which are incremented
         * independently of the DB — don't keep showing stale totals for records that no longer
         * exist.
         */
        fun resetCounts() {
            status.update { it.copy(wifiCount = 0, btCount = 0, cellCount = 0) }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ScanService::class.java).apply { action = ACTION_STOP }
            try {
                context.startService(intent)
            } catch (e: IllegalStateException) {
                // Same background-start restriction as start() — e.g. a Tasker CMD_STOP/CMD_TOGGLE
                // firing after the OS or an OEM battery manager already killed the service while
                // the app was backgrounded, so there's nothing to stop and no foreground app
                // context to satisfy the restriction. Not actionable; just don't crash the caller.
                Log.w("ScanService", "stop() blocked by background restriction", e)
            }
        }

        private fun notifyStartBlocked(context: Context) {
            postTapToOpenNotification(context, NOTIFICATION_ID_BLOCKED, context.getString(R.string.notification_start_blocked))
        }

        /**
         * Called from [at.designer2k2.nearscan.BootReceiver]: if a session was still active when
         * the device rebooted, the session silently ended with no Tasker SCAN_STOPPED event and
         * no user-visible signal. Android 15+ also forbids starting a `location`-type foreground
         * service directly from a BOOT_COMPLETED receiver, so this can only prompt the user to
         * reopen the app rather than resume scanning automatically.
         */
        fun notifySessionInterruptedByReboot(context: Context) {
            postTapToOpenNotification(
                context,
                NOTIFICATION_ID_REBOOT,
                context.getString(R.string.notification_interrupted_by_reboot),
            )
        }

        private fun postTapToOpenNotification(context: Context, notificationId: Int, text: String) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                )
                nm.createNotificationChannel(channel)
            }
            val openAppIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                openAppIntent,
                PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            nm.notify(notificationId, notification)
        }
    }
}
