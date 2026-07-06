package at.designer2k2.nearscan.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

class LocationHelperTest {

    private val context: Context = mockk()
    private val locationManager: LocationManager = mockk()
    private lateinit var helper: LocationHelper

    @Before
    fun setUp() {
        helper = LocationHelper()
        every { context.getSystemService(Context.LOCATION_SERVICE) } returns locationManager
    }

    private fun grantLocationPermission(granted: Boolean) {
        every {
            context.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, any(), any())
        } returns if (granted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
    }

    @Test
    fun `getSingleFix returns null when location permission is not granted`() = runTest {
        grantLocationPermission(granted = false)
        val result = helper.getSingleFix(context)
        assertNull(result)
    }

    @Test
    fun `getSingleFix returns null when LocationManager is unavailable`() = runTest {
        grantLocationPermission(granted = true)
        every { context.getSystemService(Context.LOCATION_SERVICE) } returns null
        val result = helper.getSingleFix(context)
        assertNull(result)
    }

    @Test
    fun `getSingleFix returns null when no provider is enabled`() = runTest {
        grantLocationPermission(granted = true)
        every { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } returns false
        every { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } returns false
        val result = helper.getSingleFix(context)
        assertNull(result)
    }

    @Test
    fun `getSingleFix returns last known location immediately when available`() = runTest {
        grantLocationPermission(granted = true)
        every { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } returns true
        val lastKnown: Location = mockk()
        every { locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } returns lastKnown
        val result = helper.getSingleFix(context)
        assertSame(lastKnown, result)
    }

    @Test
    fun `getSingleFix prefers GPS provider over network provider`() = runTest {
        grantLocationPermission(granted = true)
        every { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } returns true
        every { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } returns true
        val lastKnown: Location = mockk()
        every { locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } returns lastKnown
        helper.getSingleFix(context)
        io.mockk.verify { locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) }
    }

    @Test
    fun `getSingleFix falls back to network provider when GPS is disabled`() = runTest {
        grantLocationPermission(granted = true)
        every { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } returns false
        every { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } returns true
        val lastKnown: Location = mockk()
        every { locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } returns lastKnown
        val result = helper.getSingleFix(context)
        assertSame(lastKnown, result)
    }
}
