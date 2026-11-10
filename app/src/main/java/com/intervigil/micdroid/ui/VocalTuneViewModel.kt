package com.intervigil.micdroid.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.intervigil.micdroid.audio.AudioFocusManager
import com.intervigil.micdroid.audio.WavFileHelper
import com.intervigil.micdroid.dsp.MusicalKey
import com.intervigil.micdroid.dsp.ScaleType
import com.intervigil.micdroid.dsp.VocalDspEngine
import com.intervigil.micdroid.model.AudioState
import com.intervigil.micdroid.model.PresetManager
import com.intervigil.micdroid.model.SavedRecording
import com.intervigil.micdroid.model.TuningSettings
import com.intervigil.micdroid.model.VocalPreset
import com.intervigil.micdroid.service.VocalRecordingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class VocalTuneViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()

    private val _audioState = MutableStateFlow(AudioState())
    val audioState: StateFlow<AudioState> = _audioState.asStateFlow()

    private val _tuningSettings = MutableStateFlow(TuningSettings())
    val tuningSettings: StateFlow<TuningSettings> = _tuningSettings.asStateFlow()

    private val _presets = MutableStateFlow<List<VocalPreset>>(emptyList())
    val presets: StateFlow<List<VocalPreset>> = _presets.asStateFlow()

    private val _selectedPreset = MutableStateFlow<VocalPreset>(VocalPreset.HARD_AUTOTUNE)
    val selectedPreset: StateFlow<VocalPreset> = _selectedPreset.asStateFlow()

    private val _recordings = MutableStateFlow<List<SavedRecording>>(emptyList())
    val recordings: StateFlow<List<SavedRecording>> = _recordings.asStateFlow()

    // In-app player state for recordings
    private var mediaPlayer: MediaPlayer? = null
    private val _playingFile = MutableStateFlow<File?>(null)
    val playingFile: StateFlow<File?> = _playingFile.asStateFlow()

    // A/B Comparison state: true = playing Raw dry vocal, false = playing Tuned vocal
    private val _isPlayingRawComparison = MutableStateFlow(false)
    val isPlayingRawComparison: StateFlow<Boolean> = _isPlayingRawComparison.asStateFlow()

    private val _currentPlayingRecording = MutableStateFlow<SavedRecording?>(null)
    val currentPlayingRecording: StateFlow<SavedRecording?> = _currentPlayingRecording.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    // Offline Batch Re-Tuning state
    private val _isOfflineProcessing = MutableStateFlow(false)
    val isOfflineProcessing: StateFlow<Boolean> = _isOfflineProcessing.asStateFlow()

    private val _offlineProgress = MutableStateFlow(0f)
    val offlineProgress: StateFlow<Float> = _offlineProgress.asStateFlow()

    private val audioFocusManager = AudioFocusManager(
        context = context,
        onFocusLost = {
            pauseOrStopOnFocusLost()
        },
        onFocusGained = {
            // Audio focus regained
        }
    )

    private val dspEngine: VocalDspEngine = VocalDspEngine(
        context = context,
        onStateUpdate = { newState ->
            _audioState.update {
                newState.copy(
                    activePresetName = _selectedPreset.value.name
                )
            }
        },
        onRecordingFinished = { dual ->
            loadRecordings()
        }
    )

    init {
        loadPresets()
        loadRecordings()
    }

    private fun loadPresets() {
        val customs = PresetManager.loadCustomPresets(context)
        val all = VocalPreset.BUILT_IN_PRESETS + customs
        _presets.value = all
    }

    fun selectPreset(preset: VocalPreset) {
        _selectedPreset.value = preset
        val newSettings = preset.settings.copy(
            key = _tuningSettings.value.key,
            scale = _tuningSettings.value.scale,
            isLiveMonitoring = _tuningSettings.value.isLiveMonitoring
        )
        _tuningSettings.value = newSettings
        dspEngine.settings = newSettings
        _audioState.update { it.copy(activePresetName = preset.name) }
    }

    fun saveCustomPreset(name: String) {
        val current = _tuningSettings.value
        val created = PresetManager.saveCustomPreset(context, name, current)
        loadPresets()
        _selectedPreset.value = created
        _audioState.update { it.copy(activePresetName = created.name) }
    }

    fun deleteCustomPreset(presetId: String) {
        PresetManager.deleteCustomPreset(context, presetId)
        loadPresets()
        if (_selectedPreset.value.id == presetId) {
            selectPreset(VocalPreset.HARD_AUTOTUNE)
        }
    }

    fun toggleEngine() {
        if (dspEngine.isRunning()) {
            if (dspEngine.isRecording()) {
                stopRecording()
            }
            dspEngine.stopEngine()
            audioFocusManager.abandonAudioFocus()
            _audioState.update { it.copy(isEngineRunning = false, isRecording = false, isRecordingPaused = false) }
        } else {
            val granted = audioFocusManager.requestAudioFocus()
            dspEngine.settings = _tuningSettings.value
            val started = dspEngine.startEngine()
            if (!started && granted) {
                audioFocusManager.abandonAudioFocus()
            }
        }
    }

    fun toggleRecording() {
        if (dspEngine.isRecording()) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    fun startRecording() {
        if (!dspEngine.isRunning()) {
            val granted = audioFocusManager.requestAudioFocus()
            dspEngine.settings = _tuningSettings.value
            if (!dspEngine.startEngine() && granted) {
                audioFocusManager.abandonAudioFocus()
                return
            }
        }
        val recorded = dspEngine.startRecording()
        if (recorded) {
            VocalRecordingService.startService(context)
            _audioState.update { it.copy(isRecording = true, isRecordingPaused = false) }
        }
    }

    fun pauseRecording() {
        dspEngine.pauseRecording()
        val intent = Intent(context, VocalRecordingService::class.java).apply {
            action = VocalRecordingService.ACTION_PAUSE
        }
        context.startService(intent)
        _audioState.update { it.copy(isRecordingPaused = true) }
    }

    fun resumeRecording() {
        dspEngine.resumeRecording()
        val intent = Intent(context, VocalRecordingService::class.java).apply {
            action = VocalRecordingService.ACTION_RESUME
        }
        context.startService(intent)
        _audioState.update { it.copy(isRecordingPaused = false) }
    }

    fun stopRecording() {
        dspEngine.stopRecording()
        VocalRecordingService.stopService(context)
        _audioState.update { it.copy(isRecording = false, isRecordingPaused = false) }
        loadRecordings()
    }

    private fun pauseOrStopOnFocusLost() {
        if (dspEngine.isRecording()) {
            stopRecording()
        }
        dspEngine.stopEngine()
        _audioState.update { it.copy(isEngineRunning = false, isRecording = false, isRecordingPaused = false) }
    }

    // Tuning and effects modifiers
    fun updateStrength(strength: Float) {
        val updated = _tuningSettings.value.copy(strength = strength.coerceIn(0f, 1f))
        _tuningSettings.value = updated
        dspEngine.settings = updated
    }

    fun updateKey(key: MusicalKey) {
        val updated = _tuningSettings.value.copy(key = key)
        _tuningSettings.value = updated
        dspEngine.settings = updated
    }

    fun updateScale(scale: ScaleType) {
        val updated = _tuningSettings.value.copy(scale = scale)
        _tuningSettings.value = updated
        dspEngine.settings = updated
    }

    fun updateRetuneSpeed(speed: Float) {
        val updated = _tuningSettings.value.copy(retuneSpeed = speed.coerceIn(0f, 1f))
        _tuningSettings.value = updated
        dspEngine.settings = updated
    }

    fun updateNoiseGate(enabled: Boolean, thresholdDb: Float = _tuningSettings.value.noiseGateThresholdDb) {
        val updated = _tuningSettings.value.copy(
            noiseGateEnabled = enabled,
            noiseGateThresholdDb = thresholdDb.coerceIn(-60f, -15f)
        )
        _tuningSettings.value = updated
        dspEngine.settings = updated
    }

    fun updateEq(lowDb: Float, midDb: Float, highDb: Float) {
        val updated = _tuningSettings.value.copy(
            eqLowDb = lowDb.coerceIn(-12f, 12f),
            eqMidDb = midDb.coerceIn(-12f, 12f),
            eqHighDb = highDb.coerceIn(-12f, 12f)
        )
        _tuningSettings.value = updated
        dspEngine.settings = updated
    }

    fun updateCompressor(enabled: Boolean, thresholdDb: Float, ratio: Float, makeupDb: Float) {
        val updated = _tuningSettings.value.copy(
            compressorEnabled = enabled,
            compressorThresholdDb = thresholdDb.coerceIn(-40f, 0f),
            compressorRatio = ratio.coerceIn(1f, 10f),
            compressorMakeupDb = makeupDb.coerceIn(0f, 12f)
        )
        _tuningSettings.value = updated
        dspEngine.settings = updated
    }

    fun updateReverb(
        wet: Float,
        roomSize: Float = _tuningSettings.value.reverbRoomSize,
        damping: Float = _tuningSettings.value.reverbDamping
    ) {
        val updated = _tuningSettings.value.copy(
            reverbWet = wet.coerceIn(0f, 1f),
            reverbRoomSize = roomSize.coerceIn(0.1f, 1.0f),
            reverbDamping = damping.coerceIn(0f, 0.8f)
        )
        _tuningSettings.value = updated
        dspEngine.settings = updated
    }

    fun updateDelay(
        wet: Float,
        timeMs: Float = _tuningSettings.value.delayTimeMs,
        feedback: Float = _tuningSettings.value.delayFeedback,
        damping: Float = _tuningSettings.value.delayDamping
    ) {
        val updated = _tuningSettings.value.copy(
            delayWet = wet.coerceIn(0f, 1f),
            delayTimeMs = timeMs.coerceIn(50f, 600f),
            delayFeedback = feedback.coerceIn(0f, 0.8f),
            delayDamping = damping.coerceIn(0f, 0.8f)
        )
        _tuningSettings.value = updated
        dspEngine.settings = updated
    }

    fun updateGains(inputGainDb: Float, masterGainDb: Float) {
        val updated = _tuningSettings.value.copy(
            inputGainDb = inputGainDb.coerceIn(-12f, 18f),
            masterGainDb = masterGainDb.coerceIn(-12f, 6f)
        )
        _tuningSettings.value = updated
        dspEngine.settings = updated
    }

    fun toggleBypass() {
        val updated = _tuningSettings.value.copy(isBypass = !_tuningSettings.value.isBypass)
        _tuningSettings.value = updated
        dspEngine.settings = updated
    }

    fun toggleLiveMonitoring() {
        val updated = _tuningSettings.value.copy(isLiveMonitoring = !_tuningSettings.value.isLiveMonitoring)
        _tuningSettings.value = updated
        dspEngine.settings = updated
    }

    // Recordings library management
    fun loadRecordings() {
        viewModelScope.launch(Dispatchers.IO) {
            val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
            val allFiles = musicDir.listFiles { file -> file.isFile && file.name.endsWith(".wav", ignoreCase = true) }
                ?: emptyArray()

            // Filter out standalone _raw files so they don't clutter the library as duplicates,
            // but attach them to their matching tuned recordings!
            val rawFilesMap = allFiles.filter { it.name.contains("_raw.wav") }
                .associateBy { it.name.substringBefore("_raw.wav") }

            val mainFiles = allFiles.filter { !it.name.contains("_raw.wav") }
                .sortedByDescending { it.lastModified() }

            val list = mainFiles.map { f ->
                val base = f.name.substringBefore("_tuned.wav").substringBefore(".wav")
                val matchedRaw = rawFilesMap[base]
                val duration = WavFileHelper.getWavDurationSeconds(f)
                SavedRecording(
                    file = f,
                    name = f.nameWithoutExtension.removeSuffix("_tuned"),
                    durationSeconds = duration,
                    sizeBytes = f.length(),
                    timestamp = f.lastModified(),
                    rawFile = matchedRaw,
                    isProcessed = true
                )
            }
            _recordings.value = list
        }
    }

    fun playRecording(recording: SavedRecording, playRaw: Boolean = false) {
        val targetFile = if (playRaw && recording.rawFile != null) recording.rawFile else recording.file

        if (_playingFile.value == targetFile && mediaPlayer?.isPlaying == true) {
            stopPlayback()
            return
        }

        stopPlayback()
        try {
            val mp = MediaPlayer().apply {
                setDataSource(targetFile.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    stopPlayback()
                }
            }
            mediaPlayer = mp
            _playingFile.value = targetFile
            _currentPlayingRecording.value = recording
            _isPlayingRawComparison.value = playRaw

            viewModelScope.launch {
                while (_playingFile.value == targetFile && mp.isPlaying) {
                    val current = mp.currentPosition.toFloat()
                    val total = mp.duration.toFloat()
                    if (total > 0f) {
                        _playbackProgress.value = (current / total).coerceIn(0f, 1f)
                    }
                    delay(100)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopPlayback()
        }
    }

    /**
     * Toggles A/B comparison between Raw (dry vocal) and Tuned (pitch-corrected vocal)
     * during playback at the current exact time position.
     */
    fun toggleComparisonPlayback() {
        val recording = _currentPlayingRecording.value ?: return
        val mp = mediaPlayer ?: return
        if (!mp.isPlaying) return

        val currentPositionMs = mp.currentPosition
        val nextPlayRaw = !_isPlayingRawComparison.value
        val nextFile = if (nextPlayRaw && recording.rawFile != null) recording.rawFile else recording.file

        try {
            mp.stop()
            mp.reset()
            mp.setDataSource(nextFile.absolutePath)
            mp.prepare()
            mp.seekTo(currentPositionMs)
            mp.start()

            _playingFile.value = nextFile
            _isPlayingRawComparison.value = nextPlayRaw
        } catch (e: Exception) {
            e.printStackTrace()
            stopPlayback()
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // Ignore
        }
        mediaPlayer = null
        _playingFile.value = null
        _currentPlayingRecording.value = null
        _playbackProgress.value = 0f
        _isPlayingRawComparison.value = false
    }

    /**
     * Offline Re-Tuning: allows the user to re-render any existing recorded vocal
     * with a newly selected Musical Key, Scale, or Auto-Tune Strength.
     */
    fun retuneRecording(
        recording: SavedRecording,
        newKey: MusicalKey,
        newScale: ScaleType,
        newStrength: Float,
        newRetuneSpeed: Float = 0.15f,
        onComplete: (File) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isOfflineProcessing.value = true
            _offlineProgress.value = 0f

            val sourceRawFile = recording.rawFile ?: recording.file
            val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
            val newFile = File(musicDir, "${recording.name}_tuned_${newKey.displayName}_${newScale.name.lowercase()}.wav")

            val customSettings = _tuningSettings.value.copy(
                key = newKey,
                scale = newScale,
                strength = newStrength,
                retuneSpeed = newRetuneSpeed,
                isBypass = false
            )

            val success = dspEngine.processWavFile(
                rawInputFile = sourceRawFile,
                outputFile = newFile,
                settings = customSettings,
                onProgress = { progress ->
                    _offlineProgress.value = progress
                }
            )

            _isOfflineProcessing.value = false
            _offlineProgress.value = 1.0f

            if (success) {
                loadRecordings()
                launch(Dispatchers.Main) {
                    onComplete(newFile)
                }
            }
        }
    }

    fun deleteRecording(recording: SavedRecording) {
        if (_currentPlayingRecording.value == recording) {
            stopPlayback()
        }
        recording.file.delete()
        recording.rawFile?.delete()
        loadRecordings()
    }

    fun shareRecording(recording: SavedRecording) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                recording.file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/wav"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Share VocalTune WAV").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        stopPlayback()
        VocalRecordingService.stopService(context)
        dspEngine.stopEngine()
        audioFocusManager.abandonAudioFocus()
        super.onCleared()
    }
}
