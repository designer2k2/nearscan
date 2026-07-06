package at.designer2k2.nearscan.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WifiScannerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val context: Context = mockk()
    private val wifiManager: WifiManager = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `scan returns empty list when ACCESS_FINE_LOCATION is not granted`() = runTest(testDispatcher) {
        every {
            context.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, any(), any())
        } returns PackageManager.PERMISSION_DENIED

        val scanner = WifiScanner(context, wifiManager)
        assertTrue(scanner.scan().isEmpty())
    }
}
