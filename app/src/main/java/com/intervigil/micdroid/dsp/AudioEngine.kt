package com.intervigil.micdroid.dsp

import com.intervigil.micdroid.model.TuningSettings
import java.io.File

data class DualRecording(
    val tunedFile: File,
    val rawFile: File?
)

/**
 * Standard AudioEngine interface defining the contract for real-time
 * vocal DSP capture, pitch detection, pitch correction, effect chain, and recording.
 */
interface AudioEngine {
    /**
     * Initializes and starts audio capture from the microphone.
     */
    fun startInput(): Boolean

    /**
     * Initializes and starts audio playback to headphones/speaker.
     */
    fun startOutput(): Boolean

    /**
     * Stops active audio input and output streams without releasing hardware resources.
     */
    fun stop()

    /**
     * Stops processing and fully releases microphone and playback audio tracks.
     */
    fun release()

    /**
     * Sets the tonic root key for auto-tune snapping (C, C#, D, D#, ... B).
     */
    fun setKey(key: MusicalKey)

    /**
     * Sets the musical scale type (Major, Minor, Pentatonic, Chromatic).
     */
    fun setScale(scale: ScaleType)

    /**
     * Sets the pitch correction amount / strength between 0.0 (dry) and 1.0 (hard snap).
     */
    fun setCorrectionAmount(amount: Float)

    /**
     * Sets retune speed between 0.0 (fast / robotic snap) and 1.0 (smooth / natural).
     */
    fun setRetuneSpeed(speed: Float)

    /**
     * Enables or disables pitch correction bypass mode (hear original dry audio).
     */
    fun setBypass(bypass: Boolean)

    /**
     * Returns the current detected fundamental frequency in Hz (0.0 if unvoiced/silent).
     */
    fun getDetectedPitch(): Float

    /**
     * Returns the detected musical note name (e.g. "C4", "A#3", or "--" if unvoiced).
     */
    fun getDetectedNote(): String

    /**
     * Returns the current normalized input audio RMS level [0.0f .. 1.0f].
     */
    fun getInputLevel(): Float

    // Engine lifecycle and auxiliary controls
    fun isRunning(): Boolean
    fun isRecording(): Boolean
    fun isRecordingPaused(): Boolean
    fun startRecording(): Boolean
    fun pauseRecording()
    fun resumeRecording()
    fun stopRecording(): DualRecording?
    fun setLiveMonitoring(enabled: Boolean)
    fun setSampleRate(sampleRate: Int)
    fun setEffects(reverbWet: Float, delayWet: Float, delayTimeMs: Float, delayFeedback: Float)

    /**
     * Offline batch processing to re-tune an existing raw vocal file with new settings.
     */
    fun processWavFile(
        rawInputFile: File,
        outputFile: File,
        settings: TuningSettings,
        onProgress: (Float) -> Unit = {}
    ): Boolean
}
