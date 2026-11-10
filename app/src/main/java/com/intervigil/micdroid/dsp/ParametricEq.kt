package com.intervigil.micdroid.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 3-Band Studio Parametric Equalizer:
 * 1. Low Shelf (Sub/Bass warmth or rumble cut, ~150Hz)
 * 2. Peaking Mid (Vocal presence, clarity, and body, ~2200Hz)
 * 3. High Shelf (Air, sheen, and brightness, ~8000Hz)
 *
 * Implements Robert Bristow-Johnson Audio EQ Cookbook biquad filters.
 */
class ParametricEq(
    private val sampleRate: Int = 44100
) {
    private class Biquad {
        var b0 = 1.0f
        var b1 = 0.0f
        var b2 = 0.0f
        var a1 = 0.0f
        var a2 = 0.0f

        var x1 = 0.0f
        var x2 = 0.0f
        var y1 = 0.0f
        var y2 = 0.0f

        fun process(x0: Float): Float {
            val y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1
            x1 = x0
            y2 = y1
            y1 = y0
            return y0
        }

        fun reset() {
            x1 = 0.0f
            x2 = 0.0f
            y1 = 0.0f
            y2 = 0.0f
        }

        fun setLowShelf(sampleRate: Int, freqHz: Float, gainDb: Float) {
            val aLinear = Math.pow(10.0, (gainDb / 40.0).toDouble()).toFloat()
            val w0 = (2.0 * PI * freqHz / sampleRate).toFloat()
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 / 2.0f * sqrt(2.0f)
            val twoSqrtAAlpha = 2.0f * sqrt(aLinear) * alpha

            val a0 = (aLinear + 1.0f) + (aLinear - 1.0f) * cosW0 + twoSqrtAAlpha
            b0 = (aLinear * ((aLinear + 1.0f) - (aLinear - 1.0f) * cosW0 + twoSqrtAAlpha)) / a0
            b1 = (2.0f * aLinear * ((aLinear - 1.0f) - (aLinear + 1.0f) * cosW0)) / a0
            b2 = (aLinear * ((aLinear + 1.0f) - (aLinear - 1.0f) * cosW0 - twoSqrtAAlpha)) / a0
            a1 = (-2.0f * ((aLinear - 1.0f) + (aLinear + 1.0f) * cosW0)) / a0
            a2 = ((aLinear + 1.0f) + (aLinear - 1.0f) * cosW0 - twoSqrtAAlpha) / a0
        }

        fun setPeaking(sampleRate: Int, freqHz: Float, gainDb: Float, q: Float = 1.0f) {
            val aLinear = Math.pow(10.0, (gainDb / 40.0).toDouble()).toFloat()
            val w0 = (2.0 * PI * freqHz / sampleRate).toFloat()
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 / (2.0f * q)

            val a0 = 1.0f + alpha / aLinear
            b0 = (1.0f + alpha * aLinear) / a0
            b1 = (-2.0f * cosW0) / a0
            b2 = (1.0f - alpha * aLinear) / a0
            a1 = (-2.0f * cosW0) / a0
            a2 = (1.0f - alpha / aLinear) / a0
        }

        fun setHighShelf(sampleRate: Int, freqHz: Float, gainDb: Float) {
            val aLinear = Math.pow(10.0, (gainDb / 40.0).toDouble()).toFloat()
            val w0 = (2.0 * PI * freqHz / sampleRate).toFloat()
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 / 2.0f * sqrt(2.0f)
            val twoSqrtAAlpha = 2.0f * sqrt(aLinear) * alpha

            val a0 = (aLinear + 1.0f) - (aLinear - 1.0f) * cosW0 + twoSqrtAAlpha
            b0 = (aLinear * ((aLinear + 1.0f) + (aLinear - 1.0f) * cosW0 + twoSqrtAAlpha)) / a0
            b1 = (-2.0f * aLinear * ((aLinear - 1.0f) + (aLinear + 1.0f) * cosW0)) / a0
            b2 = (aLinear * ((aLinear + 1.0f) + (aLinear - 1.0f) * cosW0 - twoSqrtAAlpha)) / a0
            a1 = (2.0f * ((aLinear - 1.0f) - (aLinear + 1.0f) * cosW0)) / a0
            a2 = ((aLinear + 1.0f) - (aLinear - 1.0f) * cosW0 - twoSqrtAAlpha) / a0
        }
    }

    private val lowFilter = Biquad()
    private val midFilter = Biquad()
    private val highFilter = Biquad()

    private var cachedLowGain = 0f
    private var cachedMidGain = 0f
    private var cachedHighGain = 0f

    init {
        updateFilters(0f, 0f, 0f)
    }

    /**
     * Updates EQ gain bands in dB (-12 dB to +12 dB).
     */
    fun updateFilters(lowGainDb: Float, midGainDb: Float, highGainDb: Float) {
        if (lowGainDb != cachedLowGain) {
            cachedLowGain = lowGainDb.coerceIn(-15f, 15f)
            lowFilter.setLowShelf(sampleRate, 150f, cachedLowGain)
        }
        if (midGainDb != cachedMidGain) {
            cachedMidGain = midGainDb.coerceIn(-15f, 15f)
            midFilter.setPeaking(sampleRate, 2200f, cachedMidGain, 1.2f)
        }
        if (highGainDb != cachedHighGain) {
            cachedHighGain = highGainDb.coerceIn(-15f, 15f)
            highFilter.setHighShelf(sampleRate, 8000f, cachedHighGain)
        }
    }

    fun process(buffer: ShortArray, length: Int) {
        if (cachedLowGain == 0f && cachedMidGain == 0f && cachedHighGain == 0f) return

        for (i in 0 until length) {
            var x = buffer[i] / 32768.0f
            if (cachedLowGain != 0f) x = lowFilter.process(x)
            if (cachedMidGain != 0f) x = midFilter.process(x)
            if (cachedHighGain != 0f) x = highFilter.process(x)

            buffer[i] = (x * 32767f).coerceIn(-32768f, 32767f).toInt().toShort()
        }
    }

    fun reset() {
        lowFilter.reset()
        midFilter.reset()
        highFilter.reset()
    }
}
