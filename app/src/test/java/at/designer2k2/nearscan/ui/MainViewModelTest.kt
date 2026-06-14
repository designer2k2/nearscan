package at.designer2k2.nearscan.ui

import android.app.Application
import android.location.Location
import at.designer2k2.nearscan.db.BtScanDao
import at.designer2k2.nearscan.db.CellScanDao
import at.designer2k2.nearscan.db.WifiScanDao
import at.designer2k2.nearscan.location.LocationHelper
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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
    private val application: Application = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { settingsDataStore.settings } returns flowOf(NearScanSettings())
        every { wifiScanDao.count() } returns flowOf(0L)
        every { btScanDao.count() } returns flowOf(0L)
        every { cellScanDao.count() } returns flowOf(0L)
        every { application.applicationContext } returns application
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun mockLocation(lat: Double, lon: Double, alt: Double): Location {
        val loc: Location = mockk()
        every { loc.latitude } returns lat
        every { loc.longitude } returns lon
        every { loc.altitude } returns alt
        return loc
    }

    private fun createViewModel(): MainViewModel = MainViewModel(
        application = application,
        settingsDataStore = settingsDataStore,
        wifiScanDao = wifiScanDao,
        btScanDao = btScanDao,
        cellScanDao = cellScanDao,
        locationHelper = locationHelper,
    )

    @Test
    fun `initial state isRunning is false`() = runTest(testDispatcher) {
        val vm = createViewModel()
        assertFalse(vm.uiState.value.isRunning)
    }

    @Test
    fun `initial totalRecords is 0`() = runTest(testDispatcher) {
        val vm = createViewModel()
        assertEquals(0L, vm.uiState.value.totalRecords)
    }

    @Test
    fun `settings from datastore are reflected in uiState`() = runTest(testDispatcher) {
        every { settingsDataStore.settings } returns flowOf(
            NearScanSettings(latitude = 47.5, longitude = 11.5, altitude = 600.0, mqttTopic = "custom/topic"),
        )
        val vm = createViewModel()
        testScheduler.runCurrent()
        val state = vm.uiState.value
        assertEquals(47.5, state.latitude!!, 0.0)
        assertEquals(11.5, state.longitude!!, 0.0)
        assertEquals(600.0, state.altitude!!, 0.0)
        assertEquals("custom/topic", state.settings.mqttTopic)
    }

    @Test
    fun `updateLocation persists to settingsDataStore`() = runTest(testDispatcher) {
        coEvery { settingsDataStore.updateLocation(any(), any(), any()) } just Runs
        val vm = createViewModel()
        vm.updateLocation(47.0, 11.0, 574.0)
        testScheduler.runCurrent()
        coVerify { settingsDataStore.updateLocation(47.0, 11.0, 574.0) }
    }

    @Test
    fun `getGpsFix sets isGettingFix true then false`() = runTest(testDispatcher) {
        coEvery { locationHelper.getSingleFix(any()) } returns mockLocation(47.269, 11.404, 574.0)
        val vm = createViewModel()
        vm.getGpsFix()
        testScheduler.runCurrent()
        // After the suspend fix resolves, isGettingFix is back to false
        assertFalse(vm.uiState.value.isGettingFix)
    }

    @Test
    fun `getGpsFix with successful fix sets gpsFixResult`() = runTest(testDispatcher) {
        coEvery { locationHelper.getSingleFix(any()) } returns mockLocation(47.269, 11.404, 574.0)
        val vm = createViewModel()
        vm.getGpsFix()
        testScheduler.runCurrent()
        val result = vm.uiState.value.gpsFixResult
        assertEquals(Triple(47.269, 11.404, 574.0), result)
    }

    @Test
    fun `getGpsFix with null result leaves gpsFixResult null`() = runTest(testDispatcher) {
        coEvery { locationHelper.getSingleFix(any()) } returns null
        val vm = createViewModel()
        vm.getGpsFix()
        testScheduler.runCurrent()
        assertNull(vm.uiState.value.gpsFixResult)
        assertFalse(vm.uiState.value.isGettingFix)
    }

    @Test
    fun `consumeGpsFix clears gpsFixResult`() = runTest(testDispatcher) {
        coEvery { locationHelper.getSingleFix(any()) } returns mockLocation(47.269, 11.404, 574.0)
        val vm = createViewModel()
        vm.getGpsFix()
        testScheduler.runCurrent()
        assertEquals(Triple(47.269, 11.404, 574.0), vm.uiState.value.gpsFixResult)
        vm.consumeGpsFix()
        assertNull(vm.uiState.value.gpsFixResult)
    }
}
