package com.intervigil.micdroid.dsp

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Environment
import android.os.Process
import android.util.Log
import com.intervigil.micdroid.audio.WavFileHelper
import com.intervigil.micdroid.model.AudioState
import com.intervigil.micdroid.model.TuningSettings
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.log10
import kotlin.math.max

/**
 * High-performance, low-latency vocal DSP engine implementing the AudioEngine contract.
 * Features:
 * - Real-time microphone capture (AudioRecord) & low-latency monitoring (AudioTrack)
 * - Complete Studio Effects Chain:
 *     Input Gain -> Noise Gate -> YIN Pitch Detection -> PSOLA Pitch Shifter ->
 *     3-Band Parametric EQ -> Vocal Compressor -> Schroeder Reverb -> Delay -> Master Gain/Limiter
 * - Dual Recording: Simultaneously archives raw un-tuned vocal (_raw.wav) and tuned vocal (_tuned.wav)
 * - Pause and Resume recording support
 * - Offline Batch Vocal Processing for re-tuning existing recordings with different musical keys
 */
class VocalDspEngine(
    private val context: Context,
    private val onStateUpdate: (AudioState) -> Unit = {},
    private val onRecordingFinished: (DualRecording) -> Unit = {}
) : AudioEngine {

    companion object {
        private const val TAG = "VocalDspEngine"
        private const val DEFAULT_SAMPLE_RATE = 44100
        private const val CHANNELS_IN = AudioFormat.CHANNEL_IN_MONO
        private const val CHANNELS_OUT = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var currentSampleRate: Int = DEFAULT_SAMPLE_RATE

    private val isRunning = AtomicBoolean(false)
    private var workerThread: Thread? = null

    // Audio hardware components
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    // Frame size (1024 samples = ~23ms at 44.1kHz / ~21ms at 48kHz)
    private val frameSize = 1024

    // DSP components
    private var pitchDetector = PitchDetector(sampleRate = currentSampleRate, bufferSize = frameSize)
    private var pitchShifter = PitchShifter(sampleRate = currentSampleRate, grainSize = frameSize)
    private var noiseGate = NoiseGate(sampleRate = currentSampleRate)
    private var parametricEq = ParametricEq(sampleRate = currentSampleRate)
    private var compressor = Compressor(sampleRate = currentSampleRate)
    private var reverbEffect = ReverbEffect(sampleRate = currentSampleRate)
    private var delayEffect = DelayEffect(sampleRate = currentSampleRate)

    // Current settings reference (thread-safe volatile)
    @Volatile
    var settings: TuningSettings = TuningSettings()

    // Real-time metric caches for non-blocking instantaneous reads
    @Volatile
    private var lastDetectedPitchHz: Float = 0f

    @Volatile
    private var lastDetectedNoteName: String = "--"

    @Volatile
    private var lastInputLevelRms: Float = 0f

    // Recording state
    private val isRecordingActive = AtomicBoolean(false)
    private val isRecordingPaused = AtomicBoolean(false)

    private var tunedRecordingFile: File? = null
    private var rawRecordingFile: File? = null
    private var tunedOutputStream: FileOutputStream? = null
    private var rawOutputStream: FileOutputStream? = null

    private var recordedBytesCount = 0
    private var recordingStartTimeMs = 0L
    private var accumulatedRecordedDurationMs = 0L
    private var pauseStartTimeMs = 0L

    // UI state throttle
    private var lastUiUpdateTime = 0L

    init {
        detectOptimalSampleRate()
    }

    private fun detectOptimalSampleRate() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val sampleRateProp = audioManager?.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        val hardwareRate = sampleRateProp?.toIntOrNull()

        val rate = if (hardwareRate == 48000) 48000 else DEFAULT_SAMPLE_RATE
        setSampleRate(rate)
    }

    override fun setSampleRate(sampleRate: Int) {
        if (currentSampleRate == sampleRate && audioRecord != null) return
        currentSampleRate = if (sampleRate == 48000) 48000 else 44100
        pitchDetector = PitchDetector(sampleRate = currentSampleRate, bufferSize = frameSize)
        pitchShifter = PitchShifter(sampleRate = currentSampleRate, grainSize = frameSize)
        noiseGate = NoiseGate(sampleRate = currentSampleRate)
        parametricEq = ParametricEq(sampleRate = currentSampleRate)
        compressor = Compressor(sampleRate = currentSampleRate)
        reverbEffect = ReverbEffect(sampleRate = currentSampleRate)
        delayEffect = DelayEffect(sampleRate = currentSampleRate)
    }

    override fun isRunning(): Boolean = isRunning.get()
    override fun isRecording(): Boolean = isRecordingActive.get()
    override fun isRecordingPaused(): Boolean = isRecordingPaused.get()

    override fun getDetectedPitch(): Float = lastDetectedPitchHz
    override fun getDetectedNote(): String = lastDetectedNoteName
    override fun getInputLevel(): Float = lastInputLevelRms

    override fun setKey(key: MusicalKey) {
        settings = settings.copy(key = key)
    }

    override fun setScale(scale: ScaleType) {
        settings = settings.copy(scale = scale)
    }

    override fun setCorrectionAmount(amount: Float) {
        settings = settings.copy(strength = amount.coerceIn(0f, 1f))
    }

    override fun setRetuneSpeed(speed: Float) {
        settings = settings.copy(retuneSpeed = speed.coerceIn(0f, 1f))
    }

    override fun setBypass(bypass: Boolean) {
        settings = settings.copy(isBypass = bypass)
    }

    override fun setLiveMonitoring(enabled: Boolean) {
        settings = settings.copy(isLiveMonitoring = enabled)
    }

    override fun setEffects(reverbWet: Float, delayWet: Float, delayTimeMs: Float, delayFeedback: Float) {
        settings = settings.copy(
            reverbWet = reverbWet.coerceIn(0f, 1f),
            delayWet = delayWet.coerceIn(0f, 1f),
            delayTimeMs = delayTimeMs.coerceIn(50f, 600f),
            delayFeedback = delayFeedback.coerceIn(0f, 0.8f)
        )
    }

    @SuppressLint("MissingPermission")
    override fun startInput(): Boolean {
        if (audioRecord != null && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            return true
        }

        val minRecordBuf = AudioRecord.getMinBufferSize(currentSampleRate, CHANNELS_IN, AUDIO_FORMAT)
        if (minRecordBuf <= 0) {
            onStateUpdate(AudioState(errorMessage = "Microphone buffer initialization failed for $currentSampleRate Hz"))
            return false
        }

        val recordBufferSize = max(minRecordBuf, frameSize * 4)

        try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                currentSampleRate,
                CHANNELS_IN,
                AUDIO_FORMAT,
                recordBufferSize
            )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                onStateUpdate(AudioState(errorMessage = "Microphone failed to initialize"))
                return false
            }

            audioRecord = record
            record.startRecording()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting AudioRecord input", e)
            onStateUpdate(AudioState(errorMessage = e.localizedMessage ?: "Failed to start microphone"))
            return false
        }
    }

    override fun startOutput(): Boolean {
        if (audioTrack != null && audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
            return true
        }

        val minTrackBuf = AudioTrack.getMinBufferSize(currentSampleRate, CHANNELS_OUT, AUDIO_FORMAT)
        if (minTrackBuf <= 0) {
            onStateUpdate(AudioState(errorMessage = "AudioTrack buffer initialization failed"))
            return false
        }

        val trackBufferSize = max(minTrackBuf, frameSize * 4)

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(currentSampleRate)
                .setChannelMask(CHANNELS_OUT)
                .setEncoding(AUDIO_FORMAT)
                .build()

            val track = AudioTrack(
                audioAttributes,
                audioFormat,
                trackBufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )

            if (track.state != AudioTrack.STATE_INITIALIZED) {
                track.release()
                onStateUpdate(AudioState(errorMessage = "Audio playback track failed to initialize"))
                return false
            }

            audioTrack = track
            track.play()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting AudioTrack output", e)
            onStateUpdate(AudioState(errorMessage = e.localizedMessage ?: "Failed to start audio track"))
            return false
        }
    }

    fun startEngine(): Boolean {
        if (isRunning.get()) return true

        if (!startInput()) return false
        if (!startOutput()) {
            stopInput()
            return false
        }

        pitchShifter.reset()
        noiseGate.reset()
        parametricEq.reset()
        compressor.reset()
        reverbEffect.reset()
        delayEffect.reset()

        isRunning.set(true)

        workerThread = Thread({ runAudioLoop() }, "VocalDspThread").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }

        return true
    }

    private fun stopInput() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignore
        }
        audioRecord = null
    }

    private fun stopOutput() {
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            // Ignore
        }
        audioTrack = null
    }

    fun stopEngine() = stop()

    override fun stop() {
        isRunning.set(false)
        stopRecording()

        try {
            workerThread?.join(400)
        } catch (e: Exception) {
            // Ignore
        }
        workerThread = null

        audioRecord?.stop()
        audioTrack?.pause()

        lastDetectedPitchHz = 0f
        lastDetectedNoteName = "--"
        lastInputLevelRms = 0f

        onStateUpdate(AudioState(isEngineRunning = false, isRecording = false, isRecordingPaused = false))
    }

    override fun release() {
        stop()
        stopInput()
        stopOutput()
    }

    /**
     * Dedicated Real-Time DSP Audio Processing Loop.
     * All buffers are pre-allocated to avoid memory allocation or GC on the audio thread.
     */
    private fun runAudioLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)

        // Pre-allocated static buffers
        val rawInput = ShortArray(frameSize)
        val processedBuffer = ShortArray(frameSize)
        val rawByteBuffer = ByteArray(frameSize * 2)
        val tunedByteBuffer = ByteArray(frameSize * 2)

        while (isRunning.get()) {
            val record = audioRecord ?: break
            val track = audioTrack ?: break

            val samplesRead = record.read(rawInput, 0, frameSize)
            if (samplesRead <= 0) continue

            val currentSettings = settings

            // 0. Pre-amp Gain
            val preGainLinear = if (currentSettings.inputGainDb != 0f) {
                Math.pow(10.0, (currentSettings.inputGainDb / 20.0).toDouble()).toFloat()
            } else 1.0f

            for (i in 0 until samplesRead) {
                val amplified = (rawInput[i] * preGainLinear).coerceIn(-32768f, 32767f).toInt().toShort()
                rawInput[i] = amplified
                processedBuffer[i] = amplified
            }

            // 1. Noise Gate (clean mic hiss during vocal pauses)
            if (currentSettings.noiseGateEnabled) {
                noiseGate.process(
                    buffer = processedBuffer,
                    length = samplesRead,
                    thresholdDb = currentSettings.noiseGateThresholdDb,
                    enabled = true
                )
            }

            // 2. Live Pitch Detection via YIN
            val detection = pitchDetector.detectPitch(processedBuffer, samplesRead)

            lastInputLevelRms = detection.rms
            lastDetectedPitchHz = if (detection.isVoiced) detection.pitchHz else 0f

            // 3. Musical Key & Scale Quantization
            val targetNote = if (detection.isVoiced && detection.pitchHz > 50f) {
                MusicalScale.findTargetNote(
                    frequencyHz = detection.pitchHz,
                    key = currentSettings.key,
                    scale = currentSettings.scale
                )
            } else {
                TargetNote("--", 0, 0f, 0f, 0)
            }

            lastDetectedNoteName = if (detection.isVoiced && targetNote.noteName != "--") {
                "${targetNote.noteName}${if (targetNote.octave > 0) targetNote.octave else ""}"
            } else {
                "--"
            }

            // 4. Target Pitch Ratio
            val targetRatio = if (detection.isVoiced && detection.pitchHz > 50f && targetNote.targetFrequency > 50f) {
                (targetNote.targetFrequency / detection.pitchHz).toDouble()
            } else {
                1.0
            }

            // 5. Granular Pitch Shifting (Time-Domain PSOLA with strength & retune speed)
            pitchShifter.process(
                input = processedBuffer,
                output = processedBuffer,
                length = samplesRead,
                targetRatio = targetRatio,
                strength = currentSettings.strength,
                retuneSpeed = currentSettings.retuneSpeed,
                isVoiced = detection.isVoiced,
                bypass = currentSettings.isBypass
            )

            if (!currentSettings.isBypass) {
                // 6. Parametric EQ (Low shelf, Peaking mid, High shelf)
                parametricEq.updateFilters(
                    lowGainDb = currentSettings.eqLowDb,
                    midGainDb = currentSettings.eqMidDb,
                    highGainDb = currentSettings.eqHighDb
                )
                parametricEq.process(processedBuffer, samplesRead)

                // 7. Dynamic Range Vocal Compressor
                if (currentSettings.compressorEnabled) {
                    compressor.process(
                        buffer = processedBuffer,
                        length = samplesRead,
                        thresholdDb = currentSettings.compressorThresholdDb,
                        ratio = currentSettings.compressorRatio,
                        makeupGainDb = currentSettings.compressorMakeupDb,
                        enabled = true
                    )
                }

                // 8. Studio Reverb
                reverbEffect.process(
                    buffer = processedBuffer,
                    length = samplesRead,
                    wet = currentSettings.reverbWet,
                    roomSize = currentSettings.reverbRoomSize,
                    damping = currentSettings.reverbDamping
                )

                // 9. Echo / Feedback Delay
                delayEffect.process(
                    buffer = processedBuffer,
                    length = samplesRead,
                    delayMs = currentSettings.delayTimeMs,
                    feedback = currentSettings.delayFeedback,
                    wet = currentSettings.delayWet,
                    damping = currentSettings.delayDamping
                )

                // 10. Master Gain & Dry/Wet Balance
                val masterGainLinear = if (currentSettings.masterGainDb != 0f) {
                    Math.pow(10.0, (currentSettings.masterGainDb / 20.0).toDouble()).toFloat()
                } else 1.0f

                val wetMix = currentSettings.dryWetMix.coerceIn(0f, 1f)
                val dryMix = 1.0f - wetMix

                for (i in 0 until samplesRead) {
                    val drySample = rawInput[i].toFloat()
                    val wetSample = processedBuffer[i].toFloat()
                    val mixed = (drySample * dryMix + wetSample * wetMix) * masterGainLinear
                    processedBuffer[i] = mixed.coerceIn(-32768f, 32767f).toInt().toShort()
                }
            }

            // 11. Live Monitoring
            if (currentSettings.isLiveMonitoring) {
                track.write(processedBuffer, 0, samplesRead)
            }

            // 12. Dual Recording stream (Raw + Tuned)
            if (isRecordingActive.get() && !isRecordingPaused.get()) {
                val tunedFos = tunedOutputStream
                val rawFos = rawOutputStream

                if (tunedFos != null && rawFos != null) {
                    for (i in 0 until samplesRead) {
                        val tunedSample = processedBuffer[i].toInt()
                        tunedByteBuffer[i * 2] = (tunedSample and 0xff).toByte()
                        tunedByteBuffer[i * 2 + 1] = ((tunedSample shr 8) and 0xff).toByte()

                        val rawSample = rawInput[i].toInt()
                        rawByteBuffer[i * 2] = (rawSample and 0xff).toByte()
                        rawByteBuffer[i * 2 + 1] = ((rawSample shr 8) and 0xff).toByte()
                    }
                    try {
                        tunedFos.write(tunedByteBuffer, 0, samplesRead * 2)
                        rawFos.write(rawByteBuffer, 0, samplesRead * 2)
                        recordedBytesCount += samplesRead * 2
                    } catch (e: Exception) {
                        Log.e(TAG, "Error writing dual recording stream", e)
                    }
                }
            }

            // 13. UI State broadcast (throttled at ~30 FPS = ~33ms)
            val now = System.currentTimeMillis()
            if (now - lastUiUpdateTime > 33) {
                lastUiUpdateTime = now

                val rms = detection.rms
                val db = if (rms > 1e-4) (20.0 * log10(rms.toDouble())).toFloat().coerceIn(-60f, 0f) else -60f

                val activeDurationMs = if (isRecordingActive.get()) {
                    if (isRecordingPaused.get()) {
                        accumulatedRecordedDurationMs
                    } else {
                        accumulatedRecordedDurationMs + (now - recordingStartTimeMs)
                    }
                } else 0L

                val durationSec = (activeDurationMs / 1000).toInt()

                val state = AudioState(
                    isEngineRunning = true,
                    isRecording = isRecordingActive.get(),
                    isRecordingPaused = isRecordingPaused.get(),
                    recordingDurationSec = durationSec,
                    currentPitchHz = if (detection.isVoiced) detection.pitchHz else 0f,
                    targetNoteName = targetNote.noteName,
                    targetNoteOctave = targetNote.octave,
                    targetPitchHz = targetNote.targetFrequency,
                    centsOffset = targetNote.centsOffset,
                    inputLevelRms = rms.coerceIn(0f, 1f),
                    inputLevelDb = db,
                    isVoiced = detection.isVoiced,
                    errorMessage = null
                )
                onStateUpdate(state)
            }
        }
    }

    override fun startRecording(): Boolean {
        if (!isRunning.get()) {
            if (!startEngine()) return false
        }
        if (isRecordingActive.get()) return true

        try {
            val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                ?: context.filesDir
            if (!musicDir.exists()) musicDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val baseName = "VocalTune_$timestamp"

            val tunedFile = File(musicDir, "${baseName}_tuned.wav")
            val rawFile = File(musicDir, "${baseName}_raw.wav")

            val tunedFos = FileOutputStream(tunedFile)
            val rawFos = FileOutputStream(rawFile)

            WavFileHelper.writeWavHeader(tunedFos, sampleRate = currentSampleRate, channels = 1, bitsPerSample = 16, pcmDataLength = 0)
            WavFileHelper.writeWavHeader(rawFos, sampleRate = currentSampleRate, channels = 1, bitsPerSample = 16, pcmDataLength = 0)

            tunedRecordingFile = tunedFile
            rawRecordingFile = rawFile
            tunedOutputStream = tunedFos
            rawOutputStream = rawFos

            recordedBytesCount = 0
            accumulatedRecordedDurationMs = 0L
            recordingStartTimeMs = System.currentTimeMillis()
            isRecordingPaused.set(false)
            isRecordingActive.set(true)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start dual recording", e)
            return false
        }
    }

    override fun pauseRecording() {
        if (isRecordingActive.get() && !isRecordingPaused.get()) {
            accumulatedRecordedDurationMs += (System.currentTimeMillis() - recordingStartTimeMs)
            isRecordingPaused.set(true)
            onStateUpdate(AudioState(isEngineRunning = isRunning.get(), isRecording = true, isRecordingPaused = true))
        }
    }

    override fun resumeRecording() {
        if (isRecordingActive.get() && isRecordingPaused.get()) {
            recordingStartTimeMs = System.currentTimeMillis()
            isRecordingPaused.set(false)
            onStateUpdate(AudioState(isEngineRunning = isRunning.get(), isRecording = true, isRecordingPaused = false))
        }
    }

    override fun stopRecording(): DualRecording? {
        if (!isRecordingActive.get()) return null
        isRecordingActive.set(false)
        isRecordingPaused.set(false)

        val tuned = tunedRecordingFile
        val raw = rawRecordingFile

        try {
            tunedOutputStream?.flush()
            tunedOutputStream?.close()
            tunedOutputStream = null

            rawOutputStream?.flush()
            rawOutputStream?.close()
            rawOutputStream = null

            if (tuned != null && tuned.exists()) {
                WavFileHelper.updateWavHeaderSizes(tuned, recordedBytesCount)
            }
            if (raw != null && raw.exists()) {
                WavFileHelper.updateWavHeaderSizes(raw, recordedBytesCount)
            }

            if (tuned != null) {
                val dual = DualRecording(tunedFile = tuned, rawFile = raw)
                onRecordingFinished(dual)
                tunedRecordingFile = null
                rawRecordingFile = null
                recordedBytesCount = 0
                accumulatedRecordedDurationMs = 0L
                return dual
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to finalize dual recording", e)
        }

        tunedRecordingFile = null
        rawRecordingFile = null
        recordedBytesCount = 0
        accumulatedRecordedDurationMs = 0L
        return null
    }

    /**
     * Offline High-Quality Re-processing of an existing raw vocal recording.
     * Re-runs the exact vocal DSP chain with new Key, Scale, and Correction settings.
     */
    override fun processWavFile(
        rawInputFile: File,
        outputFile: File,
        settings: TuningSettings,
        onProgress: (Float) -> Unit
    ): Boolean {
        if (!rawInputFile.exists()) return false

        try {
            val samples = WavFileHelper.readWavPcm(rawInputFile)
            if (samples.isEmpty()) return false

            val totalSamples = samples.size
            val localPitchDetector = PitchDetector(sampleRate = currentSampleRate, bufferSize = frameSize)
            val localPitchShifter = PitchShifter(sampleRate = currentSampleRate, grainSize = frameSize)
            val localEq = ParametricEq(sampleRate = currentSampleRate)
            val localCompressor = Compressor(sampleRate = currentSampleRate)
            val localReverb = ReverbEffect(sampleRate = currentSampleRate)
            val localDelay = DelayEffect(sampleRate = currentSampleRate)
            val localNoiseGate = NoiseGate(sampleRate = currentSampleRate)

            val processed = ShortArray(totalSamples)
            val blockIn = ShortArray(frameSize)
            val blockOut = ShortArray(frameSize)

            var offset = 0
            while (offset < totalSamples) {
                val blockSize = minOf(frameSize, totalSamples - offset)
                System.arraycopy(samples, offset, blockIn, 0, blockSize)

                // 1. Noise gate
                if (settings.noiseGateEnabled) {
                    localNoiseGate.process(blockIn, blockSize, settings.noiseGateThresholdDb)
                }

                // 2. Pitch detect
                val detection = localPitchDetector.detectPitch(blockIn, blockSize)

                // 3. Scale quantization
                val targetNote = if (detection.isVoiced && detection.pitchHz > 50f) {
                    MusicalScale.findTargetNote(detection.pitchHz, settings.key, settings.scale)
                } else {
                    TargetNote("--", 0, 0f, 0f, 0)
                }

                val targetRatio = if (detection.isVoiced && detection.pitchHz > 50f && targetNote.targetFrequency > 50f) {
                    (targetNote.targetFrequency / detection.pitchHz).toDouble()
                } else 1.0

                // 4. Pitch shift
                localPitchShifter.process(
                    input = blockIn,
                    output = blockOut,
                    length = blockSize,
                    targetRatio = targetRatio,
                    strength = settings.strength,
                    retuneSpeed = settings.retuneSpeed,
                    isVoiced = detection.isVoiced,
                    bypass = settings.isBypass
                )

                if (!settings.isBypass) {
                    // 5. EQ
                    localEq.updateFilters(settings.eqLowDb, settings.eqMidDb, settings.eqHighDb)
                    localEq.process(blockOut, blockSize)

                    // 6. Compressor
                    if (settings.compressorEnabled) {
                        localCompressor.process(
                            buffer = blockOut,
                            length = blockSize,
                            thresholdDb = settings.compressorThresholdDb,
                            ratio = settings.compressorRatio,
                            makeupGainDb = settings.compressorMakeupDb
                        )
                    }

                    // 7. Reverb
                    localReverb.process(
                        buffer = blockOut,
                        length = blockSize,
                        wet = settings.reverbWet,
                        roomSize = settings.reverbRoomSize,
                        damping = settings.reverbDamping
                    )

                    // 8. Delay
                    localDelay.process(
                        buffer = blockOut,
                        length = blockSize,
                        delayMs = settings.delayTimeMs,
                        feedback = settings.delayFeedback,
                        wet = settings.delayWet,
                        damping = settings.delayDamping
                    )
                }

                System.arraycopy(blockOut, 0, processed, offset, blockSize)
                offset += blockSize

                onProgress(offset.toFloat() / totalSamples.toFloat())
            }

            WavFileHelper.writeWavFromPcm(outputFile, processed, sampleRate = currentSampleRate)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Offline re-tuning failed", e)
            return false
        }
    }
}
