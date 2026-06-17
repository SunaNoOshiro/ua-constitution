package ua.constitution.audio

class ProceduralAnthemSynth {
    private var audioTrack: android.media.AudioTrack? = null
    private var isPlaying = false

    fun play(onComplete: () -> Unit) {
        if (isPlaying) return
        isPlaying = true
        Thread {
            try {
                val sampleRate = 22050
                val minBufferSize = android.media.AudioTrack.getMinBufferSize(
                    sampleRate,
                    android.media.AudioFormat.CHANNEL_OUT_MONO,
                    android.media.AudioFormat.ENCODING_PCM_16BIT
                )
                val track = android.media.AudioTrack(
                    android.media.AudioManager.STREAM_MUSIC,
                    sampleRate,
                    android.media.AudioFormat.CHANNEL_OUT_MONO,
                    android.media.AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize.coerceAtLeast(4096),
                    android.media.AudioTrack.MODE_STREAM
                )
                audioTrack = track
                track.play()

                // Melody of the anthem of Ukraine: frequency in Hz, duration in ms
                // BPM = 90. Quarter note = 667ms
                val quarter = 667
                val half = 1333
                val eighth = 333

                val melody = listOf(
                    // "Shche ne vmerla Ukrainy"
                    Pair(369.99f, eighth),  // F#4
                    Pair(392.00f, eighth),  // G4
                    Pair(440.00f, quarter), // A4
                    Pair(493.88f, eighth),  // B4
                    Pair(440.00f, quarter), // A4
                    Pair(392.00f, eighth),  // G4
                    Pair(369.99f, quarter), // F#4
                    Pair(329.63f, eighth),  // E4
                    Pair(293.66f, half),    // D4

                    // "i slava, i volia"
                    Pair(369.99f, eighth),  // F#4
                    Pair(392.00f, eighth),  // G4
                    Pair(440.00f, quarter), // A4
                    Pair(493.88f, eighth),  // B4
                    Pair(440.00f, quarter), // A4
                    Pair(392.00f, eighth),  // G4
                    Pair(493.88f, half),    // B4
                    Pair(440.00f, half),    // A4

                    // "Shche nam, brattia molodii"
                    Pair(369.99f, eighth),  // F#4
                    Pair(392.00f, eighth),  // G4
                    Pair(440.00f, quarter), // A4
                    Pair(493.88f, eighth),  // B4
                    Pair(440.00f, quarter), // A4
                    Pair(392.00f, eighth),  // G4
                    Pair(369.99f, quarter), // F#4
                    Pair(329.63f, eighth),  // E4
                    Pair(293.66f, half),    // D4

                    // "usmiknetsia dolia."
                    Pair(369.99f, eighth),  // F#4
                    Pair(392.00f, eighth),  // G4
                    Pair(440.00f, quarter), // A4
                    Pair(329.63f, eighth),  // E4
                    Pair(369.99f, quarter), // F#4
                    Pair(329.63f, eighth),  // E4
                    Pair(293.66f, half),    // D4

                    // Chorus: "Zhynut nashi vorizhenky" (chorus)
                    Pair(440.00f, quarter), // A4
                    Pair(440.00f, eighth),  // A4
                    Pair(493.88f, quarter), // B4
                    Pair(493.88f, eighth),  // B4
                    Pair(523.25f, quarter), // C5
                    Pair(523.25f, eighth),  // C5
                    Pair(493.88f, half),    // B4

                    // "yak rosa na sontsi"
                    Pair(440.00f, eighth),  // A4
                    Pair(392.00f, eighth),  // G4
                    Pair(369.99f, quarter), // F#4
                    Pair(329.63f, eighth),  // E4
                    Pair(293.66f, quarter), // D4
                    Pair(329.63f, eighth),  // E4
                    Pair(369.99f, half),    // F#4
                    Pair(440.00f, half)     // A4
                )

                for (note in melody) {
                    if (!isPlaying) break
                    val freq = note.first
                    val dur = note.second

                    val numSamples = (dur * sampleRate) / 1000
                    val samples = ShortArray(numSamples)

                    for (i in 0 until numSamples) {
                        val t = i.toDouble() / sampleRate
                        var s = Math.sin(2.0 * Math.PI * freq * t)

                        val envelope = when {
                            i < sampleRate * 0.05 -> i / (sampleRate * 0.05) // Attack (50ms)
                            i > numSamples - sampleRate * 0.05 -> (numSamples - i) / (sampleRate * 0.05) // Release (50ms)
                            else -> 1.0
                        }

                        s += 0.3 * Math.sin(2.0 * Math.PI * (freq * 2.0) * t)
                        samples[i] = (s * 10000.0 * envelope).toInt().toShort()
                    }
                    track.write(samples, 0, numSamples)

                    val gapSamples = (40 * sampleRate) / 1000
                    val gap = ShortArray(gapSamples)
                    track.write(gap, 0, gapSamples)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isPlaying = false
                try {
                    audioTrack?.stop()
                    audioTrack?.release()
                } catch (e: Exception) {}
                audioTrack = null
                onComplete()
            }
        }.start()
    }

    fun stop() {
        isPlaying = false
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {}
        audioTrack = null
    }
}
