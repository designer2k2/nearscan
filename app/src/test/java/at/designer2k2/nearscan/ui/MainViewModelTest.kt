package at.designer2k2.nearscan.ui

import android.app.Application
import android.location.Location
import at.designer2k2.nearscan.db.BtScanDao
import at.designer2k2.nearscan.db.CellScanDao
import at.designer2k2.nearscan.db.WifiScanDao
import at.designer2k2.nearscan.export.ExportManager
import at.designer2k2.nearscan.location.GpsFix
import at.designer2k2.nearscan.location.LocationHelper
import androidx.lifecycle.viewModelScope
import at.designer2k2.nearscan.prefs.NearScanSettings
import at.designer2k2.nearscan.prefs.SettingsDataStore
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val settingsDataStore: SettingsDataStore = mockk()
    private val wifiScanDao: WifiScanDao = mockk()
    private val btScanDao: BtScanDao = mockk()
    private val cellScanDao: CellScanDao = mockk()
    private val locationHelper: LocationHelper = mockk()
    private val exportManager: ExportManager = mockk()
    private val application: Application = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { settingsDataStore.settings } returns flowOf(NearScanSettings())
        every { wifiScanDao.count() } returns flowOf(0L)
        every { btScanDao.count() } returns flowOf(0L)
        every { cellScanDao.count() } returns flowOf(0L)
        every { application.applicationContext } returns application
        // Relaxed mockk auto-mocks File (bypassing its constructor, leaving internal fields
        // null) unless stubbed explicitly, which crashes the real File(File, String) resolve
        // path. Return a properly-constructed File so exportNow()'s fallback dir works.
        every { application.getExternalFilesDir(null) } returns java.io.File("/tmp/nearscan-test-files")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun mockLocation(lat: Double, lon: Double, alt: Double, accuracy: Float? = null): Location {
        val loc: Location = mockk()
        every { loc.latitude } returns lat
        every { loc.longitude } returns lon
        every { loc.altitude } returns alt
        every { loc.hasAccuracy() } returns (accuracy != null)
        every { loc.accuracy } returns (accuracy ?: 0f)
        return loc
    }

    private fun createViewModel(): MainViewModel = MainViewModel(
        application = application,
        settingsDataStore = settingsDataStore,
        wifiScanDao = wifiScanDao,
        btScanDao = btScanDao,
        cellScanDao = cellScanDao,
        locationHelper = locationHelper,
        exportManager = exportManager,
    )

    /**
     * MainViewModel's init block launches a `while (true) { delay(1_000) }` session-timer
     * ticker in viewModelScope. Since Dispatchers.Main is set to [testDispatcher], that ticker
     * shares the test's TestCoroutineScheduler and reschedules itself forever, so runTest's
     * implicit advanceUntilIdle() at the end of the test body would spin indefinitely unless
     * the ViewModel's scope is cancelled before the test body returns.
     */
    private fun runVmTest(testBody: suspend TestScope.(MainViewModel) -> Unit) = runTest(testDispatcher) {
        val vm = createViewModel()
        try {
            testBody(vm)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `initial state isRunning is false`() = runVmTest { vm ->
        assertFalse(vm.uiState.value.isRunning)
    }

    @Test
    fun `initial totalRecords is 0`() = runVmTest { vm ->
        assertEquals(0L, vm.uiState.value.totalRecords)
    }

    @Test
    fun `settings from datastore are reflected in uiState`() = runTest(testDispatcher) {
        every { settingsDataStore.settings } returns flowOf(
            NearScanSettings(latitude = 47.5, longitude = 11.5, altitude = 600.0, mqttTopic = "custom/topic"),
        )
        val vm = createViewModel()
        try {
            testScheduler.runCurrent()
            val state = vm.uiState.value
            assertEquals(47.5, state.latitude!!, 0.0)
            assertEquals(11.5, state.longitude!!, 0.0)
            assertEquals(600.0, state.altitude!!, 0.0)
            assertEquals("custom/topic", state.settings.mqttTopic)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `updateLocation persists to settingsDataStore`() = runVmTest { vm ->
        coEvery { settingsDataStore.updateLocation(any(), any(), any()) } just Runs
        vm.updateLocation(47.0, 11.0, 574.0)
        testScheduler.runCurrent()
        coVerify { settingsDataStore.updateLocation(47.0, 11.0, 574.0) }
    }

    @Test
    fun `getGpsFix sets isGettingFix true then false`() = runVmTest { vm ->
        coEvery { locationHelper.getSingleFix(any()) } returns mockLocation(47.269, 11.404, 574.0)
        vm.getGpsFix()
        testScheduler.runCurrent()
        // After the suspend fix resolves, isGettingFix is back to false
        assertFalse(vm.uiState.value.isGettingFix)
    }

    @Test
    fun `getGpsFix with successful fix sets gpsFixResult`() = runVmTest { vm ->
        coEvery { locationHelper.getSingleFix(any()) } returns mockLocation(47.269, 11.404, 574.0)
        vm.getGpsFix()
        testScheduler.runCurrent()
        val result = vm.uiState.value.gpsFixResult
        assertEquals(GpsFix(47.269, 11.404, 574.0, null), result)
    }

    @Test
    fun `getGpsFix with null result leaves gpsFixResult null`() = runVmTest { vm ->
        coEvery { locationHelper.getSingleFix(any()) } returns null
        vm.getGpsFix()
        testScheduler.runCurrent()
        assertNull(vm.uiState.value.gpsFixResult)
        assertFalse(vm.uiState.value.isGettingFix)
    }

    @Test
    fun `consumeGpsFix clears gpsFixResult`() = runVmTest { vm ->
        coEvery { locationHelper.getSingleFix(any()) } returns mockLocation(47.269, 11.404, 574.0)
        vm.getGpsFix()
        testScheduler.runCurrent()
        assertEquals(GpsFix(47.269, 11.404, 574.0, null), vm.uiState.value.gpsFixResult)
        vm.consumeGpsFix()
        assertNull(vm.uiState.value.gpsFixResult)
    }

    @Test
    fun `exportNow with successful export sets exportResult success`() = runVmTest { vm ->
        val exportedFile = java.io.File("nearscan_wigle_123.csv")
        coEvery { exportManager.export(any(), any()) } returns exportedFile
        vm.exportNow()
        testScheduler.runCurrent()
        val result = vm.uiState.value.exportResult
        assertEquals(true, result?.success)
        assertEquals("nearscan_wigle_123.csv", result?.message)
        assertFalse(vm.uiState.value.isExporting)
    }

    @Test
    fun `exportNow with failing export sets exportResult failure`() = runVmTest { vm ->
        coEvery { exportManager.export(any(), any()) } throws IllegalStateException("disk full")
        vm.exportNow()
        testScheduler.runCurrent()
        val result = vm.uiState.value.exportResult
        assertEquals(false, result?.success)
        assertEquals("disk full", result?.message)
    }

    @Test
    fun `consumeExportResult clears exportResult`() = runVmTest { vm ->
        coEvery { exportManager.export(any(), any()) } returns java.io.File("f.csv")
        vm.exportNow()
        testScheduler.runCurrent()
        vm.consumeExportResult()
        assertNull(vm.uiState.value.exportResult)
    }
}
