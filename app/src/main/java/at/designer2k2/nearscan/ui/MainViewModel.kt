package at.designer2k2.nearscan.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import at.designer2k2.nearscan.db.BtScanDao
import at.designer2k2.nearscan.db.CellScanDao
import at.designer2k2.nearscan.db.WifiScanDao
import at.designer2k2.nearscan.export.ExportManager
import at.designer2k2.nearscan.location.GpsFix
import at.designer2k2.nearscan.location.LocationHelper
import at.designer2k2.nearscan.prefs.NearScanSettings
import at.designer2k2.nearscan.prefs.SettingsDataStore
import at.designer2k2.nearscan.service.ScanService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Result of a manual export, shown once (Snackbar on failure, share sheet on success) then cleared. */
data class ExportResult(
    val success: Boolean,
    val message: String,
    val filePath: String? = null,
    val mimeType: String? = null,
)

data class MainUiState(
    val isRunning: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val wifiCount: Int = 0,
    val btCount: Int = 0,
    val cellCount: Int = 0,
    val totalRecords: Long = 0,
    val sessionSeconds: Long = 0,
    val settings: NearScanSettings = NearScanSettings(),
    val isGettingFix: Boolean = false,
    val gpsFixResult: GpsFix? = null,  // consumed once then cleared
    val isExporting: Boolean = false,
    val exportResult: ExportResult? = null,  // consumed once then cleared
    // True when DataStore still marks a session active but ScanService isn't running — an OS
    // reboot, an OEM battery manager, or a low-memory kill ended it without a clean stop.
    val interruptedSessionDetected: Boolean = false,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val settingsDataStore: SettingsDataStore,
    private val wifiScanDao: WifiScanDao,
    private val btScanDao: BtScanDao,
    private val cellScanDao: CellScanDao,
    private val locationHelper: LocationHelper,
    private val exportManager: ExportManager,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        // Settings -> UI state (location + settings snapshot)
        viewModelScope.launch {
            settingsDataStore.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        settings = settings,
                        latitude = settings.latitude,
                        longitude = settings.longitude,
                        altitude = settings.altitude,
                    )
                }
            }
        }

        // Service status -> UI state (counters, running, session timer)
        viewModelScope.launch {
            ScanService.statusFlow.collect { status ->
                val sessionSeconds = if (status.isRunning && status.sessionStartMs > 0) {
                    (System.currentTimeMillis() - status.sessionStartMs) / 1000
                } else {
                    0L
                }
                _uiState.update {
                    it.copy(
                        isRunning = status.isRunning,
                        wifiCount = status.wifiCount,
                        btCount = status.btCount,
                        cellCount = status.cellCount,
                        sessionSeconds = sessionSeconds,
                    )
                }
            }
        }

        // Per-second ticker so the session timer advances even without scan events
        viewModelScope.launch {
            while (true) {
                delay(1_000L)
                val status = ScanService.statusFlow.value
                if (status.isRunning && status.sessionStartMs > 0L) {
                    _uiState.update {
                        it.copy(
                            sessionSeconds = (System.currentTimeMillis() - status.sessionStartMs) / 1000,
                        )
                    }
                }
            }
        }

        // Detects a session that ended without a clean stop (reboot / OEM kill / low-memory kill):
        // DataStore still says active, but the service's in-memory status says otherwise.
        viewModelScope.launch {
            combine(settingsDataStore.sessionActive, ScanService.statusFlow) { (wasActive, _), status ->
                wasActive && !status.isRunning
            }.collect { interrupted ->
                _uiState.update { it.copy(interruptedSessionDetected = interrupted) }
            }
        }

        // Total record count across all tables
        viewModelScope.launch {
            combine(
                wifiScanDao.count(),
                btScanDao.count(),
                cellScanDao.count(),
            ) { w, b, c -> w + b + c }
                .collect { total ->
                    _uiState.update { it.copy(totalRecords = total) }
                }
        }
    }

    fun startScan() {
        ScanService.start(getApplication())
    }

    fun stopScan() {
        ScanService.stop(getApplication())
    }

    fun updateLocation(lat: Double, lon: Double, alt: Double) {
        viewModelScope.launch {
            settingsDataStore.updateLocation(lat, lon, alt)
        }
    }

    fun getGpsFix() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGettingFix = true) }
            val loc = locationHelper.getSingleFix(getApplication())
            if (loc != null) {
                _uiState.update {
                    it.copy(
                        isGettingFix = false,
                        gpsFixResult = GpsFix(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            altitude = loc.altitude,
                            accuracyMeters = if (loc.hasAccuracy()) loc.accuracy else null,
                        ),
                    )
                }
            } else {
                _uiState.update { it.copy(isGettingFix = false) }
            }
        }
    }

    fun consumeGpsFix() {
        _uiState.update { it.copy(gpsFixResult = null) }
    }

    fun updateSettings(settings: NearScanSettings) {
        viewModelScope.launch {
            settingsDataStore.update(settings)
        }
    }

    /** Exports the current database using the currently-configured format and output folder. */
    fun exportNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            val settings = uiState.value.settings
            val outputDir = exportManager.resolveOutputDir(settings.outputFolder)
            val result = runCatching { exportManager.export(settings.exportFormat, outputDir) }
            _uiState.update {
                it.copy(
                    isExporting = false,
                    exportResult = result.fold(
                        onSuccess = { file ->
                            ExportResult(
                                success = true,
                                message = file.name,
                                filePath = file.absolutePath,
                                mimeType = settings.exportFormat.mimeType,
                            )
                        },
                        onFailure = { e -> ExportResult(success = false, message = e.message ?: "") },
                    ),
                )
            }
        }
    }

    fun consumeExportResult() {
        _uiState.update { it.copy(exportResult = null) }
    }

    /** Restarts the service; it reads the still-active persisted session and resumes it. */
    fun resumeInterruptedSession() {
        ScanService.start(getApplication())
    }

    /** Ends the stale session bookkeeping without restarting scanning. */
    fun dismissInterruptedSession() {
        viewModelScope.launch {
            settingsDataStore.markSessionInactive()
        }
    }

    /** Permanently deletes all logged WiFi/BT/Cell records. Does not affect settings. */
    fun clearAllData() {
        viewModelScope.launch {
            wifiScanDao.clearAll()
            btScanDao.clearAll()
            cellScanDao.clearAll()
            ScanService.resetCounts()
        }
    }
}
