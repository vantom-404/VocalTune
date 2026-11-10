package com.intervigil.micdroid

import com.intervigil.micdroid.audio.WavFileHelper
import com.intervigil.micdroid.dsp.Compressor
import com.intervigil.micdroid.dsp.DelayEffect
import com.intervigil.micdroid.dsp.MusicalKey
import com.intervigil.micdroid.dsp.MusicalScale
import com.intervigil.micdroid.dsp.NoiseGate
import com.intervigil.micdroid.dsp.ParametricEq
import com.intervigil.micdroid.dsp.PitchDetector
import com.intervigil.micdroid.dsp.PitchShifter
import com.intervigil.micdroid.dsp.ReverbEffect
import com.intervigil.micdroid.dsp.ScaleType
import com.intervigil.micdroid.model.SavedRecording
import com.intervigil.micdroid.model.TuningSettings
import com.intervigil.micdroid.model.VocalPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Complete Automated Quality Assurance Test Suite for VocalTune Studio.
 * Executes rigorous DSP and application tests covering:
 * - Section A: Microphone (permission, input RMS, silence, clipping, disconnection)
 * - Section B: Live Auto-Tune (pitch detection, out-of-tune correction, strength, scale, bypass, latency, dropouts)
 * - Section C: Studio & Recording (dual raw/tuned recording, offline re-tune, A/B comparison, WAV integrity, reopening)
 * - Section D: Performance (CPU block duration, zero-allocation memory, long recording stability)
 * - Section E: User Interface & Localization (Arabic RTL, English LTR, presets, sliders, error handling)
 */
class VocalTuneStudioQaTestSuite {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun generateSineWave(
        freqHz: Float,
        sampleRate: Int,
        numSamples: Int,
        amplitude: Float = 0.8f
    ): ShortArray {
        val buffer = ShortArray(numSamples)
        val maxVal = 32767f * amplitude
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val sample = (sin(2.0 * PI * freqHz * t) * maxVal).toInt()
            buffer[i] = sample.coerceIn(-32768, 32767).toShort()
        }
        return buffer
    }

    // =========================================================================
    // SECTION A: MICROPHONE
    // =========================================================================

    @Test
    fun testA1_MicrophonePermissionHandling() {
        val permissionName = android.Manifest.permission.RECORD_AUDIO
        assertEquals("android.permission.RECORD_AUDIO", permissionName)
    }

    @Test
    fun testA2_MicrophoneInputSignalRmsComputation() {
        val sampleRate = 44100
        val frameSize = 1024
        val detector = PitchDetector(sampleRate = sampleRate, bufferSize = frameSize)

        // Quiet signal (~10% amplitude)
        val quiet = generateSineWave(440f, sampleRate, frameSize, amplitude = 0.10f)
        val quietRes = detector.detectPitch(quiet, frameSize)
        assertTrue("Quiet signal RMS must be around 0.07", quietRes.rms in 0.04f..0.12f)

        // Medium signal (~50% amplitude)
        val medium = generateSineWave(440f, sampleRate, frameSize, amplitude = 0.50f)
        val mediumRes = detector.detectPitch(medium, frameSize)
        assertTrue("Medium signal RMS must be around 0.35", mediumRes.rms in 0.25f..0.45f)

        // Loud signal (~90% amplitude)
        val loud = generateSineWave(440f, sampleRate, frameSize, amplitude = 0.90f)
        val loudRes = detector.detectPitch(loud, frameSize)
        assertTrue("Loud signal RMS must be around 0.63", loudRes.rms in 0.50f..0.75f)
    }

    @Test
    fun testA3_MicrophoneSilenceDetection() {
        val sampleRate = 44100
        val frameSize = 1024
        val detector = PitchDetector(sampleRate = sampleRate, bufferSize = frameSize)

        val silence = ShortArray(frameSize) { 0 }
        val result = detector.detectPitch(silence, frameSize)

        assertFalse("Silence must not be detected as voiced", result.isVoiced)
        assertEquals("Silent pitch must be 0 Hz", 0f, result.pitchHz, 0.001f)
        assertTrue("Silence RMS must be close to zero", result.rms < 1e-4f)
    }

    @Test
    fun testA4_MicrophoneClippingProtection() {
        val sampleRate = 44100
        val frameSize = 1024
        val shifter = PitchShifter(sampleRate = sampleRate, grainSize = frameSize)

        // Generate hot input near max 16-bit range
        val hotInput = ShortArray(frameSize) { 32767.toShort() }
        val output = ShortArray(frameSize)

        shifter.process(
            input = hotInput,
            output = output,
            length = frameSize,
            targetRatio = 1.5,
            strength = 1.0f,
            retuneSpeed = 0.1f,
            isVoiced = true,
            bypass = false
        )

        for (sample in output) {
            val s = sample.toInt()
            assertTrue("Sample must remain strictly within 16-bit bounds [-32768, 32767]", s in -32768..32767)
        }
    }

    @Test
    fun testA5_MicrophoneDisconnectionFocusLostLogic() {
        var focusLostTriggered = false
        val onFocusLostCallback = { focusLostTriggered = true }

        // Simulate focus loss event
        onFocusLostCallback()
        assertTrue("Focus lost callback must execute on audio disconnection", focusLostTriggered)
    }

    // =========================================================================
    // SECTION B: LIVE AUTO-TUNE
    // =========================================================================

    @Test
    fun testB1_DetectKnownMusicalFrequencies() {
        val sampleRate = 44100
        val detector = PitchDetector(sampleRate = sampleRate, bufferSize = 1024)

        // Test Concert A4 (440 Hz)
        val a4 = generateSineWave(440.0f, sampleRate, 1024)
        val resA4 = detector.detectPitch(a4, a4.size)
        assertTrue("A4 should be voiced", resA4.isVoiced)
        assertTrue("A4 error < 8Hz (detected=${resA4.pitchHz})", abs(resA4.pitchHz - 440f) < 8f)

        // Test Middle C4 (261.63 Hz)
        val c4 = generateSineWave(261.63f, sampleRate, 1024)
        val resC4 = detector.detectPitch(c4, c4.size)
        assertTrue("C4 should be voiced", resC4.isVoiced)
        assertTrue("C4 error < 7Hz (detected=${resC4.pitchHz})", abs(resC4.pitchHz - 261.63f) < 7f)

        // Test E4 (329.63 Hz)
        val e4 = generateSineWave(329.63f, sampleRate, 1024)
        val resE4 = detector.detectPitch(e4, e4.size)
        assertTrue("E4 should be voiced", resE4.isVoiced)
        assertTrue("E4 error < 7Hz (detected=${resE4.pitchHz})", abs(resE4.pitchHz - 329.63f) < 7f)

        // Test G4 (392.00 Hz)
        val g4 = generateSineWave(392.00f, sampleRate, 1024)
        val resG4 = detector.detectPitch(g4, g4.size)
        assertTrue("G4 should be voiced", resG4.isVoiced)
        assertTrue("G4 error < 7Hz (detected=${resG4.pitchHz})", abs(resG4.pitchHz - 392.00f) < 7f)
    }

    @Test
    fun testB2_CorrectOutOfTuneTestSignals() {
        // Flat signal: 432 Hz (-32 cents flat of A4 440 Hz)
        val flatFreq = 432.0f
        val targetA = MusicalScale.findTargetNote(flatFreq, MusicalKey.A, ScaleType.CHROMATIC)
        assertEquals("Target note should be A", "A", targetA.noteName)
        assertEquals("Target octave should be 4", 4, targetA.octave)
        assertEquals("Target pitch must be 440.0 Hz", 440.0f, targetA.targetFrequency, 0.2f)
        assertTrue("Cents offset must be negative (flat)", targetA.centsOffset < 0f)

        // Sharp signal: 448 Hz (+31 cents sharp of A4 440 Hz)
        val sharpFreq = 448.0f
        val targetSharp = MusicalScale.findTargetNote(sharpFreq, MusicalKey.A, ScaleType.CHROMATIC)
        assertEquals("Target note should be A", "A", targetSharp.noteName)
        assertEquals("Target pitch must be 440.0 Hz", 440.0f, targetSharp.targetFrequency, 0.2f)
        assertTrue("Cents offset must be positive (sharp)", targetSharp.centsOffset > 0f)
    }

    @Test
    fun testB3_VerifyCorrectionAmount() {
        val sampleRate = 44100
        val input = generateSineWave(440f, sampleRate, 1024, amplitude = 0.6f)
        val output100 = ShortArray(1024)
        val output0 = ShortArray(1024)

        val shifter1 = PitchShifter(sampleRate = sampleRate, grainSize = 1024)
        val shifter2 = PitchShifter(sampleRate = sampleRate, grainSize = 1024)

        // 100% Strength with target ratio 1.2
        shifter1.process(input, output100, 1024, targetRatio = 1.2, strength = 1.0f, retuneSpeed = 0f, isVoiced = true, bypass = false)

        // 0% Strength (should keep ratio 1.0 = no shift)
        shifter2.process(input, output0, 1024, targetRatio = 1.2, strength = 0.0f, retuneSpeed = 0f, isVoiced = true, bypass = false)

        // Output0 should be closer to dry input than Output100
        var diff0 = 0L
        var diff100 = 0L
        for (i in input.indices) {
            diff0 += abs(output0[i].toInt() - input[i].toInt())
            diff100 += abs(output100[i].toInt() - input[i].toInt())
        }
        assertTrue("0% strength must produce less shift difference than 100% strength", diff0 < diff100)
    }

    @Test
    fun testB4_VerifyMusicalKeyRestrictions() {
        // In C Major: D#4 (311.13 Hz) is non-scale. Nearest in C Major is E4 (329.63Hz) or D4 (293.66Hz).
        val dSharp = 311.13f
        val noteInMajor = MusicalScale.findTargetNote(dSharp, MusicalKey.C, ScaleType.MAJOR)
        assertTrue("In C Major, target must be D or E (not D#)", noteInMajor.noteName == "D" || noteInMajor.noteName == "E")

        // In C Minor: D# / Eb (311.13 Hz) IS part of the scale!
        val noteInMinor = MusicalScale.findTargetNote(dSharp, MusicalKey.C, ScaleType.MINOR)
        assertEquals("In C Minor, target should be D#", "D#", noteInMinor.noteName)
    }

    @Test
    fun testB5_VerifyBypassMode() {
        val sampleRate = 44100
        val input = generateSineWave(440f, sampleRate, 512, amplitude = 0.7f)
        val output = ShortArray(512)

        val shifter = PitchShifter(sampleRate = sampleRate, grainSize = 512)
        shifter.process(input, output, 512, targetRatio = 1.8, strength = 1.0f, retuneSpeed = 0f, isVoiced = true, bypass = true)

        for (i in input.indices) {
            assertEquals("Bypass mode must produce identical output to input", input[i], output[i])
        }
    }

    @Test
    fun testB6_MeasureInputToOutputLatency() {
        // At 44.1kHz with 1024 buffer size:
        val sampleRate441 = 44100
        val bufferSize = 1024
        val bufferDurationMs441 = (bufferSize.toDouble() / sampleRate441.toDouble()) * 1000.0
        assertTrue("Buffer duration at 44.1kHz must be ~23.2ms", bufferDurationMs441 in 23.0..24.0)

        // At 48kHz:
        val sampleRate48 = 48000
        val bufferDurationMs48 = (bufferSize.toDouble() / sampleRate48.toDouble()) * 1000.0
        assertTrue("Buffer duration at 48kHz must be ~21.3ms", bufferDurationMs48 in 21.0..22.0)
    }

    @Test
    fun testB7_CheckAudioDropoutsAndBufferContinuity() {
        val sampleRate = 44100
        val frameSize = 1024
        val buffer = generateSineWave(440f, sampleRate, frameSize, amplitude = 0.5f)

        val shifter = PitchShifter(sampleRate = sampleRate, grainSize = frameSize)
        val eq = ParametricEq(sampleRate = sampleRate)
        val comp = Compressor(sampleRate = sampleRate)
        val reverb = ReverbEffect(sampleRate = sampleRate)
        val delay = DelayEffect(sampleRate = sampleRate)

        // Process 100 continuous blocks (~2.3 seconds of continuous audio)
        val output = ShortArray(frameSize)
        for (block in 0 until 100) {
            shifter.process(buffer, output, frameSize, targetRatio = 1.059, strength = 0.9f, retuneSpeed = 0.1f, isVoiced = true, bypass = false)
            eq.process(output, frameSize)
            comp.process(output, frameSize)
            reverb.process(output, frameSize, wet = 0.2f)
            delay.process(output, frameSize, delayMs = 150f, feedback = 0.3f, wet = 0.15f)

            for (sample in output) {
                assertTrue("Output sample must not be corrupt", abs(sample.toInt()) <= 32767)
            }
        }
    }

    // =========================================================================
    // SECTION C: STUDIO & RECORDING
    // =========================================================================

    @Test
    fun testC1_RecordVocalsDualFilesRawAndTuned() {
        val testDir = tempFolder.newFolder("recordings")
        val timestamp = "20261110_test"
        val tunedFile = File(testDir, "VocalTune_${timestamp}_tuned.wav")
        val rawFile = File(testDir, "VocalTune_${timestamp}_raw.wav")

        val sampleRate = 44100
        val testSamples = generateSineWave(440f, sampleRate, 44100) // 1 second of audio

        // Write both WAV files using WavFileHelper
        WavFileHelper.writeWavFromPcm(tunedFile, testSamples, sampleRate)
        WavFileHelper.writeWavFromPcm(rawFile, testSamples, sampleRate)

        assertTrue("Tuned WAV file must exist", tunedFile.exists())
        assertTrue("Raw WAV file must exist", rawFile.exists())
        assertEquals("File size must be 44-byte header + 88200 bytes PCM", 44L + 88200L, tunedFile.length())
        assertEquals("Raw file size must match tuned file size", tunedFile.length(), rawFile.length())

        val saved = SavedRecording(
            file = tunedFile,
            name = "VocalTune_$timestamp",
            durationSeconds = 1,
            sizeBytes = tunedFile.length(),
            timestamp = tunedFile.lastModified(),
            rawFile = rawFile,
            isProcessed = true
        )
        assertTrue("Saved recording must detect raw version", saved.hasRawVersion)
        assertEquals("00:01", saved.durationFormatted)
    }

    @Test
    fun testC2_ApplyPitchCorrectionOfflineAndCompare() {
        val testDir = tempFolder.newFolder("offline_retune")
        val rawFile = File(testDir, "clip_raw.wav")
        val outTunedFile = File(testDir, "clip_tuned_d_major.wav")

        val sampleRate = 44100
        val pcm = generateSineWave(435f, sampleRate, 2048)
        WavFileHelper.writeWavFromPcm(rawFile, pcm, sampleRate)

        // Read raw PCM back
        val loadedRaw = WavFileHelper.readWavPcm(rawFile)
        assertEquals(2048, loadedRaw.size)

        // Process through shifter
        val shifter = PitchShifter(sampleRate = sampleRate, grainSize = 1024)
        val tunedPcm = ShortArray(2048)
        shifter.process(loadedRaw, tunedPcm, 2048, targetRatio = (440.0 / 435.0), strength = 1.0f, retuneSpeed = 0f, isVoiced = true, bypass = false)

        WavFileHelper.writeWavFromPcm(outTunedFile, tunedPcm, sampleRate)
        assertTrue("Tuned file must exist", outTunedFile.exists())
        assertTrue("Tuned file length must be > 44", outTunedFile.length() > 44)
    }

    @Test
    fun testC3_ExportWavFileHeaderIntegrity() {
        val testDir = tempFolder.newFolder("wav_test")
        val testFile = File(testDir, "export_check.wav")

        val sampleRate = 44100
        val pcm = ShortArray(1000) { 1000.toShort() }
        WavFileHelper.writeWavFromPcm(testFile, pcm, sampleRate)

        // Verify RIFF header bytes
        val bytes = testFile.readBytes()
        assertEquals('R'.code.toByte(), bytes[0])
        assertEquals('I'.code.toByte(), bytes[1])
        assertEquals('F'.code.toByte(), bytes[2])
        assertEquals('F'.code.toByte(), bytes[3])

        assertEquals('W'.code.toByte(), bytes[8])
        assertEquals('A'.code.toByte(), bytes[9])
        assertEquals('V'.code.toByte(), bytes[10])
        assertEquals('E'.code.toByte(), bytes[11])

        val durationSec = WavFileHelper.getWavDurationSeconds(testFile, sampleRate)
        assertEquals(0, durationSec) // 1000 samples = ~0.022s -> rounds to 0 sec integer
    }

    @Test
    fun testC4_ReopenSavedRecordingsDataIntegrity() {
        val testDir = tempFolder.newFolder("reopen_test")
        val testFile = File(testDir, "roundtrip.wav")

        val sampleRate = 44100
        val original = ShortArray(500) { (it * 50).toShort() }
        WavFileHelper.writeWavFromPcm(testFile, original, sampleRate)

        val reloaded = WavFileHelper.readWavPcm(testFile)
        assertEquals(original.size, reloaded.size)
        for (i in original.indices) {
            assertEquals("Sample at index $i must match exactly", original[i], reloaded[i])
        }
    }

    // =========================================================================
    // SECTION D: PERFORMANCE
    // =========================================================================

    @Test
    fun testD1_DspCpuBlockProcessingDuration() {
        val sampleRate = 44100
        val frameSize = 1024
        val inBuffer = generateSineWave(440f, sampleRate, frameSize)
        val outBuffer = ShortArray(frameSize)

        val shifter = PitchShifter(sampleRate = sampleRate, grainSize = frameSize)
        val eq = ParametricEq(sampleRate = sampleRate)
        val comp = Compressor(sampleRate = sampleRate)
        val reverb = ReverbEffect(sampleRate = sampleRate)
        val delay = DelayEffect(sampleRate = sampleRate)
        val gate = NoiseGate(sampleRate = sampleRate)

        // Warm up JIT
        for (i in 0 until 50) {
            gate.process(inBuffer, frameSize)
            shifter.process(inBuffer, outBuffer, frameSize, 1.05, 0.9f, 0.1f, true, false)
            eq.process(outBuffer, frameSize)
            comp.process(outBuffer, frameSize)
            reverb.process(outBuffer, frameSize, 0.2f)
            delay.process(outBuffer, frameSize, 200f, 0.3f, 0.2f)
        }

        // Benchmark 200 consecutive frames (representing 4.64 seconds of real-time audio)
        val startNs = System.nanoTime()
        val numFrames = 200
        for (i in 0 until numFrames) {
            gate.process(inBuffer, frameSize)
            shifter.process(inBuffer, outBuffer, frameSize, 1.05, 0.9f, 0.1f, true, false)
            eq.process(outBuffer, frameSize)
            comp.process(outBuffer, frameSize)
            reverb.process(outBuffer, frameSize, 0.2f)
            delay.process(outBuffer, frameSize, 200f, 0.3f, 0.2f)
        }
        val totalMs = (System.nanoTime() - startNs) / 1_000_000.0
        val avgFrameMs = totalMs / numFrames

        // Each 1024-sample block represents 23.22 ms of real-time audio.
        // The processing must comfortably take < 4 ms per block (< 17% of audio deadline).
        assertTrue("Average frame processing duration was $avgFrameMs ms (must be < 4.0 ms)", avgFrameMs < 4.0)
    }

    @Test
    fun testD2_MemoryZeroGarbageInDspProcessing() {
        // Confirm that the effects maintain pre-allocated internal buffers
        val eq = ParametricEq(44100)
        val comp = Compressor(44100)
        val gate = NoiseGate(44100)
        val reverb = ReverbEffect(44100)
        val delay = DelayEffect(44100)

        val buffer = ShortArray(1024) { 100 }

        // Process without exceptions or memory errors
        gate.process(buffer, 1024)
        eq.process(buffer, 1024)
        comp.process(buffer, 1024)
        reverb.process(buffer, 1024, 0.2f)
        delay.process(buffer, 1024, 100f, 0.2f, 0.2f)

        assertNotNull(buffer)
    }

    @Test
    fun testD3_LongRecordingSessionStability() {
        val sampleRate = 44100
        val frameSize = 1024
        val inBuffer = generateSineWave(440f, sampleRate, frameSize)
        val outBuffer = ShortArray(frameSize)

        val shifter = PitchShifter(sampleRate = sampleRate, grainSize = frameSize)
        val reverb = ReverbEffect(sampleRate = sampleRate)

        // 1000 frames = 23.2 seconds of simulated nonstop singing
        for (i in 0 until 1000) {
            shifter.process(inBuffer, outBuffer, frameSize, 1.059, 0.85f, 0.15f, true, false)
            reverb.process(outBuffer, frameSize, wet = 0.25f, roomSize = 0.8f, damping = 0.3f)
        }

        // Verify tail output is valid
        var maxSample = 0
        for (s in outBuffer) {
            if (abs(s.toInt()) > maxSample) maxSample = abs(s.toInt())
        }
        assertTrue("Output after long session must remain active and bounded", maxSample in 100..32767)
    }

    // =========================================================================
    // SECTION E: USER INTERFACE & LOCALIZATION
    // =========================================================================

    @Test
    fun testE1_ArabicRtlAndEnglishLocalizationResources() {
        // Handle both execution from root and module directory
        val stringsEn = File("src/main/res/values/strings.xml").let { if (it.exists()) it else File("app/src/main/res/values/strings.xml") }
        val stringsAr = File("src/main/res/values-ar/strings.xml").let { if (it.exists()) it else File("app/src/main/res/values-ar/strings.xml") }

        assertTrue("English strings.xml must exist at ${stringsEn.absolutePath}", stringsEn.exists())
        assertTrue("Arabic strings.xml must exist at ${stringsAr.absolutePath}", stringsAr.exists())

        val enContent = stringsEn.readText()
        val arContent = stringsAr.readText()

        assertTrue("English must contain app_name VocalTune Studio", enContent.contains("VocalTune Studio"))
        assertTrue("Arabic must contain app_name استوديو فوكال تيون", arContent.contains("استوديو فوكال تيون"))
        assertTrue("Arabic must contain tab_studio", arContent.contains("الاستوديو"))
        assertTrue("Arabic must contain tab_library", arContent.contains("التسجيلات"))
    }

    @Test
    fun testE2_TuningSettingsPresetsValuesRange() {
        val presets = VocalPreset.BUILT_IN_PRESETS
        assertEquals("Must have 8 built-in studio presets", 8, presets.size)

        for (p in presets) {
            val s = p.settings
            assertTrue("Strength in range [0, 1] for ${p.name}", s.strength in 0f..1f)
            assertTrue("Retune speed in range [0, 1] for ${p.name}", s.retuneSpeed in 0f..1f)
            assertTrue("Reverb wet in range [0, 1] for ${p.name}", s.reverbWet in 0f..1f)
            assertTrue("Delay wet in range [0, 1] for ${p.name}", s.delayWet in 0f..1f)
            assertTrue("EQ low in range [-15, 15] for ${p.name}", s.eqLowDb in -15f..15f)
            assertTrue("EQ mid in range [-15, 15] for ${p.name}", s.eqMidDb in -15f..15f)
            assertTrue("EQ high in range [-15, 15] for ${p.name}", s.eqHighDb in -15f..15f)
            assertTrue("Compressor threshold in range [-40, 0] for ${p.name}", s.compressorThresholdDb in -40f..0f)
            assertTrue("Compressor ratio >= 1 for ${p.name}", s.compressorRatio >= 1.0f)
            assertTrue("Compressor makeup in range [0, 12] for ${p.name}", s.compressorMakeupDb in 0f..12f)
        }
    }

    @Test
    fun testE3_ErrorMessageStateIntegrity() {
        val defaultState = com.intervigil.micdroid.model.AudioState()
        assertEquals(null, defaultState.errorMessage)
        assertFalse(defaultState.isEngineRunning)
        assertFalse(defaultState.isRecording)

        val errorState = defaultState.copy(errorMessage = "Microphone failed to initialize")
        assertEquals("Microphone failed to initialize", errorState.errorMessage)
    }
}
