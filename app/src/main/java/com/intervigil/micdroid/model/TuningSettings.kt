package com.intervigil.micdroid.model

import com.intervigil.micdroid.dsp.MusicalKey
import com.intervigil.micdroid.dsp.ScaleType

data class TuningSettings(
    val strength: Float = 0.85f,             // 0.0 to 1.0 (85% default Auto-tune)
    val key: MusicalKey = MusicalKey.C,
    val scale: ScaleType = ScaleType.MAJOR,
    val retuneSpeed: Float = 0.15f,          // 0.0 (fast/robotic) to 1.0 (natural)
    val isBypass: Boolean = false,           // Bypass mode (dry vocal)
    val isLiveMonitoring: Boolean = true,     // Hear live in headphones/speaker

    // Noise Gate
    val noiseGateEnabled: Boolean = true,
    val noiseGateThresholdDb: Float = -45f,   // -60 dB to -20 dB

    // Parametric EQ (3-Band)
    val eqLowDb: Float = 0.0f,               // -12 dB to +12 dB (Bass warmth / Rumble cut)
    val eqMidDb: Float = 0.0f,               // -12 dB to +12 dB (Presence / Body)
    val eqHighDb: Float = 0.0f,              // -12 dB to +12 dB (Air / Brightness)

    // Dynamic Range Compressor
    val compressorEnabled: Boolean = true,
    val compressorThresholdDb: Float = -20f, // -40 dB to 0 dB
    val compressorRatio: Float = 3.0f,       // 1.0 to 10.0
    val compressorMakeupDb: Float = 2.0f,    // 0 dB to 12 dB

    // Reverb
    val reverbWet: Float = 0.20f,            // 0.0 to 1.0
    val reverbRoomSize: Float = 0.70f,       // 0.1 to 1.0
    val reverbDamping: Float = 0.25f,        // 0.0 to 0.8

    // Echo / Delay
    val delayWet: Float = 0.15f,             // 0.0 to 1.0
    val delayTimeMs: Float = 220f,           // 50ms to 600ms
    val delayFeedback: Float = 0.35f,        // 0.0 to 0.8
    val delayDamping: Float = 0.30f,         // 0.0 to 0.8

    // Vocal Gains
    val inputGainDb: Float = 0.0f,           // Pre-amp gain (-12dB to +18dB)
    val masterGainDb: Float = 0.0f,          // Master output gain (-12dB to +6dB)
    val dryWetMix: Float = 1.0f              // 0.0 (all raw dry) to 1.0 (all processed)
)

data class AudioState(
    val isEngineRunning: Boolean = false,
    val isRecording: Boolean = false,
    val isRecordingPaused: Boolean = false,
    val recordingDurationSec: Int = 0,
    val currentPitchHz: Float = 0f,
    val targetNoteName: String = "--",
    val targetNoteOctave: Int = 0,
    val targetPitchHz: Float = 0f,
    val centsOffset: Float = 0f,             // -50 cents to +50 cents
    val inputLevelRms: Float = 0f,           // 0.0 to 1.0
    val inputLevelDb: Float = -60f,          // -60 dB to 0 dB
    val isVoiced: Boolean = false,
    val errorMessage: String? = null,
    val activePresetName: String = "Hard Auto-Tune"
)
