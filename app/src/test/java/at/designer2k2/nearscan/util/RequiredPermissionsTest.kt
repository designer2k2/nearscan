package at.designer2k2.nearscan.util

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Test

class RequiredPermissionsTest {

    @Test
    fun `bluetooth scan needs fine location before Android 12`() {
        assertEquals(Manifest.permission.ACCESS_FINE_LOCATION, RequiredPermissions.forBluetoothScan(sdkInt = 26))
        assertEquals(Manifest.permission.ACCESS_FINE_LOCATION, RequiredPermissions.forBluetoothScan(sdkInt = 30))
    }

    @Test
    fun `bluetooth scan needs BLUETOOTH_SCAN from Android 12`() {
        assertEquals(Manifest.permission.BLUETOOTH_SCAN, RequiredPermissions.forBluetoothScan(sdkInt = 31))
        assertEquals(Manifest.permission.BLUETOOTH_SCAN, RequiredPermissions.forBluetoothScan(sdkInt = 36))
    }
}
