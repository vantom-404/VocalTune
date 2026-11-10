package com.intervigil.micdroid.dsp

/**
 * Studio Schroeder Reverb Processor with 4 parallel low-pass feedback comb filters
 * and 2 series all-pass diffusers.
 * Supports configurable decay time / room size, damping, and dry/wet mix.
 */
class ReverbEffect(sampleRate: Int = 44100) {

    // Delay lengths tuned for 44.1kHz (prime numbers to avoid flutter echoes)
    private val combLengths = intArrayOf(
        (1557 * sampleRate) / 44100,
        (1617 * sampleRate) / 44100,
        (1491 * sampleRate) / 44100,
        (1422 * sampleRate) / 44100
    )
    private val allpassLengths = intArrayOf(
        (225 * sampleRate) / 44100,
        (556 * sampleRate) / 44100
    )

    private class LowPassCombFilter(size: Int) {
        val buffer = FloatArray(size)
        var index = 0
        var filterStore = 0f

        fun process(input: Float, feedback: Float, damp: Float): Float {
            val output = buffer[index]
            filterStore = (output * (1.0f - damp)) + (filterStore * damp)
            buffer[index] = input + filterStore * feedback
            index = (index + 1) % buffer.size
            return output
        }

        fun reset() {
            buffer.fill(0f)
            index = 0
            filterStore = 0f
        }
    }

    private class AllpassFilter(size: Int, val feedback: Float = 0.5f) {
        val buffer = FloatArray(size)
        var index = 0

        fun process(input: Float): Float {
            val bufOut = buffer[index]
            val output = -input + bufOut
            buffer[index] = input + bufOut * feedback
            index = (index + 1) % buffer.size
            return output
        }

        fun reset() {
            buffer.fill(0f)
            index = 0
        }
    }

    private val combFilters = Array(4) { i ->
        LowPassCombFilter(combLengths[i])
    }

    private val allpassFilters = Array(2) { i ->
        AllpassFilter(allpassLengths[i], 0.5f)
    }

    /**
     * Process audio in-place.
     * @param buffer 16-bit PCM buffer to process
     * @param length Number of samples
     * @param wet Wet mix [0.0f .. 1.0f]
     * @param roomSize Decay duration / room size [0.0f .. 1.0f] (default 0.75f)
     * @param damping High frequency damping [0.0f .. 1.0f] (default 0.25f)
     */
    fun process(
        buffer: ShortArray,
        length: Int,
        wet: Float,
        roomSize: Float = 0.75f,
        damping: Float = 0.25f
    ) {
        if (wet <= 0.005f) return

        val feedback = 0.70f + (roomSize.coerceIn(0.1f, 0.98f) * 0.25f)
        val damp = damping.coerceIn(0.0f, 0.8f)

        val dry = 1.0f - wet * 0.4f
        val wetScaled = wet * 0.5f

        for (i in 0 until length) {
            val input = buffer[i] / 32768.0f

            // Parallel Low-Pass Comb filters
            var combOut = 0f
            for (cf in combFilters) {
                combOut += cf.process(input, feedback, damp)
            }
            combOut *= 0.25f

            // Series Allpass filters for stereo-like diffusion
            var diffused = combOut
            for (ap in allpassFilters) {
                diffused = ap.process(diffused)
            }

            // Mix dry and wet
            val output = (input * dry) + (diffused * wetScaled)
            buffer[i] = (output * 32767f).coerceIn(-32768f, 32767f).toInt().toShort()
        }
    }

    fun reset() {
        combFilters.forEach { it.reset() }
        allpassFilters.forEach { it.reset() }
    }
}
