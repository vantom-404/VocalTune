package com.intervigil.micdroid.dsp

import kotlin.math.PI
import kotlin.math.cos

/**
 * Real-time time-domain granular pitch shifter with dual-read pointer crossfade,
 * cubic Hermite interpolation, and soft-limiting clipping protection.
 * Preserves duration and vocal characteristics without altering playback speed.
 */
class PitchShifter(
    var sampleRate: Int = 44100,
    private val grainSize: Int = 1024
) {
    private val bufferSize = 8192
    private val bufferMask = bufferSize - 1
    private val circularBuffer = FloatArray(bufferSize)
    private var writeIndex = 0

    // Dual read pointers for grain cross-fading
    private var readPointer1 = 0.0
    private var readPointer2 = (grainSize / 2).toDouble()
    private var grainPhase = 0

    // Smoothing for retune speed
    private var currentPitchRatio = 1.0

    // Formant preservation filter states
    private var formantFilterPrev = 0f

    // Precomputed window table
    private val window = FloatArray(grainSize)

    init {
        // Hann window for grain envelope
        for (i in 0 until grainSize) {
            window[i] = (0.5 - 0.5 * cos(2.0 * PI * i / grainSize)).toFloat()
        }
    }

    /**
     * Process input samples and write pitch-shifted audio to output.
     * @param input Raw 16-bit PCM samples
     * @param output Destination 16-bit PCM buffer
     * @param targetRatio Pitch multiplier (e.g. 1.05 = shift up 5%, 0.95 = shift down)
     * @param strength Correction strength [0.0f .. 1.0f]
     * @param retuneSpeed Speed factor [0.0f (instant) .. 1.0f (slow/smooth)]
     * @param isVoiced Whether current frame contains voiced pitch
     * @param bypass If true, passes dry signal without modification
     */
    fun process(
        input: ShortArray,
        output: ShortArray,
        length: Int,
        targetRatio: Double,
        strength: Float,
        retuneSpeed: Float,
        isVoiced: Boolean,
        bypass: Boolean
    ) {
        if (bypass || !isVoiced || strength <= 0.01f) {
            // Immediate pass-through
            System.arraycopy(input, 0, output, 0, length)
            currentPitchRatio = 1.0
            return
        }

        // Apply strength factor: ratio = 1.0 + strength * (targetRatio - 1.0)
        val clampedTarget = targetRatio.coerceIn(0.5, 2.0)
        val desiredRatio = 1.0 + strength.toDouble() * (clampedTarget - 1.0)

        // Retune speed smoothing:
        // retuneSpeed: 0.0 -> instant (alpha = 1.0)
        // retuneSpeed: 1.0 -> slow/smooth (alpha = 0.05)
        val alpha = (1.0 - (retuneSpeed * 0.90)).coerceIn(0.05, 1.0)
        currentPitchRatio += alpha * (desiredRatio - currentPitchRatio)

        val ratio = currentPitchRatio.coerceIn(0.6, 1.8)

        // Formant tilt coefficient to mitigate chipmunk spectral brightening when shifting up
        val formantTilt = ((1.0 - ratio) * 0.15).toFloat().coerceIn(-0.25f, 0.25f)

        for (i in 0 until length) {
            // Write input to circular buffer
            circularBuffer[writeIndex] = input[i].toFloat()

            // Read grain 1 with cubic interpolation
            val sample1 = readCubicInterpolated(readPointer1)
            // Read grain 2 (offset by 180 degrees)
            val sample2 = readCubicInterpolated(readPointer2)

            // Crossfade envelope
            val winIdx1 = grainPhase
            val winIdx2 = (grainPhase + grainSize / 2) % grainSize

            val w1 = window[winIdx1]
            val w2 = window[winIdx2]

            // Recombined pitch-shifted sample
            var shiftedSample = (sample1 * w1 + sample2 * w2)

            // Gentle one-pole spectral tilt for basic formant balance preservation
            shiftedSample = shiftedSample + formantTilt * (shiftedSample - formantFilterPrev)
            formantFilterPrev = shiftedSample

            // Soft-clipping protection
            output[i] = softLimit(shiftedSample)

            // Advance pointers
            readPointer1 += ratio
            readPointer2 += ratio
            writeIndex = (writeIndex + 1) and bufferMask

            grainPhase++
            if (grainPhase >= grainSize) {
                grainPhase = 0
                // Re-anchor read pointers near the current write head to prevent drift
                val lag = grainSize * 1.5
                readPointer1 = (writeIndex - lag + bufferSize) % bufferSize
                readPointer2 = (readPointer1 + grainSize / 2) % bufferSize
            }
        }
    }

    /**
     * 4-point, 3rd-order Hermite cubic interpolation for artifact-free audio resampling.
     */
    private fun readCubicInterpolated(pos: Double): Float {
        var p = pos
        while (p < 0) p += bufferSize
        while (p >= bufferSize) p -= bufferSize

        val i1 = p.toInt()
        val i0 = (i1 - 1 + bufferSize) and bufferMask
        val i2 = (i1 + 1) and bufferMask
        val i3 = (i1 + 2) and bufferMask
        val frac = (p - i1).toFloat()

        val y0 = circularBuffer[i0]
        val y1 = circularBuffer[i1]
        val y2 = circularBuffer[i2]
        val y3 = circularBuffer[i3]

        val c0 = y1
        val c1 = 0.5f * (y2 - y0)
        val c2 = y0 - 2.5f * y1 + 2.0f * y2 - 0.5f * y3
        val c3 = 0.5f * (y3 - y0) + 1.5f * (y1 - y2)

        return ((c3 * frac + c2) * frac + c1) * frac + c0
    }

    /**
     * Soft-knee limiter preventing digital wrapping and distortion.
     */
    private fun softLimit(sample: Float): Short {
        val norm = sample / 32768.0f
        val limited = when {
            norm > 1.25f -> 0.98f
            norm < -1.25f -> -0.98f
            norm > 0.85f -> 0.85f + 0.15f * (1.0f - kotlin.math.exp(-((norm - 0.85f) / 0.15f)))
            norm < -0.85f -> -0.85f - 0.15f * (1.0f - kotlin.math.exp(-((-norm - 0.85f) / 0.15f)))
            else -> norm
        }
        return (limited * 32767.0f).toInt().toShort()
    }

    fun reset() {
        circularBuffer.fill(0f)
        writeIndex = 0
        readPointer1 = 0.0
        readPointer2 = (grainSize / 2).toDouble()
        grainPhase = 0
        currentPitchRatio = 1.0
        formantFilterPrev = 0f
    }
}
