package com.intervigil.micdroid.dsp

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max

/**
 * Studio Dynamic Range Vocal Compressor.
 * Tames dynamics, levels vocal peaks, and provides makeup gain.
 * Features an exponential peak detector envelope and smooth gain reduction.
 */
class Compressor(
    private val sampleRate: Int = 44100
) {
    // Smoothed gain reduction envelope (linear multiplier [0.0f .. 1.0f])
    private var envelopeGain = 1.0f

    /**
     * Process 16-bit PCM audio samples in-place.
     * @param buffer 16-bit PCM samples
     * @param length Number of samples
     * @param thresholdDb Compression threshold in dB (-40 to 0)
     * @param ratio Compression ratio (1.0 to 10.0)
     * @param attackMs Attack time in ms (~5 to 30ms)
     * @param releaseMs Release time in ms (~50 to 250ms)
     * @param makeupGainDb Post-compression makeup gain in dB (0 to 12dB)
     * @param enabled Whether compressor is active
     */
    fun process(
        buffer: ShortArray,
        length: Int,
        thresholdDb: Float = -20f,
        ratio: Float = 3.5f,
        attackMs: Float = 12f,
        releaseMs: Float = 100f,
        makeupGainDb: Float = 3.0f,
        enabled: Boolean = true
    ) {
        if (!enabled || ratio <= 1.05f) return

        val attackCoef = exp(-1.0f / (sampleRate * (attackMs / 1000f).coerceAtLeast(0.001f)))
        val releaseCoef = exp(-1.0f / (sampleRate * (releaseMs / 1000f).coerceAtLeast(0.005f)))
        val makeupGainLinear = Math.pow(10.0, (makeupGainDb / 20.0).toDouble()).toFloat()

        val slope = 1.0f - (1.0f / ratio.coerceAtLeast(1.0f))

        for (i in 0 until length) {
            val sample = buffer[i] / 32768.0f
            val absSample = abs(sample)

            // Convert peak to dB
            val inputDb = if (absSample > 1e-5f) (20.0f * log10(absSample)) else -100f

            // Calculate gain reduction in dB
            val overDb = inputDb - thresholdDb
            val targetGainDb = if (overDb > 0f) -slope * overDb else 0.0f
            val targetGainLinear = Math.pow(10.0, (targetGainDb / 20.0).toDouble()).toFloat()

            // Smooth envelope follower: if targetGain < envelopeGain (compressing), use attack; else release
            envelopeGain = if (targetGainLinear < envelopeGain) {
                attackCoef * envelopeGain + (1.0f - attackCoef) * targetGainLinear
            } else {
                releaseCoef * envelopeGain + (1.0f - releaseCoef) * targetGainLinear
            }

            // Apply compressed envelope and makeup gain
            val output = sample * envelopeGain * makeupGainLinear

            // Soft-knee clipping protection
            val clipped = if (output > 1.0f) {
                1.0f - exp(-output) * 0.368f
            } else if (output < -1.0f) {
                -1.0f + exp(output) * 0.368f
            } else {
                output
            }

            buffer[i] = (clipped * 32767f).coerceIn(-32768f, 32767f).toInt().toShort()
        }
    }

    fun reset() {
        envelopeGain = 1.0f
    }
}
