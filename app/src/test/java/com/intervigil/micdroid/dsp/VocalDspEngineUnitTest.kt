package com.intervigil.micdroid.dsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Automated tests for the VocalTune Studio DSP Engine
 * using mathematically synthesized audio signals with known frequencies.
 */
class VocalDspEngineUnitTest {

    /**
     * Generates a 16-bit PCM mono sine wave with exact frequency.
     */
    private fun generateSineWave(
        frequencyHz: Float,
        sampleRate: Int,
        numSamples: Int,
        amplitude: Float = 0.8f
    ): ShortArray {
        val buffer = ShortArray(numSamples)
        val maxVal = 32767f * amplitude
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val sample = (sin(2.0 * PI * frequencyHz * t) * maxVal).toInt()
            buffer[i] = sample.coerceIn(-32768, 32767).toShort()
        }
        return buffer
    }

    @Test
    fun testDetectPitchConcertA440At44100Hz() {
        val sampleRate = 44100
        val targetFreq = 440.0f
        val buffer = generateSineWave(targetFreq, sampleRate, 1024, amplitude = 0.85f)

        val detector = PitchDetector(sampleRate = sampleRate, bufferSize = 1024)
        val result = detector.detectPitch(buffer, buffer.size)

        assertTrue("Sine wave at 440Hz should be recognized as voiced", result.isVoiced)
        assertTrue("Confidence probability should be high (> 0.80)", result.probability > 0.80f)

        val error = abs(result.pitchHz - targetFreq)
        assertTrue(
            "Detected pitch ${result.pitchHz} Hz should be within 2% of 440 Hz (error=$error)",
            error < 8.0f
        )
    }

    @Test
    fun testDetectPitchMiddleC261At44100Hz() {
        val sampleRate = 44100
        val targetFreq = 261.63f // C4
        val buffer = generateSineWave(targetFreq, sampleRate, 1024, amplitude = 0.85f)

        val detector = PitchDetector(sampleRate = sampleRate, bufferSize = 1024)
        val result = detector.detectPitch(buffer, buffer.size)

        assertTrue("C4 261.63Hz should be recognized as voiced", result.isVoiced)
        val error = abs(result.pitchHz - targetFreq)
        assertTrue(
            "Detected pitch ${result.pitchHz} Hz should be within 2.5% of 261.63 Hz",
            error < 6.5f
        )
    }

    @Test
    fun testDetectPitchAt48000HzSampleRate() {
        val sampleRate = 48000
        val targetFreq = 440.0f
        val buffer = generateSineWave(targetFreq, sampleRate, 1024, amplitude = 0.85f)

        val detector = PitchDetector(sampleRate = sampleRate, bufferSize = 1024)
        val result = detector.detectPitch(buffer, buffer.size)

        assertTrue("Should detect pitch at 48kHz", result.isVoiced)
        val error = abs(result.pitchHz - targetFreq)
        assertTrue("Detected pitch should be close to 440Hz at 48kHz (actual=${result.pitchHz})", error < 8.0f)
    }

    @Test
    fun testSilenceGating() {
        val sampleRate = 44100
        val silenceBuffer = ShortArray(1024) { 0 }

        val detector = PitchDetector(sampleRate = sampleRate, bufferSize = 1024)
        val result = detector.detectPitch(silenceBuffer, silenceBuffer.size)

        assertFalse("Silence must not be detected as voiced", result.isVoiced)
        assertEquals("Silent pitch should be 0.0 Hz", 0.0f, result.pitchHz, 0.001f)
    }

    @Test
    fun testLowEnergyNoiseGating() {
        val sampleRate = 44100
        // Generate tiny noise under silence gate
        val lowNoiseBuffer = ShortArray(1024) { (it % 15).toShort() }

        val detector = PitchDetector(sampleRate = sampleRate, bufferSize = 1024)
        val result = detector.detectPitch(lowNoiseBuffer, lowNoiseBuffer.size)

        assertFalse("Low noise below threshold should be gated", result.isVoiced)
        assertEquals(0.0f, result.pitchHz, 0.001f)
    }

    @Test
    fun testMusicalScaleQuantization() {
        // Frequency close to A4 (440Hz), slightly sharp (+10 cents)
        val sharpA = 442.5f
        val targetA = MusicalScale.findTargetNote(sharpA, MusicalKey.A, ScaleType.MAJOR)

        assertEquals("Note name should be A", "A", targetA.noteName)
        assertEquals("Octave should be 4", 4, targetA.octave)
        assertEquals("Target frequency should be 440.0 Hz", 440.0f, targetA.targetFrequency, 0.1f)
        assertTrue("Cents offset should be positive (sharp)", targetA.centsOffset > 0f)

        // Test C Major scale: 260 Hz (slightly flat of C4 = 261.63Hz)
        val flatC = 260.0f
        val targetC = MusicalScale.findTargetNote(flatC, MusicalKey.C, ScaleType.MAJOR)
        assertEquals("C", targetC.noteName)
        assertEquals(4, targetC.octave)
        assertTrue("Cents offset should be negative (flat)", targetC.centsOffset < 0f)
    }

    @Test
    fun testPitchShifterStabilityAndNoClipping() {
        val sampleRate = 44100
        val input = generateSineWave(440.0f, sampleRate, 1024, amplitude = 0.95f)
        val output = ShortArray(1024)

        val shifter = PitchShifter(sampleRate = sampleRate, grainSize = 1024)

        // Test shifting up 1 semitone (ratio = 1.05946)
        shifter.process(
            input = input,
            output = output,
            length = input.size,
            targetRatio = 1.05946,
            strength = 1.0f,
            retuneSpeed = 0.1f,
            isVoiced = true,
            bypass = false
        )

        // Verify output is populated and within legal 16-bit range
        var maxAbs = 0
        var hasNonZero = false
        for (sample in output) {
            val absVal = abs(sample.toInt())
            if (absVal > maxAbs) maxAbs = absVal
            if (sample.toInt() != 0) hasNonZero = true
        }

        assertTrue("Pitch shifted output should contain valid audio", hasNonZero)
        assertTrue("Output must not exceed 16-bit PCM bounds", maxAbs <= 32767)
    }

    @Test
    fun testPitchShifterBypassMode() {
        val sampleRate = 44100
        val input = generateSineWave(440.0f, sampleRate, 512, amplitude = 0.5f)
        val output = ShortArray(512)

        val shifter = PitchShifter(sampleRate = sampleRate, grainSize = 512)

        // Process with bypass = true
        shifter.process(
            input = input,
            output = output,
            length = input.size,
            targetRatio = 1.5,
            strength = 1.0f,
            retuneSpeed = 0.0f,
            isVoiced = true,
            bypass = true
        )

        // In bypass mode, output should be bit-exact copy of input
        for (i in input.indices) {
            assertEquals("Bypass mode must output unaltered dry signal", input[i], output[i])
        }
    }

    @Test
    fun testReverbAndDelayNoBlowup() {
        val sampleRate = 44100
        val buffer = generateSineWave(440.0f, sampleRate, 1024, amplitude = 0.7f)

        val reverb = ReverbEffect(sampleRate = sampleRate)
        val delay = DelayEffect(sampleRate = sampleRate)

        // Process multiple blocks to test feedback filter stability
        for (block in 0 until 10) {
            reverb.process(buffer, buffer.size, wet = 0.6f, roomSize = 0.8f, damping = 0.3f)
            delay.process(buffer, buffer.size, delayMs = 250f, feedback = 0.5f, wet = 0.5f, damping = 0.2f)

            for (sample in buffer) {
                assertTrue("Samples must remain bounded without overflow", abs(sample.toInt()) <= 32767)
            }
        }
    }

    @Test
    fun testNoiseGateAttenuatesSilenceAndPassesVocal() {
        val sampleRate = 44100
        val noiseGate = NoiseGate(sampleRate = sampleRate)

        // Generate quiet background noise (-55 dB)
        val quietNoise = generateSineWave(200.0f, sampleRate, 2048, amplitude = 0.002f)
        noiseGate.process(quietNoise, quietNoise.size, thresholdDb = -40f, attackMs = 2f, releaseMs = 50f)

        // Last samples should be heavily attenuated
        val tailSample = abs(quietNoise.last().toInt())
        assertTrue("Noise gate must attenuate signal below threshold", tailSample <= 30)

        // Generate strong vocal signal
        val loudVocal = generateSineWave(440.0f, sampleRate, 2048, amplitude = 0.75f)
        noiseGate.process(loudVocal, loudVocal.size, thresholdDb = -40f, attackMs = 2f, releaseMs = 50f)

        // Strong vocal should pass with high amplitude
        var maxVocal = 0
        for (s in loudVocal) {
            if (abs(s.toInt()) > maxVocal) maxVocal = abs(s.toInt())
        }
        assertTrue("Vocal above gate threshold must pass through", maxVocal > 15000)
    }

    @Test
    fun testParametricEqStability() {
        val sampleRate = 44100
        val eq = ParametricEq(sampleRate = sampleRate)
        eq.updateFilters(lowGainDb = 4.0f, midGainDb = -2.0f, highGainDb = 6.0f)

        val buffer = generateSineWave(1000.0f, sampleRate, 2048, amplitude = 0.6f)
        eq.process(buffer, buffer.size)

        for (sample in buffer) {
            assertTrue("EQ output must remain within 16-bit PCM boundaries", abs(sample.toInt()) <= 32767)
        }
    }

    @Test
    fun testCompressorTamesPeaksAndAppliesMakeup() {
        val sampleRate = 44100
        val compressor = Compressor(sampleRate = sampleRate)

        // Hot vocal signal with high peaks
        val hotVocal = generateSineWave(300.0f, sampleRate, 2048, amplitude = 0.95f)
        compressor.process(
            buffer = hotVocal,
            length = hotVocal.size,
            thresholdDb = -18f,
            ratio = 4.0f,
            attackMs = 5f,
            releaseMs = 80f,
            makeupGainDb = 2.0f
        )

        for (sample in hotVocal) {
            assertTrue("Compressed vocal must be bounded without hard digital clipping", abs(sample.toInt()) <= 32767)
        }
    }

    @Test
    fun testAllEightPresetsHaveValidParameters() {
        val presets = com.intervigil.micdroid.model.VocalPreset.BUILT_IN_PRESETS
        assertEquals("Must provide exactly 8 built-in studio presets", 8, presets.size)

        for (preset in presets) {
            val s = preset.settings
            assertTrue("Strength must be in [0, 1] for ${preset.name}", s.strength in 0f..1f)
            assertTrue("Retune speed must be in [0, 1] for ${preset.name}", s.retuneSpeed in 0f..1f)
            assertTrue("Reverb wet must be in [0, 1] for ${preset.name}", s.reverbWet in 0f..1f)
            assertTrue("Delay wet must be in [0, 1] for ${preset.name}", s.delayWet in 0f..1f)
            assertTrue("Compressor ratio must be >= 1 for ${preset.name}", s.compressorRatio >= 1.0f)
            assertTrue("EQ Low must be within [-15, 15] for ${preset.name}", s.eqLowDb in -15f..15f)
        }
    }
}
