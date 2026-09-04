package at.designer2k2.nearscan.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    // resolveMccOrMnc — Issue #7 regression coverage. Below API 28 the string-based
    // CellIdentity getters don't exist (NoSuchMethodError risk on Android 8.0/8.1,
    // minSdk 26), so below P this must fall back to the legacy Int getter untouched.
    private val scanner = CellScanner(context, telephonyManager)

    @Test
    fun `resolveMccOrMnc uses modern string value on API 28+`() {
        assertEquals(
            310,
            scanner.resolveMccOrMnc(sdkInt = Build.VERSION_CODES.P, modernValue = "310", legacyValue = 999),
        )
    }

    @Test
    fun `resolveMccOrMnc returns null on API 28+ when modern value is unparseable`() {
        assertNull(
            scanner.resolveMccOrMnc(sdkInt = Build.VERSION_CODES.P, modernValue = null, legacyValue = 310),
        )
    }

    @Test
    fun `resolveMccOrMnc falls back to legacy int below API 28`() {
        assertEquals(
            310,
            scanner.resolveMccOrMnc(sdkInt = 26, modernValue = null, legacyValue = 310),
        )
    }

    @Test
    fun `resolveMccOrMnc maps legacy unavailable sentinel to null below API 28`() {
        assertNull(
            scanner.resolveMccOrMnc(sdkInt = 26, modernValue = null, legacyValue = Int.MAX_VALUE),
        )
    }
}
