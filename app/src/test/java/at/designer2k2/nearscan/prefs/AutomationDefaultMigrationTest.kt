package at.designer2k2.nearscan.prefs

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationDefaultMigrationTest {

    private val automation = booleanPreferencesKey("automation_commands_enabled")
    private val lat = doublePreferencesKey("nearscan_lat")

    @Test
    fun `fresh install gets automation explicitly off`() = runTest {
        val prefs = emptyPreferences()
        assertTrue(AutomationDefaultMigration.shouldMigrate(prefs))
        assertEquals(false, AutomationDefaultMigration.migrate(prefs)[automation])
    }

    @Test
    fun `install upgraded from 0_1_3 keeps automation on`() = runTest {
        val prefs = preferencesOf(lat to 47.0)
        assertTrue(AutomationDefaultMigration.shouldMigrate(prefs))
        val migrated = AutomationDefaultMigration.migrate(prefs)
        assertEquals(true, migrated[automation])
        assertEquals(47.0, migrated[lat]!!, 0.0) // other settings untouched
    }

    @Test
    fun `explicit choice is never overwritten - fresh install that later saves other settings stays off`() = runTest {
        // Fresh install: migration wrote false, then the user set a location. Next launch must
        // not reinterpret "store is non-empty" as a legacy install.
        val prefs = preferencesOf(automation to false, lat to 47.0)
        assertFalse(AutomationDefaultMigration.shouldMigrate(prefs))
    }

    @Test
    fun `explicit opt-in is kept`() = runTest {
        assertFalse(AutomationDefaultMigration.shouldMigrate(preferencesOf(automation to true)))
    }
}
