package at.designer2k2.nearscan.mqtt

import at.designer2k2.nearscan.db.BtScanEntity
import at.designer2k2.nearscan.db.CellScanEntity
import at.designer2k2.nearscan.db.WifiScanEntity
import at.designer2k2.nearscan.extra.ExtraFields
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertTrue
import org.junit.Test

class MqttPublisherTest {

    private val mqttClient: MqttClient = mockk(relaxed = true)
    private val publisher = MqttPublisher(mqttClient)

    private fun wifiEntity(
        ssid: String? = "MyNetwork",
        bssid: String = "AA:BB:CC:DD:EE:FF",
        lat: Double? = 47.2692,
        lon: Double? = 11.4041,
        alt: Double? = 574.0,
    ): WifiScanEntity = WifiScanEntity(
        timestamp = 1_718_352_000_000L,
        latitude = lat,
        longitude = lon,
        altitude = alt,
        ssid = ssid,
        bssid = bssid,
        rssi = -65,
        frequency = 2437,
        channel = 6,
        capabilities = "[WPA2]",
        band = "2.4",
    )

    private fun btEntity(): BtScanEntity = BtScanEntity(
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

    private fun cellEntity(): CellScanEntity = CellScanEntity(
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

    private fun capturePayload(block: (MqttPublisher) -> Unit): String {
        val slot = slot<String>()
        every { mqttClient.publish(any(), capture(slot)) } just Runs
        block(publisher)
        return slot.captured
    }

    @Test
    fun `publish WiFi entity calls mqttClient publish`() {
        publisher.publish("test/topic", wifiEntity(), 47.0, 11.0, 574.0)
        verify { mqttClient.publish("test/topic", any()) }
    }

    @Test
    fun `WiFi payload contains type WIFI`() {
        val payload = capturePayload { it.publish("t", wifiEntity(), 47.0, 11.0, 574.0) }
        assertTrue(payload.contains("\"type\":\"WIFI\""))
    }

    @Test
    fun `WiFi payload contains ssid and bssid`() {
        val payload = capturePayload {
            it.publish("t", wifiEntity(ssid = "MyNetwork", bssid = "AA:BB:CC:DD:EE:FF"), 47.0, 11.0, 574.0)
        }
        assertTrue(payload.contains("\"ssid\":\"MyNetwork\""))
        assertTrue(payload.contains("\"bssid\":\"AA:BB:CC:DD:EE:FF\""))
    }

    @Test
    fun `WiFi payload contains rssi`() {
        val payload = capturePayload { it.publish("t", wifiEntity(), 47.0, 11.0, 574.0) }
        assertTrue(payload.contains("\"rssi\":-65"))
    }

    @Test
    fun `BT payload contains type BT`() {
        val payload = capturePayload { it.publish("t", btEntity(), 47.0, 11.0, 574.0) }
        assertTrue(payload.contains("\"type\":\"BT\""))
    }

    @Test
    fun `BT payload contains address`() {
        val payload = capturePayload { it.publish("t", btEntity(), 47.0, 11.0, 574.0) }
        assertTrue(payload.contains("\"address\":\"AA:BB:CC:11:22:33\""))
    }

    @Test
    fun `BT payload contains is_ble`() {
        val payload = capturePayload { it.publish("t", btEntity(), 47.0, 11.0, 574.0) }
        assertTrue(payload.contains("\"is_ble\":true"))
    }

    @Test
    fun `Cell payload contains type CELL`() {
        val payload = capturePayload { it.publish("t", cellEntity(), 47.0, 11.0, 574.0) }
        assertTrue(payload.contains("\"type\":\"CELL\""))
    }

    @Test
    fun `Cell payload contains technology`() {
        val payload = capturePayload { it.publish("t", cellEntity(), 47.0, 11.0, 574.0) }
        assertTrue(payload.contains("\"technology\":\"LTE\""))
    }

    @Test
    fun `null lat lon serialize as null in JSON`() {
        // Entity has null coords and no extra fallback coords supplied.
        val payload = capturePayload {
            it.publish("t", wifiEntity(lat = null, lon = null, alt = null), null, null, null)
        }
        assertTrue(payload.contains("\"lat\":null"))
        assertTrue(payload.contains("\"lon\":null"))
    }

    @Test
    fun `extra fields appended when ExtraFields provided`() {
        val payload = capturePayload {
            it.publish("t", wifiEntity(), 47.0, 11.0, 574.0, ExtraFields(batteryLevel = 85))
        }
        assertTrue(payload.contains("\"battery_level\":85"))
    }
}
