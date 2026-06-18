package ua.constitution

import org.junit.Assert.assertEquals
import org.junit.Test
import ua.constitution.ui.formatMillisToMinutesSeconds

/**
 * Pure tests for the MM:SS formatter extracted from HomeScreen's anthem player in N4. Pins the
 * exact current behavior — notably that minutes are NOT wrapped at 60.
 */
class TimeFormatterTest {

    @Test
    fun `zero is 00 00`() {
        assertEquals("00:00", formatMillisToMinutesSeconds(0))
    }

    @Test
    fun `sub-minute and minute boundaries`() {
        assertEquals("00:24", formatMillisToMinutesSeconds(24_000))
        assertEquals("01:05", formatMillisToMinutesSeconds(65_000))
        assertEquals("01:24", formatMillisToMinutesSeconds(84_000)) // the anthem fallback duration
    }

    @Test
    fun `minutes are not wrapped at 60`() {
        assertEquals("61:01", formatMillisToMinutesSeconds(3_661_000))
    }

    @Test
    fun `sub-second truncates toward zero`() {
        assertEquals("00:00", formatMillisToMinutesSeconds(999))
    }
}
