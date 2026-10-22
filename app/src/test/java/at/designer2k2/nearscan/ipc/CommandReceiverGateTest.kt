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
    fun `defaults block every command so a fresh install can't be driven by other apps`() {
        val defaults = NearScanSettings()
        (nonDestructive + CommandReceiver.ACTION_CLEAR_DATA).forEach {
            assertFalse(it, CommandReceiver.isAllowed(it, defaults))
        }
    }

    @Test
    fun `opting in allows non-destructive commands but still blocks CMD_CLEAR_DATA`() {
        val optedIn = NearScanSettings(automationCommandsEnabled = true)
        nonDestructive.forEach { assertTrue(it, CommandReceiver.isAllowed(it, optedIn)) }
        assertFalse(CommandReceiver.isAllowed(CommandReceiver.ACTION_CLEAR_DATA, optedIn))
    }

    @Test
    fun `CMD_CLEAR_DATA allowed only with both opt-ins`() {
        val both = NearScanSettings(automationCommandsEnabled = true, automationClearDataEnabled = true)
        assertTrue(CommandReceiver.isAllowed(CommandReceiver.ACTION_CLEAR_DATA, both))
    }

    @Test
    fun `master switch off blocks every command, including opted-in clear data`() {
        val off = NearScanSettings(automationCommandsEnabled = false, automationClearDataEnabled = true)
        (nonDestructive + CommandReceiver.ACTION_CLEAR_DATA).forEach {
            assertFalse(it, CommandReceiver.isAllowed(it, off))
        }
    }
}
