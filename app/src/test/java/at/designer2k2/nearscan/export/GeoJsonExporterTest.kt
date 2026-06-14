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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class GeoJsonExporterTest {

    private val wifiScanDao: WifiScanDao = mockk()
    private val btScanDao: BtScanDao = mockk()
    private val cellScanDao: CellScanDao = mockk()
    private val exporter = GeoJsonExporter(wifiScanDao, btScanDao, cellScanDao)
    private lateinit var outputDir: File

    @Before
    fun setUp() {
        outputDir = createTempDir(prefix = "geojson-test")
        coEvery { wifiScanDao.getAll() } returns emptyList()
        coEvery { btScanDao.getAll() } returns emptyList()
        coEvery { cellScanDao.getAll() } returns emptyList()
    }

    @After
    fun tearDown() {
        outputDir.deleteRecursively()
    }

    private fun makeWifiEntity(
        lat: Double? = 47.2692,
        lon: Double? = 11.4041,
        alt: Double? = 574.0,
    ): WifiScanEntity = WifiScanEntity(
        timestamp = 1_718_352_000_000L,
        latitude = lat,
        longitude = lon,
        altitude = alt,
        ssid = "TestNet",
        bssid = "AA:BB:CC:DD:EE:FF",
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
        name = "Speaker",
        rssi = -70,
        deviceClass = 1024,
        isBle = true,
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

    private fun exportJson(): String {
        val file = runBlocking { exporter.export(outputDir) }
        return file.readText()
    }

    @Test
    fun `export produces valid FeatureCollection wrapper`() {
        val json = exportJson()
        assertTrue(json.contains("\"type\":\"FeatureCollection\""))
        assertTrue(json.contains("\"features\":["))
        assertTrue(json.endsWith("]}"))
    }

    @Test
    fun `each wifi record becomes a Feature`() {
        coEvery { wifiScanDao.getAll() } returns listOf(makeWifiEntity(), makeWifiEntity())
        val json = exportJson()
        val featureCount = "\"type\":\"Feature\"".toRegex().findAll(json).count()
        assertTrue(featureCount == 2)
    }

    @Test
    fun `feature has Point geometry type`() {
        coEvery { wifiScanDao.getAll() } returns listOf(makeWifiEntity())
        assertTrue(exportJson().contains("\"geometry\":{\"type\":\"Point\""))
    }

    @Test
    fun `coordinates order is longitude latitude altitude`() {
        coEvery { wifiScanDao.getAll() } returns listOf(makeWifiEntity(lat = 47.2692, lon = 11.4041, alt = 574.0))
        // lon first, then lat, then alt
        assertTrue(exportJson().contains("\"coordinates\":[11.4041,47.2692,574.0]"))
    }

    @Test
    fun `feature properties contain scan_type WIFI`() {
        coEvery { wifiScanDao.getAll() } returns listOf(makeWifiEntity())
        assertTrue(exportJson().contains("\"scan_type\":\"WIFI\""))
    }

    @Test
    fun `null coordinates produce has_location false property`() {
        coEvery { wifiScanDao.getAll() } returns listOf(makeWifiEntity(lat = null, lon = null, alt = null))
        val json = exportJson()
        assertTrue(json.contains("\"has_location\":false"))
        assertTrue(json.contains("\"coordinates\":null"))
    }

    @Test
    fun `bt record has scan_type BT`() {
        coEvery { btScanDao.getAll() } returns listOf(makeBtEntity())
        assertTrue(exportJson().contains("\"scan_type\":\"BT\""))
    }

    @Test
    fun `cell record has scan_type CELL`() {
        coEvery { cellScanDao.getAll() } returns listOf(makeCellEntity())
        assertTrue(exportJson().contains("\"scan_type\":\"CELL\""))
    }

    @Test
    fun `empty database produces empty features array`() {
        assertTrue(exportJson().contains("\"features\":[]"))
    }
}
