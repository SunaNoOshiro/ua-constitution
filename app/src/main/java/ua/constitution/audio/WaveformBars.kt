package ua.constitution.audio

/**
 * For each of [barCount] waveform bars, whether it is "played" given playback [progress] in [0,1].
 * Mirrors the former inline `(index.toFloat() / heights.size) <= progress` in AudioWaveformVisualizer:
 * a bar at index i is played when i/barCount <= progress (so the boundary bar IS played, e.g.
 * progress 0.5 with 24 bars marks indices 0..12).
 */
fun computeWaveformBarStates(barCount: Int, progress: Float): List<Boolean> =
    (0 until barCount).map { index -> (index.toFloat() / barCount) <= progress }
