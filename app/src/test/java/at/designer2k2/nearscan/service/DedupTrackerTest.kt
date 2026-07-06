package at.designer2k2.nearscan.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DedupTrackerTest {

    private lateinit var tracker: DedupTracker

    @Before
    fun setUp() {
        tracker = DedupTracker()
    }

    @Test
    fun `first sighting of a BSSID is always logged`() {
        assertTrue(tracker.shouldLogWifi("AA:BB:CC:DD:EE:FF", -65))
    }

    @Test
    fun `repeat sighting within 3dBm window is skipped`() {
        tracker.shouldLogWifi("AA:BB:CC:DD:EE:FF", -65)
        assertFalse(tracker.shouldLogWifi("AA:BB:CC:DD:EE:FF", -67))
    }

    @Test
    fun `repeat sighting exactly 3dBm apart is skipped`() {
        tracker.shouldLogWifi("AA:BB:CC:DD:EE:FF", -65)
        assertFalse(tracker.shouldLogWifi("AA:BB:CC:DD:EE:FF", -68))
    }

    @Test
    fun `repeat sighting more than 3dBm apart is logged`() {
        tracker.shouldLogWifi("AA:BB:CC:DD:EE:FF", -65)
        assertTrue(tracker.shouldLogWifi("AA:BB:CC:DD:EE:FF", -69))
    }

    @Test
    fun `logging updates the tracked RSSI so a further small drift stays deduped`() {
        tracker.shouldLogWifi("AA:BB:CC:DD:EE:FF", -65)
        assertTrue(tracker.shouldLogWifi("AA:BB:CC:DD:EE:FF", -70)) // logged, now tracked = -70
        assertFalse(tracker.shouldLogWifi("AA:BB:CC:DD:EE:FF", -72)) // within 3dBm of -70
    }

    @Test
    fun `different BSSIDs are tracked independently`() {
        tracker.shouldLogWifi("AA:BB:CC:DD:EE:FF", -65)
        assertTrue(tracker.shouldLogWifi("11:22:33:44:55:66", -65))
    }

    @Test
    fun `BT dedup behaves the same as WiFi dedup`() {
        tracker.shouldLogBt("AA:BB:CC:11:22:33", -70)
        assertFalse(tracker.shouldLogBt("AA:BB:CC:11:22:33", -71))
        assertTrue(tracker.shouldLogBt("AA:BB:CC:11:22:33", -75))
    }

    @Test
    fun `WiFi and BT tracking do not interfere with each other`() {
        tracker.shouldLogWifi("AA:BB:CC:DD:EE:FF", -65)
        assertTrue(tracker.shouldLogBt("AA:BB:CC:DD:EE:FF", -65))
    }

    @Test
    fun `reset clears tracked state so next sighting logs again`() {
        tracker.shouldLogWifi("AA:BB:CC:DD:EE:FF", -65)
        assertFalse(tracker.shouldLogWifi("AA:BB:CC:DD:EE:FF", -66))
        tracker.reset()
        assertTrue(tracker.shouldLogWifi("AA:BB:CC:DD:EE:FF", -66))
    }
}
