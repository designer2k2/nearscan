package at.designer2k2.nearscan.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NearScanSettingsTest {

    @Test
    fun `anyExtraFieldEnabled is false when all extra fields are off`() {
        assertFalse(NearScanSettings().anyExtraFieldEnabled)
    }

    @Test
    fun `anyExtraFieldEnabled is true when batteryLevel is on`() {
        assertTrue(NearScanSettings(extraBatteryLevel = true).anyExtraFieldEnabled)
    }

    @Test
    fun `anyExtraFieldEnabled is true when any single extra field is on`() {
        val singleFieldVariants = listOf<(NearScanSettings) -> NearScanSettings>(
            { it.copy(extraBatteryLevel = true) },
            { it.copy(extraBatteryCharging = true) },
            { it.copy(extraBatteryTemp = true) },
            { it.copy(extraScreenOn = true) },
            { it.copy(extraMobileData = true) },
            { it.copy(extraNetworkType = true) },
            { it.copy(extraConnectedSsid = true) },
            { it.copy(extraHeading = true) },
            { it.copy(extraTilt = true) },
            { it.copy(extraScanDuration = true) },
            { it.copy(extraMemory = true) },
        )
        for (variant in singleFieldVariants) {
            val settings = variant(NearScanSettings())
            assertTrue(
                "expected anyExtraFieldEnabled to be true for $settings",
                settings.anyExtraFieldEnabled,
            )
        }
    }

    @Test
    fun `anyExtraFieldEnabled is true when all extra fields are on`() {
        val settings = NearScanSettings(
            extraBatteryLevel = true,
            extraBatteryCharging = true,
            extraBatteryTemp = true,
            extraScreenOn = true,
            extraMobileData = true,
            extraNetworkType = true,
            extraConnectedSsid = true,
            extraHeading = true,
            extraTilt = true,
            extraScanDuration = true,
            extraMemory = true,
        )
        assertTrue(settings.anyExtraFieldEnabled)
    }

    @Test
    fun `default NearScanSettings has all extra fields disabled`() {
        val s = NearScanSettings()
        assertFalse(s.extraBatteryLevel)
        assertFalse(s.extraBatteryCharging)
        assertFalse(s.extraBatteryTemp)
        assertFalse(s.extraScreenOn)
        assertFalse(s.extraMobileData)
        assertFalse(s.extraNetworkType)
        assertFalse(s.extraConnectedSsid)
        assertFalse(s.extraHeading)
        assertFalse(s.extraTilt)
        assertFalse(s.extraScanDuration)
        assertFalse(s.extraMemory)
    }

    @Test
    fun `default intervals match spec values`() {
        val s = NearScanSettings()
        assertEquals(10, s.intervalWifiSec)
        assertEquals(30, s.intervalBtSec)
        assertEquals(15, s.intervalBleSec)
        assertEquals(60, s.intervalCellSec)
    }

    @Test
    fun `default export format is WIGLE_CSV`() {
        assertEquals(ExportFormat.WIGLE_CSV, NearScanSettings().exportFormat)
    }

    @Test
    fun `captureBleAdvertisingData defaults to true`() {
        assertTrue(NearScanSettings().captureBleAdvertisingData)
    }
}
