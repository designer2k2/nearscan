package at.designer2k2.nearscan.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
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
class CellScannerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val context: Context = mockk()
    private val telephonyManager: TelephonyManager = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun grantPermissions(phoneState: Boolean, fineLocation: Boolean) {
        every {
            context.checkPermission(Manifest.permission.READ_PHONE_STATE, any(), any())
        } returns if (phoneState) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
        every {
            context.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, any(), any())
        } returns if (fineLocation) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
    }

    @Test
    fun `scan returns empty list when telephonyManager is null`() = runTest(testDispatcher) {
        grantPermissions(phoneState = true, fineLocation = true)
        val scanner = CellScanner(context, null)
        assertTrue(scanner.scan().isEmpty())
    }

    @Test
    fun `scan returns empty list when READ_PHONE_STATE is not granted`() = runTest(testDispatcher) {
        grantPermissions(phoneState = false, fineLocation = true)
        val scanner = CellScanner(context, telephonyManager)
        assertTrue(scanner.scan().isEmpty())
    }

    @Test
    fun `scan returns empty list when ACCESS_FINE_LOCATION is not granted`() = runTest(testDispatcher) {
        grantPermissions(phoneState = true, fineLocation = false)
        val scanner = CellScanner(context, telephonyManager)
        assertTrue(scanner.scan().isEmpty())
    }

    @Test
    fun `scan returns empty list when allCellInfo is null`() = runTest(testDispatcher) {
        grantPermissions(phoneState = true, fineLocation = true)
        every { telephonyManager.allCellInfo } returns null
        val scanner = CellScanner(context, telephonyManager)
        assertTrue(scanner.scan().isEmpty())
    }

    @Test
    fun `scan returns empty list when allCellInfo is empty`() = runTest(testDispatcher) {
        grantPermissions(phoneState = true, fineLocation = true)
        every { telephonyManager.allCellInfo } returns emptyList()
        val scanner = CellScanner(context, telephonyManager)
        assertTrue(scanner.scan().isEmpty())
    }
}
