package com.intervigil.micdroid.dsp

/**
 * Studio Echo / Delay Effect with feedback, delay time (ms),
 * high-cut damping, and wet/dry mix.
 */
class DelayEffect(
    private val sampleRate: Int = 44100,
    maxDelayMs: Int = 1000
) {
    private val maxDelaySamples = (sampleRate * maxDelayMs) / 1000
    private val delayBuffer = FloatArray(maxDelaySamples)
    private var writeIndex = 0
    private var filterStore = 0f

    /**
     * Process audio buffer in-place.
     * @param buffer 16-bit PCM samples
     * @param length Number of samples
     * @param delayMs Delay time in milliseconds (e.g. 50ms to 600ms)
     * @param feedback Feedback intensity (0.0f to 0.85f)
     * @param wet Wet mix (0.0f to 1.0f)
     * @param damping High frequency damping (0.0f = crisp, 0.6f = warm analog tape echo)
     */
    fun process(
        buffer: ShortArray,
        length: Int,
        delayMs: Float,
        feedback: Float,
        wet: Float,
        damping: Float = 0.3f
    ) {
        if (wet <= 0.005f) return

        val delaySamples = ((delayMs * sampleRate) / 1000f).toInt().coerceIn(100, maxDelaySamples - 1)
        val fb = feedback.coerceIn(0.0f, 0.85f)
        val dry = 1.0f - wet * 0.3f
        val wetScaled = wet * 0.7f
        val damp = damping.coerceIn(0.0f, 0.8f)

        for (i in 0 until length) {
            val input = buffer[i] / 32768.0f

            // Read from delayed position
            var readIndex = writeIndex - delaySamples
            if (readIndex < 0) readIndex += maxDelaySamples

            val delayed = delayBuffer[readIndex]

            // Apply 1-pole low-pass damping to feedback for vintage warm repeats
            filterStore = (delayed * (1.0f - damp)) + (filterStore * damp)

            // Write input + feedback into delay line
            delayBuffer[writeIndex] = input + filterStore * fb
            writeIndex = (writeIndex + 1) % maxDelaySamples

            // Output mix
            val output = (input * dry) + (delayed * wetScaled)
            buffer[i] = (output * 32767f).coerceIn(-32768f, 32767f).toInt().toShort()
        }
    }

    fun reset() {
        delayBuffer.fill(0f)
        writeIndex = 0
        filterStore = 0f
    }
}
