package com.intervigil.micdroid.dsp

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10

/**
 * Studio Noise Gate DSP processor to eliminate background room noise,
 * fan hum, and microphone hiss during vocal pauses.
 * Uses a smooth exponential attack/release envelope to prevent audio clicking.
 */
class NoiseGate(
    private val sampleRate: Int = 44100
) {
    // Current smoothed gain [0.0f .. 1.0f]
    private var envelopeGain = 1.0f

    /**
     * Processes 16-bit PCM samples in-place.
     * @param buffer Samples array
     * @param length Number of samples
     * @param thresholdDb Threshold below which gate closes (-60 dB to -20 dB)
     * @param attackMs Time in ms for gate to open upon vocal onset (~2 to 10ms)
     * @param releaseMs Time in ms for gate to close after voice stops (~50 to 200ms)
     * @param enabled Whether gate is active
     */
    fun process(
        buffer: ShortArray,
        length: Int,
        thresholdDb: Float = -45f,
        attackMs: Float = 5f,
        releaseMs: Float = 80f,
        enabled: Boolean = true
    ) {
        if (!enabled || thresholdDb <= -60f) return

        val attackCoef = exp(-1.0f / (sampleRate * (attackMs / 1000f).coerceAtLeast(0.001f)))
        val releaseCoef = exp(-1.0f / (sampleRate * (releaseMs / 1000f).coerceAtLeast(0.005f)))
        val thresholdLinear = Math.pow(10.0, (thresholdDb / 20.0).toDouble()).toFloat()

        for (i in 0 until length) {
            val sample = buffer[i] / 32768.0f
            val sampleAbs = abs(sample)

            // Determine target gain: 1.0 if above threshold, 0.0 if below
            val targetGain = if (sampleAbs >= thresholdLinear) 1.0f else 0.0f

            // Smooth envelope follower
            envelopeGain = if (targetGain > envelopeGain) {
                attackCoef * envelopeGain + (1.0f - attackCoef) * targetGain
            } else {
                releaseCoef * envelopeGain + (1.0f - releaseCoef) * targetGain
            }

            val gatedSample = sample * envelopeGain
            buffer[i] = (gatedSample * 32767f).coerceIn(-32768f, 32767f).toInt().toShort()
        }
    }

    fun reset() {
        envelopeGain = 1.0f
    }
}
