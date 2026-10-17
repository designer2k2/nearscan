package at.designer2k2.nearscan.ipc

import at.designer2k2.nearscan.prefs.NearScanSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandReceiverGateTest {

    private val nonDestructive = listOf(
        CommandReceiver.ACTION_START,
        CommandReceiver.ACTION_STOP,
        CommandReceiver.ACTION_TOGGLE,
        CommandReceiver.ACTION_EXPORT,
        CommandReceiver.ACTION_SET_LOCATION,
        CommandReceiver.ACTION_SET_INTERVAL,
    )

    @Test
    fun `defaults allow non-destructive commands so existing Tasker setups keep working`() {
        val defaults = NearScanSettings()
        nonDestructive.forEach { assertTrue(it, CommandReceiver.isAllowed(it, defaults)) }
    }

    @Test
    fun `defaults block CMD_CLEAR_DATA`() {
        assertFalse(CommandReceiver.isAllowed(CommandReceiver.ACTION_CLEAR_DATA, NearScanSettings()))
    }

    @Test
    fun `CMD_CLEAR_DATA allowed only after explicit opt-in`() {
        val optedIn = NearScanSettings(automationClearDataEnabled = true)
        assertTrue(CommandReceiver.isAllowed(CommandReceiver.ACTION_CLEAR_DATA, optedIn))
    }

    @Test
    fun `master switch off blocks every command, including opted-in clear data`() {
        val off = NearScanSettings(automationCommandsEnabled = false, automationClearDataEnabled = true)
        (nonDestructive + CommandReceiver.ACTION_CLEAR_DATA).forEach {
            assertFalse(it, CommandReceiver.isAllowed(it, off))
        }
    }
}
