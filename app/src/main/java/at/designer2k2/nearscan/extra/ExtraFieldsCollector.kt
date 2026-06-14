package at.designer2k2.nearscan.extra

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.PowerManager
import at.designer2k2.nearscan.prefs.NearScanSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class ExtraFields(
    val batteryLevel: Int? = null,
    val batteryCharging: Boolean? = null,
    val batteryTemperature: Float? = null,
    val screenOn: Boolean? = null,
    val mobileDataActive: Boolean? = null,
    val activeNetworkType: String? = null,
    val connectedSsid: String? = null,
    val heading: Float? = null,
    val tilt: Float? = null,
    val scanDurationMs: Long? = null,
    val memoryAvailableMb: Long? = null,
)

/**
 * Collects optional self-logged device/network/sensor fields. Each field is only populated
 * when its corresponding flag is enabled in [NearScanSettings]. All reads are synchronous;
 * [ExtraFields.scanDurationMs] is left null and filled in by the caller (ScanService).
 */
@Singleton
class ExtraFieldsCollector @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun collect(settings: NearScanSettings): ExtraFields {
        var batteryLevel: Int? = null
        var batteryCharging: Boolean? = null
        var batteryTemperature: Float? = null

        if (settings.extraBatteryLevel || settings.extraBatteryCharging || settings.extraBatteryTemp) {
            val battery = readBattery()
            if (settings.extraBatteryLevel) batteryLevel = battery?.level
            if (settings.extraBatteryCharging) batteryCharging = battery?.charging
            if (settings.extraBatteryTemp) batteryTemperature = battery?.temperatureC
        }

        val screenOn = if (settings.extraScreenOn) readScreenOn() else null

        var mobileDataActive: Boolean? = null
        var activeNetworkType: String? = null
        if (settings.extraMobileData || settings.extraNetworkType) {
            val net = readNetwork()
            if (settings.extraMobileData) mobileDataActive = net.mobileDataActive
            if (settings.extraNetworkType) activeNetworkType = net.type
        }

        val connectedSsid = if (settings.extraConnectedSsid) readSsid() else null

        var heading: Float? = null
        var tilt: Float? = null
        if (settings.extraHeading || settings.extraTilt) {
            val orientation = readOrientation()
            if (settings.extraHeading) heading = orientation?.heading
            if (settings.extraTilt) tilt = orientation?.tilt
        }

        val memoryAvailableMb = if (settings.extraMemory) readMemoryMb() else null

        return ExtraFields(
            batteryLevel = batteryLevel,
            batteryCharging = batteryCharging,
            batteryTemperature = batteryTemperature,
            screenOn = screenOn,
            mobileDataActive = mobileDataActive,
            activeNetworkType = activeNetworkType,
            connectedSsid = connectedSsid,
            heading = heading,
            tilt = tilt,
            scanDurationMs = null,
            memoryAvailableMb = memoryAvailableMb,
        )
    }

    private data class BatteryInfo(val level: Int?, val charging: Boolean?, val temperatureC: Float?)

    private fun readBattery(): BatteryInfo? {
        val intent = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull() ?: return null

        val rawLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val level = if (rawLevel >= 0 && scale > 0) (rawLevel * 100) / scale else null

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING

        val rawTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val temperature = if (rawTemp != Int.MIN_VALUE) rawTemp / 10f else null

        return BatteryInfo(level, charging, temperature)
    }

    private fun readScreenOn(): Boolean? {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return null
        return pm.isInteractive
    }

    private data class NetworkInfo(val mobileDataActive: Boolean?, val type: String?)

    private fun readNetwork(): NetworkInfo {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetworkInfo(null, null)
        val active = cm.activeNetwork
        val caps = active?.let { cm.getNetworkCapabilities(it) }
            ?: return NetworkInfo(false, "NONE")

        val mobile = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        val type = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
            else -> "NONE"
        }
        return NetworkInfo(mobile, type)
    }

    @Suppress("DEPRECATION", "MissingPermission")
    private fun readSsid(): String? {
        val wm = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
        val raw = runCatching { wm.connectionInfo?.ssid }.getOrNull() ?: return null
        val stripped = raw.removeSurrounding("\"")
        return if (stripped.isEmpty() || stripped == "<unknown ssid>") null else stripped
    }

    private data class Orientation(val heading: Float?, val tilt: Float?)

    private fun readOrientation(): Orientation? {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return null
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) ?: return null

        val latch = CountDownLatch(1)
        val rotationMatrix = FloatArray(9)
        val orientationValues = FloatArray(3)
        var captured = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event ?: return
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationValues)
                captured = true
                latch.countDown()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        runCatching { latch.await(500, TimeUnit.MILLISECONDS) }
        sm.unregisterListener(listener)

        if (!captured) return null

        var headingDeg = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
        if (headingDeg < 0f) headingDeg += 360f
        val tiltDeg = Math.toDegrees(orientationValues[1].toDouble()).toFloat()

        return Orientation(headingDeg, tiltDeg)
    }

    private fun readMemoryMb(): Long? {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return null
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        return mi.availMem / (1024 * 1024)
    }
}
