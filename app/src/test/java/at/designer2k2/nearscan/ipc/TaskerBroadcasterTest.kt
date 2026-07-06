package at.designer2k2.nearscan.ipc

import android.content.Context
import at.designer2k2.nearscan.db.BtScanEntity
import at.designer2k2.nearscan.db.CellScanEntity
import at.designer2k2.nearscan.db.WifiScanEntity
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Exercises the seen-set dedup logic that decides whether TaskerBroadcaster fires a NEW_*
 * broadcast, via call-count assertions on [Context.sendBroadcast]. Real [android.content.Intent]
 * instances constructed in production code aren't asserted on directly: this module's unit
 * tests run against the Android SDK stub jar (`isReturnDefaultValues = true`, no Robolectric),
 * so a real (non-mocked) Intent's putExtra/getStringExtra round-trip is a no-op — only the
 * mocked [Context] call itself is reliably observable.
 */
class TaskerBroadcasterTest {

    private val context: Context = mockk(relaxed = true)
    private lateinit var broadcaster: TaskerBroadcaster
    private var sendCount = 0

    @Before
    fun setUp() {
        sendCount = 0
        every { context.sendBroadcast(any()) } answers { sendCount++; Unit }
        broadcaster = TaskerBroadcaster(context)
    }

    private fun wifiEntity(bssid: String = "AA:BB:CC:DD:EE:FF"): WifiScanEntity = WifiScanEntity(
        timestamp = 1L, latitude = 47.0, longitude = 11.0, altitude = 500.0,
        ssid = "Net", bssid = bssid, rssi = -65, frequency = 2437, channel = 6,
        capabilities = "[WPA2]", band = "2.4",
    )

    private fun btEntity(address: String = "AA:BB:CC:11:22:33"): BtScanEntity = BtScanEntity(
        timestamp = 1L, latitude = 47.0, longitude = 11.0, altitude = 500.0,
        address = address, name = "Dev", rssi = -70, deviceClass = 0, isBle = false,
    )

    private fun cellEntity(cid: Long?): CellScanEntity = CellScanEntity(
        timestamp = 1L, latitude = 47.0, longitude = 11.0, altitude = 500.0,
        mcc = 232, mnc = 1, lac = 100, cid = cid, rssi = -95, technology = "LTE",
    )

    @Test
    fun `onScanStarted sends exactly one broadcast`() {
        broadcaster.onScanStarted()
        assertEquals(1, sendCount)
    }

    @Test
    fun `onScanStopped sends exactly one broadcast`() {
        broadcaster.onScanStopped(5, 3, 1, 120L)
        assertEquals(1, sendCount)
    }

    @Test
    fun `onNewWifi fires once for first sighting of a BSSID`() {
        broadcaster.onNewWifi(listOf(wifiEntity()))
        assertEquals(1, sendCount)
    }

    @Test
    fun `onNewWifi does not refire for the same BSSID again this session`() {
        broadcaster.onNewWifi(listOf(wifiEntity(bssid = "AA:BB:CC:DD:EE:FF")))
        broadcaster.onNewWifi(listOf(wifiEntity(bssid = "AA:BB:CC:DD:EE:FF")))
        assertEquals(1, sendCount)
    }

    @Test
    fun `onNewWifi fires again after reset`() {
        broadcaster.onNewWifi(listOf(wifiEntity()))
        broadcaster.reset()
        broadcaster.onNewWifi(listOf(wifiEntity()))
        assertEquals(2, sendCount)
    }

    @Test
    fun `onNewWifi fires once per distinct BSSID in a batch`() {
        broadcaster.onNewWifi(listOf(wifiEntity(bssid = "AA:11"), wifiEntity(bssid = "BB:22")))
        assertEquals(2, sendCount)
    }

    @Test
    fun `WiFi and BT seen-sets are independent`() {
        broadcaster.onNewWifi(listOf(wifiEntity(bssid = "SAME")))
        broadcaster.onNewBt(listOf(btEntity(address = "SAME")))
        assertEquals(2, sendCount)
    }

    @Test
    fun `BT and BLE seen-sets are independent`() {
        broadcaster.onNewBt(listOf(btEntity(address = "AA:11")))
        broadcaster.onNewBle(listOf(btEntity(address = "AA:11")))
        assertEquals(2, sendCount)
    }

    @Test
    fun `onNewBt does not refire for the same address`() {
        broadcaster.onNewBt(listOf(btEntity(address = "AA:11")))
        broadcaster.onNewBt(listOf(btEntity(address = "AA:11")))
        assertEquals(1, sendCount)
    }

    @Test
    fun `onNewCell fires once per distinct CID and skips null cid`() {
        broadcaster.onNewCell(listOf(cellEntity(cid = 111L), cellEntity(cid = null)))
        assertEquals(1, sendCount)
    }

    @Test
    fun `onNewCell does not refire for the same CID`() {
        broadcaster.onNewCell(listOf(cellEntity(cid = 111L)))
        broadcaster.onNewCell(listOf(cellEntity(cid = 111L)))
        assertEquals(1, sendCount)
    }

    @Test
    fun `onRoundComplete always sends a broadcast`() {
        broadcaster.onRoundComplete(1, 2, 3)
        broadcaster.onRoundComplete(1, 2, 3)
        assertEquals(2, sendCount)
    }

    @Test
    fun `onExportComplete sends exactly one broadcast`() {
        broadcaster.onExportComplete(File("/tmp/export.csv"), "wigle_csv", 42L)
        assertEquals(1, sendCount)
    }

    @Test
    fun `empty batches never send a broadcast`() {
        broadcaster.onNewWifi(emptyList())
        broadcaster.onNewBt(emptyList())
        broadcaster.onNewBle(emptyList())
        broadcaster.onNewCell(emptyList())
        assertEquals(0, sendCount)
    }
}
