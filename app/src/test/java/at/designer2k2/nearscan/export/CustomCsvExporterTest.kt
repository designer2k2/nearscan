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
import kotlin.io.path.createTempDirectory

class CustomCsvExporterTest {

    private val wifiScanDao: WifiScanDao = mockk()
    private val btScanDao: BtScanDao = mockk()
    private val cellScanDao: CellScanDao = mockk()
    private val exporter = CustomCsvExporter(wifiScanDao, btScanDao, cellScanDao)
    private lateinit var outputDir: File

    @Before
    fun setUp() {
        outputDir = createTempDirectory("custom-test").toFile()
        coEvery { wifiScanDao.getPage(any(), any()) } returns emptyList()
        coEvery { btScanDao.getPage(any(), any()) } returns emptyList()
        coEvery { cellScanDao.getPage(any(), any()) } returns emptyList()
    }

    @After
    fun tearDown() {
        outputDir.deleteRecursively()
    }

    private fun makeWifiEntity(
        ssid: String? = "TestNet",
        bssid: String = "AA:BB:CC:DD:EE:FF",
    ): WifiScanEntity = WifiScanEntity(
        timestamp = 1_718_352_000_000L,
        latitude = 47.0,
        longitude = 11.0,
        altitude = 574.0,
        ssid = ssid,
        bssid = bssid,
        rssi = -65,
        frequency = 2437,
        channel = 6,
        capabilities = "[WPA2]",
        band = "2.4",
    )

    private fun makeBtEntity(): BtScanEntity = BtScanEntity(
        timestamp = 1_718_352_000_000L,
        latitude = 47.0,
        longitude = 11.0,
        altitude = 574.0,
        address = "AA:BB:CC:11:22:33",
        name = "Headphones",
        rssi = -70,
        deviceClass = 1024,
        isBle = false,
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

    private fun runExport(): List<String> {
        val file = runBlocking { exporter.export(outputDir) }
        return file.readLines()
    }

    private fun header(): String = runExport().first()

    private fun rowFor(type: String): String =
        runExport().drop(1).first { it.split(",").first() == type }

    @Test
    fun `export writes a header row as first line`() {
        assertTrue(header().startsWith("type,"))
    }

    @Test
    fun `header contains type column`() {
        assertTrue(header().split(",").contains("type"))
    }

    @Test
    fun `header contains wifi-specific columns`() {
        val cols = header().split(",")
        assertTrue(cols.contains("ssid"))
        assertTrue(cols.contains("bssid"))
        assertTrue(cols.contains("channel"))
    }

    @Test
    fun `header contains bt-specific columns`() {
        val cols = header().split(",")
        assertTrue(cols.contains("bt_address"))
        assertTrue(cols.contains("is_ble"))
    }

    @Test
    fun `header contains cell-specific columns`() {
        val cols = header().split(",")
        assertTrue(cols.contains("mcc"))
        assertTrue(cols.contains("technology"))
    }

    @Test
    fun `wifi row has type WIFI`() {
        coEvery { wifiScanDao.getPage(any(), any()) } returns listOf(makeWifiEntity())
        assertEquals("WIFI", rowFor("WIFI").split(",").first())
    }

    @Test
    fun `bt row has type BT`() {
        coEvery { btScanDao.getPage(any(), any()) } returns listOf(makeBtEntity())
        assertEquals("BT", rowFor("BT").split(",").first())
    }

    @Test
    fun `cell row has type CELL`() {
        coEvery { cellScanDao.getPage(any(), any()) } returns listOf(makeCellEntity())
        assertEquals("CELL", rowFor("CELL").split(",").first())
    }

    @Test
    fun `ssid with comma is quoted`() {
        coEvery { wifiScanDao.getPage(any(), any()) } returns listOf(makeWifiEntity(ssid = "My,Network"))
        assertTrue(rowFor("WIFI").contains("\"My,Network\""))
    }

    @Test
    fun `ssid with double quote is escaped`() {
        coEvery { wifiScanDao.getPage(any(), any()) } returns listOf(makeWifiEntity(ssid = "My\"Net"))
        // A literal inner quote is doubled and the field wrapped in quotes -> "My""Net"
        assertTrue(rowFor("WIFI").contains("\"My\"\"Net\""))
    }
}
