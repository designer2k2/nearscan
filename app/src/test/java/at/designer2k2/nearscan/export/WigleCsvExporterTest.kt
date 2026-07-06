package at.designer2k2.nearscan.export

import at.designer2k2.nearscan.db.BtScanDao
import at.designer2k2.nearscan.db.BtScanEntity
import at.designer2k2.nearscan.db.CellScanDao
import at.designer2k2.nearscan.db.CellScanEntity
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
    private val btScanDao: BtScanDao = mockk()
    private val cellScanDao: CellScanDao = mockk()
    private val exporter = WigleCsvExporter(wifiScanDao, btScanDao, cellScanDao)
    private lateinit var outputDir: File

    @Before
    fun setUp() {
        outputDir = createTempDir(prefix = "wigle-test")
        coEvery { wifiScanDao.getPage(any(), any()) } returns emptyList()
        coEvery { btScanDao.getPage(any(), any()) } returns emptyList()
        coEvery { cellScanDao.getPage(any(), any()) } returns emptyList()
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

    private fun makeBtEntity(isBle: Boolean = false): BtScanEntity = BtScanEntity(
        timestamp = 1_718_352_000_000L,
        latitude = 47.0,
        longitude = 11.0,
        altitude = 574.0,
        address = "AA:BB:CC:11:22:33",
        name = "Speaker",
        rssi = -70,
        deviceClass = 1024,
        isBle = isBle,
    )

    private fun makeCellEntity(): CellScanEntity = CellScanEntity(
        timestamp = 1_718_352_000_000L,
        latitude = 47.0,
        longitude = 11.0,
        altitude = 574.0,
        mcc = 232,
        mnc = 1,
        lac = 100,
        cid = 12345L,
        rssi = -95,
        technology = "LTE",
    )

    private fun export(entities: List<WifiScanEntity>): File {
        coEvery { wifiScanDao.getPage(any(), any()) } returns entities
        return runBlocking { exporter.export(outputDir) }
    }

    private fun lines(file: File): List<String> = file.readLines()

    @Test
    fun `export writes WiGLE pre-header on first line`() {
        val file = export(listOf(makeWifiEntity()))
        assertTrue(lines(file).first().startsWith("WigleWifi-1.6,"))
    }

    @Test
    fun `export writes column header on second line`() {
        val file = export(listOf(makeWifiEntity()))
        assertEquals(
            "MAC,SSID,AuthMode,FirstSeen,Channel,Frequency,RSSI,CurrentLatitude,CurrentLongitude," +
                "AltitudeMeters,AccuracyMeters,RCOIs,MfgrId,Type",
            lines(file)[1],
        )
    }

    @Test
    fun `export row FirstSeen is a formatted UTC date, not raw epoch millis`() {
        val file = export(listOf(makeWifiEntity()))
        val cols = lines(file)[2].split(",")
        // timestamp = 1_718_352_000_000L == 2024-06-14 08:00:00 UTC
        assertEquals("2024-06-14 08:00:00", cols[3])
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

    @Test
    fun `BT row is included with type BT`() {
        coEvery { btScanDao.getPage(any(), any()) } returns listOf(makeBtEntity(isBle = false))
        val file = runBlocking { exporter.export(outputDir) }
        val row = lines(file).drop(2).first { it.split(",").last() == "BT" }
        assertTrue(row.startsWith("AA:BB:CC:11:22:33,Speaker,"))
    }

    @Test
    fun `BLE row is included with type BLE, not BT`() {
        coEvery { btScanDao.getPage(any(), any()) } returns listOf(makeBtEntity(isBle = true))
        val file = runBlocking { exporter.export(outputDir) }
        val row = lines(file).drop(2).first { it.split(",").last() == "BLE" }
        assertTrue(row.startsWith("AA:BB:CC:11:22:33,Speaker,"))
    }

    @Test
    fun `Cell row is included with technology as Type`() {
        coEvery { cellScanDao.getPage(any(), any()) } returns listOf(makeCellEntity())
        val file = runBlocking { exporter.export(outputDir) }
        val row = lines(file).drop(2).first { it.split(",").last() == "LTE" }
        assertTrue(row.contains("232-1-12345"))
    }

    @Test
    fun `export includes wifi, bt, and cell rows together`() {
        coEvery { wifiScanDao.getPage(any(), any()) } returns listOf(makeWifiEntity())
        coEvery { btScanDao.getPage(any(), any()) } returns listOf(makeBtEntity())
        coEvery { cellScanDao.getPage(any(), any()) } returns listOf(makeCellEntity())
        val file = runBlocking { exporter.export(outputDir) }
        // 2 header lines + 3 data rows
        assertEquals(5, lines(file).size)
    }
}
