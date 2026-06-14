package at.designer2k2.nearscan.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import at.designer2k2.nearscan.db.BtScanDao
import at.designer2k2.nearscan.db.CellScanDao
import at.designer2k2.nearscan.db.WifiScanDao
import at.designer2k2.nearscan.prefs.NearScanSettings
import at.designer2k2.nearscan.prefs.SettingsDataStore
import at.designer2k2.nearscan.service.ScanService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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
)

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val settingsDataStore: SettingsDataStore,
    wifiScanDao: WifiScanDao,
    btScanDao: BtScanDao,
    cellScanDao: CellScanDao,
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

    fun updateSettings(settings: NearScanSettings) {
        viewModelScope.launch {
            settingsDataStore.update(settings)
        }
    }
}
