package ua.constitution

import org.junit.Assert.assertEquals
import org.junit.Test
import ua.constitution.audio.computeWaveformBarStates

/**
 * Pure tests for the waveform bar-highlight math extracted from AudioWaveformVisualizer in N4.
 * Boundary rule pinned verbatim: bar i is played when i/barCount <= progress (boundary IS played).
 */
class WaveformBarsTest {

    @Test
    fun `progress zero plays only the first bar`() {
        // index 0 -> 0f <= 0f is true; all others false.
        val states = computeWaveformBarStates(24, 0f)
        assertEquals(24, states.size)
        assertEquals(true, states[0])
        assertEquals(1, states.count { it })
    }

    @Test
    fun `progress one plays every bar`() {
        assertEquals(24, computeWaveformBarStates(24, 1f).count { it })
    }

    @Test
    fun `progress half plays through the boundary bar inclusive`() {
        // 24 bars, progress 0.5: indices 0..12 satisfy i/24 <= 0.5 (12/24 == 0.5) => 13 bars.
        val states = computeWaveformBarStates(24, 0.5f)
        assertEquals(13, states.count { it })
        assertEquals(true, states[12])
        assertEquals(false, states[13])
    }

    @Test
    fun `zero bars yields an empty list`() {
        assertEquals(emptyList<Boolean>(), computeWaveformBarStates(0, 0.5f))
    }
}
