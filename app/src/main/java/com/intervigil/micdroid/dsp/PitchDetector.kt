package com.intervigil.micdroid.dsp

import kotlin.math.sqrt

/**
 * YIN Pitch Detection Algorithm implementation in Kotlin.
 * Optimized for low-latency real-time vocal pitch estimation.
 */
class PitchDetector(
    private val sampleRate: Int = 44100,
    private val bufferSize: Int = 1024,
    private val threshold: Float = 0.15f
) {
    private val yinBuffer = FloatArray(bufferSize / 2)
    private val minPeriod = (sampleRate / 1000).coerceAtLeast(2) // ~1000 Hz upper limit
    private val maxPeriod = (sampleRate / 65).coerceAtMost(bufferSize / 2 - 1) // ~65 Hz lower limit

    data class DetectionResult(
        val pitchHz: Float,
        val probability: Float,
        val isVoiced: Boolean,
        val rms: Float
    )

    fun detectPitch(audioBuffer: ShortArray, length: Int): DetectionResult {
        val windowSize = minOf(length, bufferSize)
        if (windowSize < 256) {
            return DetectionResult(0f, 0f, false, 0f)
        }

        // Calculate RMS to gate silence
        var sumSquares = 0.0
        for (i in 0 until windowSize) {
            val s = audioBuffer[i] / 32768.0
            sumSquares += s * s
        }
        val rms = sqrt(sumSquares / windowSize).toFloat()
        if (rms < 0.008f) {
            // Silence gate
            return DetectionResult(0f, 0f, false, rms)
        }

        // Step 1: Difference Function
        val halfWindow = windowSize / 2
        yinBuffer[0] = 1f
        var runningSum = 0f

        for (tau in 1 until halfWindow) {
            var diffSum = 0f
            for (j in 0 until halfWindow) {
                val diff = (audioBuffer[j] - audioBuffer[j + tau]).toFloat() / 32768.0f
                diffSum += diff * diff
            }
            yinBuffer[tau] = diffSum
        }

        // Step 2: Cumulative Mean Normalized Difference Function
        for (tau in 1 until halfWindow) {
            runningSum += yinBuffer[tau]
            yinBuffer[tau] = if (runningSum > 0f) {
                yinBuffer[tau] * tau / runningSum
            } else {
                1f
            }
        }

        // Step 3: Absolute Threshold search
        var tauEstimate = -1
        for (tau in minPeriod until minOf(maxPeriod, halfWindow)) {
            if (yinBuffer[tau] < threshold) {
                // Find local minimum
                var localTau = tau
                while (localTau + 1 < halfWindow && yinBuffer[localTau + 1] < yinBuffer[localTau]) {
                    localTau++
                }
                tauEstimate = localTau
                break
            }
        }

        if (tauEstimate == -1) {
            // No dip below threshold, search for global minimum as fallback
            var minVal = Float.MAX_VALUE
            for (tau in minPeriod until minOf(maxPeriod, halfWindow)) {
                if (yinBuffer[tau] < minVal) {
                    minVal = yinBuffer[tau]
                    tauEstimate = tau
                }
            }
            if (minVal > 0.45f) {
                return DetectionResult(0f, 1f - minVal, false, rms)
            }
        }

        // Step 4: Parabolic Interpolation for sub-sample accuracy
        val betterTau: Float = if (tauEstimate > 0 && tauEstimate < halfWindow - 1) {
            val s0 = yinBuffer[tauEstimate - 1]
            val s1 = yinBuffer[tauEstimate]
            val s2 = yinBuffer[tauEstimate + 1]
            val denom = 2f * (2f * s1 - s0 - s2)
            if (kotlin.math.abs(denom) > 1e-6) {
                tauEstimate + (s2 - s0) / denom
            } else {
                tauEstimate.toFloat()
            }
        } else {
            tauEstimate.toFloat()
        }

        val pitch = if (betterTau > 0f) sampleRate / betterTau else 0f
        val probability = (1f - yinBuffer[tauEstimate]).coerceIn(0f, 1f)
        val isVoiced = probability > 0.60f && pitch in 65f..1200f

        return DetectionResult(
            pitchHz = if (isVoiced) pitch else 0f,
            probability = probability,
            isVoiced = isVoiced,
            rms = rms
        )
    }
}
