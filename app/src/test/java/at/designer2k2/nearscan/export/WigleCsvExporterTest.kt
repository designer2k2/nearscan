package at.designer2k2.nearscan.export

import at.designer2k2.nearscan.db.WifiScanDao
import at.designer2k2.nearscan.db.WifiScanEntity
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class WigleCsvExporterTest {

    private val wifiScanDao: WifiScanDao = mockk()
    private val exporter = WigleCsvExporter(wifiScanDao)
    private lateinit var outputDir: File

    @Before
    fun setUp() {
        outputDir = createTempDir(prefix = "wigle-test")
    }

    @After
    fun tearDown() {
        outputDir.deleteRecursively()
    }

    private fun makeWifiEntity(
        ssid: String = "TestNet",
        bssid: String = "AA:BB:CC:DD:EE:FF",
        rssi: Int = -65,
        channel: Int = 6,
        lat: Double? = 47.0,
        lon: Double? = 11.0,
        alt: Double? = 574.0,
    ): WifiScanEntity = WifiScanEntity(
        timestamp = 1_718_352_000_000L,
        latitude = lat,
        longitude = lon,
        altitude = alt,
        ssid = ssid,
        bssid = bssid,
        rssi = rssi,
        frequency = 2437,
        channel = channel,
        capabilities = "[WPA2-PSK-CCMP]",
        band = "2.4",
    )

    private fun export(entities: List<WifiScanEntity>): File {
        coEvery { wifiScanDao.getAll() } returns entities
        return runBlocking { exporter.export(outputDir) }
    }

    private fun lines(file: File): List<String> = file.readLines()

    @Test
    fun `export writes WiGLE pre-header on first line`() {
        val file = export(listOf(makeWifiEntity()))
        assertTrue(lines(file).first().startsWith("WigleWifi-1.4,"))
    }

    @Test
    fun `export writes column header on second line`() {
        val file = export(listOf(makeWifiEntity()))
        assertEquals(
            "MAC,SSID,AuthMode,FirstSeen,Channel,RSSI,CurrentLatitude,CurrentLongitude,AltitudeMeters,AccuracyMeters,Type",
            lines(file)[1],
        )
    }

    @Test
    fun `export writes one row per wifi entity`() {
        val file = export(listOf(makeWifiEntity(), makeWifiEntity(bssid = "11:22:33:44:55:66")))
        // 2 header lines + 2 data rows
        assertEquals(4, lines(file).size)
    }

    @Test
    fun `export row contains correct BSSID and SSID`() {
        val file = export(listOf(makeWifiEntity(ssid = "HomeWiFi", bssid = "DE:AD:BE:EF:00:01")))
        val row = lines(file)[2]
        assertTrue(row.startsWith("DE:AD:BE:EF:00:01,HomeWiFi,"))
    }

    @Test
    fun `export row type column is WIFI`() {
        val file = export(listOf(makeWifiEntity()))
        val cols = lines(file)[2].split(",")
        assertEquals("WIFI", cols.last())
    }

    @Test
    fun `export returns a File that exists`() {
        val file = export(listOf(makeWifiEntity()))
        assertTrue(file.exists())
    }

    @Test
    fun `export with empty list writes only headers`() {
        val file = export(emptyList())
        assertEquals(2, lines(file).size)
    }
}
