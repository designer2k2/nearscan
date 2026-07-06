package at.designer2k2.nearscan.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FlexibleExtraTest {

    @Test
    fun `parseDouble parses a Tasker-style string extra`() {
        assertEquals(47.5, FlexibleExtra.parseDouble("47.5", Double.NaN)!!, 0.0)
    }

    @Test
    fun `parseDouble falls back to the typed value when string is not parseable`() {
        assertEquals(12.3, FlexibleExtra.parseDouble(null, 12.3)!!, 0.0)
    }

    @Test
    fun `parseDouble prefers the string value over the typed value when both are present`() {
        assertEquals(1.0, FlexibleExtra.parseDouble("1.0", 2.0)!!, 0.0)
    }

    @Test
    fun `parseDouble returns null when neither string nor typed value is usable`() {
        assertNull(FlexibleExtra.parseDouble(null, Double.NaN))
    }

    @Test
    fun `parseDouble returns null for garbage string with no typed fallback`() {
        assertNull(FlexibleExtra.parseDouble("not-a-number", Double.NaN))
    }

    @Test
    fun `parseInt parses a Tasker-style string extra`() {
        assertEquals(30, FlexibleExtra.parseInt("30", Int.MIN_VALUE))
    }

    @Test
    fun `parseInt falls back to the typed value when string is not parseable`() {
        assertEquals(42, FlexibleExtra.parseInt(null, 42))
    }

    @Test
    fun `parseInt returns null when neither string nor typed value is usable`() {
        assertNull(FlexibleExtra.parseInt(null, Int.MIN_VALUE))
    }
}
