package at.designer2k2.nearscan.util

import android.Manifest
import android.os.Build

/**
 * Runtime permissions needed for scan functionality (WiFi/BT/BLE/Cell), shared between the
 * initial request in `MainActivity` and the missing-permission check in `MainScreen`. Excludes
 * `POST_NOTIFICATIONS`: its absence only suppresses the foreground-service notification, it
 * doesn't silently break a scan type the way the others do.
 */
object RequiredPermissions {
    fun forScanning(): List<String> = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        add(Manifest.permission.READ_PHONE_STATE)
    }
}
