package at.designer2k2.nearscan.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Runtime permissions needed for scan functionality (WiFi/BT/BLE/Cell), shared between the
 * initial request in `MainActivity` and the missing-permission check in `MainScreen`. Excludes
 * `POST_NOTIFICATIONS`: its absence only suppresses the foreground-service notification, it
 * doesn't silently break a scan type the way the others do.
 */
object RequiredPermissions {
    fun forScanning(): List<String> = buildList {
        // Requested together: Android 12+ only shows the precise-location option in the runtime
        // dialog when COARSE and FINE are requested in the same ActivityCompat.requestPermissions
        // call, which MainActivity does since this whole list is passed in one request.
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        add(Manifest.permission.READ_PHONE_STATE)
    }

    /**
     * The runtime permission a Bluetooth Classic / BLE scan needs on this OS version:
     * `BLUETOOTH_SCAN` on Android 12+, `ACCESS_FINE_LOCATION` before that (`BLUETOOTH_SCAN`
     * doesn't exist pre-31, so checking it there always reports DENIED).
     */
    /**
     * A `location`-type foreground service may only be started while FINE or COARSE location is
     * granted — Android 14+ throws SecurityException from startForeground() otherwise, and since
     * the service was launched via startForegroundService() the process is then killed for never
     * reaching the foreground. Check this before ScanService is ever started.
     */
    fun hasLocationForForegroundService(context: Context): Boolean =
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    // BLUETOOTH_SCAN is a compile-time String constant, only returned when sdkInt >= 31; lint
    // can't see that guard through the sdkInt parameter (kept as a parameter for testability).
    @SuppressLint("InlinedApi")
    fun forBluetoothScan(sdkInt: Int = Build.VERSION.SDK_INT): String =
        if (sdkInt >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_SCAN
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
}
