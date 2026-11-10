package com.intervigil.micdroid.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.intervigil.micdroid.ui.VocalTuneViewModel
import com.intervigil.micdroid.ui.components.MasterEngineButton
import com.intervigil.micdroid.ui.components.MusicalKeySelector
import com.intervigil.micdroid.ui.components.PitchDisplay
import com.intervigil.micdroid.ui.components.PresetSelectorCarousel
import com.intervigil.micdroid.ui.components.RecordControlBar
import com.intervigil.micdroid.ui.components.SavePresetDialog
import com.intervigil.micdroid.ui.components.ScaleTypeSelector
import com.intervigil.micdroid.ui.components.StudioRackQuickToggles
import com.intervigil.micdroid.ui.components.StudioSlider
import com.intervigil.micdroid.ui.components.VuMeter
import com.intervigil.micdroid.ui.theme.ElectricViolet
import com.intervigil.micdroid.ui.theme.FlatAmber
import com.intervigil.micdroid.ui.theme.InTuneGreen
import com.intervigil.micdroid.ui.theme.NeonCyan
import com.intervigil.micdroid.ui.theme.NeonPink
import com.intervigil.micdroid.ui.theme.NeonPurpleBright
import com.intervigil.micdroid.ui.theme.NeonPurplePrimary
import com.intervigil.micdroid.ui.theme.StudioCard
import com.intervigil.micdroid.ui.theme.StudioDarkBackground
import com.intervigil.micdroid.ui.theme.TextPrimary
import com.intervigil.micdroid.ui.theme.TextSecondary

@Composable
fun StudioScreen(
    viewModel: VocalTuneViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioState by viewModel.audioState.collectAsState()
    val settings by viewModel.tuningSettings.collectAsState()
    val presets by viewModel.presets.collectAsState()
    val selectedPreset by viewModel.selectedPreset.collectAsState()

    var showSavePresetDialog by remember { mutableStateOf(false) }
    var showAdvancedEffects by remember { mutableStateOf(true) }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            viewModel.toggleEngine()
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioDarkBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Permission Banner if missing
        if (!hasMicPermission) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF451A03))
                    .border(1.dp, FlatAmber, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = FlatAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Microphone Permission Required",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Enable mic to process and auto-tune live voice",
                                fontSize = 11.sp,
                                color = Color(0xFFFDE68A)
                            )
                        }
                    }

                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        colors = ButtonDefaults.buttonColors(containerColor = FlatAmber, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "GRANT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Error message if any
        if (audioState.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF7F1D1D))
                    .padding(10.dp)
            ) {
                Text(
                    text = audioState.errorMessage ?: "",
                    fontSize = 11.sp,
                    color = Color.White
                )
            }
        }

        // Live Audio VU Meter
        VuMeter(
            levelRms = if (audioState.isEngineRunning) audioState.inputLevelRms else 0f,
            levelDb = if (audioState.isEngineRunning) audioState.inputLevelDb else -60f,
            isVoiced = audioState.isVoiced
        )

        // Pitch & Tuning Display
        PitchDisplay(
            pitchHz = audioState.currentPitchHz,
            targetNote = audioState.targetNoteName,
            octave = audioState.targetNoteOctave,
            targetHz = audioState.targetPitchHz,
            centsOffset = audioState.centsOffset,
            isVoiced = audioState.isVoiced
        )

        // Master Activate / Deactivate Button
        MasterEngineButton(
            isRunning = audioState.isEngineRunning,
            onToggle = {
                if (!hasMicPermission) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                } else {
                    viewModel.toggleEngine()
                }
            }
        )

        // Preset Carousel (8 Studio Presets + Custom Presets)
        PresetSelectorCarousel(
            presets = presets,
            selectedPreset = selectedPreset,
            onPresetSelected = { viewModel.selectPreset(it) },
            onSaveCustomPresetClick = { showSavePresetDialog = true }
        )

        // Dual Record Control Bar with Pause / Resume
        RecordControlBar(
            isRecording = audioState.isRecording,
            isPaused = audioState.isRecordingPaused,
            recordingDurationSec = audioState.recordingDurationSec,
            onStartRecording = {
                if (!hasMicPermission) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                } else {
                    viewModel.startRecording()
                }
            },
            onPauseRecording = { viewModel.pauseRecording() },
            onResumeRecording = { viewModel.resumeRecording() },
            onStopRecording = { viewModel.stopRecording() }
        )

        // Musical Key & Scale Selection
        MusicalKeySelector(
            selectedKey = settings.key,
            onKeySelected = { viewModel.updateKey(it) }
        )

        ScaleTypeSelector(
            selectedScale = settings.scale,
            onScaleSelected = { viewModel.updateScale(it) }
        )

        // Auto-Tune Core Parameters
        StudioSlider(
            label = "AUTO-TUNE STRENGTH",
            value = settings.strength,
            displayValue = "${(settings.strength * 100).toInt()}%",
            subLabel = "0% = Natural voice, 100% = Hard snap",
            onValueChange = { viewModel.updateStrength(it) },
            color = NeonPurplePrimary
        )

        StudioSlider(
            label = "RETUNE SPEED",
            value = settings.retuneSpeed,
            displayValue = if (settings.retuneSpeed < 0.10f) "0ms (INSTANT T-PAIN)" else if (settings.retuneSpeed < 0.40f) "FAST" else "NATURAL",
            subLabel = "Retune transition speed to target pitch",
            onValueChange = { viewModel.updateRetuneSpeed(it) },
            color = ElectricViolet
        )

        // Expandable Studio Effects Rack Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(StudioCard)
                .border(1.dp, Color(0xFF2D2942), RoundedCornerShape(12.dp))
                .clickable { showAdvancedEffects = !showAdvancedEffects }
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Equalizer,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "STUDIO EFFECTS RACK",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Noise Gate, 3-Band EQ, Compressor, Reverb, Delay",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Icon(
                imageVector = if (showAdvancedEffects) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = NeonCyan
            )
        }

        // Advanced Effects Rack
        AnimatedVisibility(visible = showAdvancedEffects) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Noise Gate Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(StudioCard)
                        .border(1.dp, Color(0xFF2D2942), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = FlatAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "NOISE GATE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = FlatAmber
                            )
                        }

                        Switch(
                            checked = settings.noiseGateEnabled,
                            onCheckedChange = { viewModel.updateNoiseGate(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = FlatAmber,
                                checkedTrackColor = Color(0xFF78350F)
                            )
                        )
                    }

                    if (settings.noiseGateEnabled) {
                        Spacer(modifier = Modifier.height(6.dp))
                        StudioSlider(
                            label = "GATE THRESHOLD",
                            value = (settings.noiseGateThresholdDb + 60f) / 45f,
                            displayValue = "${settings.noiseGateThresholdDb.toInt()} dB",
                            subLabel = "Cuts mic hiss & room noise between singing phrases",
                            onValueChange = { norm ->
                                val db = -60f + (norm * 45f)
                                viewModel.updateNoiseGate(true, db)
                            },
                            color = FlatAmber
                        )
                    }
                }

                // 2. 3-Band Parametric EQ Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(StudioCard)
                        .border(1.dp, Color(0xFF2D2942), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = NeonPurpleBright,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PARAMETRIC EQUALIZER (3-BAND)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonPurpleBright
                        )
                    }

                    StudioSlider(
                        label = "LOW BASS / WARMTH (150 Hz)",
                        value = (settings.eqLowDb + 12f) / 24f,
                        displayValue = String.format("%+.1f dB", settings.eqLowDb),
                        onValueChange = { norm ->
                            val db = -12f + (norm * 24f)
                            viewModel.updateEq(db, settings.eqMidDb, settings.eqHighDb)
                        },
                        color = NeonPurplePrimary
                    )

                    StudioSlider(
                        label = "MID VOCAL PRESENCE (2.2 kHz)",
                        value = (settings.eqMidDb + 12f) / 24f,
                        displayValue = String.format("%+.1f dB", settings.eqMidDb),
                        onValueChange = { norm ->
                            val db = -12f + (norm * 24f)
                            viewModel.updateEq(settings.eqLowDb, db, settings.eqHighDb)
                        },
                        color = ElectricViolet
                    )

                    StudioSlider(
                        label = "HIGH AIR & SHEEN (8.0 kHz)",
                        value = (settings.eqHighDb + 12f) / 24f,
                        displayValue = String.format("%+.1f dB", settings.eqHighDb),
                        onValueChange = { norm ->
                            val db = -12f + (norm * 24f)
                            viewModel.updateEq(settings.eqLowDb, settings.eqMidDb, db)
                        },
                        color = NeonCyan
                    )
                }

                // 3. Dynamic Range Compressor Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(StudioCard)
                        .border(1.dp, Color(0xFF2D2942), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = NeonPink,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "VOCAL COMPRESSOR",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonPink
                            )
                        }

                        Switch(
                            checked = settings.compressorEnabled,
                            onCheckedChange = {
                                viewModel.updateCompressor(it, settings.compressorThresholdDb, settings.compressorRatio, settings.compressorMakeupDb)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonPink,
                                checkedTrackColor = Color(0xFF831843)
                            )
                        )
                    }

                    if (settings.compressorEnabled) {
                        Spacer(modifier = Modifier.height(6.dp))

                        StudioSlider(
                            label = "THRESHOLD",
                            value = (settings.compressorThresholdDb + 40f) / 40f,
                            displayValue = "${settings.compressorThresholdDb.toInt()} dB",
                            onValueChange = { norm ->
                                val db = -40f + (norm * 40f)
                                viewModel.updateCompressor(true, db, settings.compressorRatio, settings.compressorMakeupDb)
                            },
                            color = NeonPink
                        )

                        StudioSlider(
                            label = "RATIO",
                            value = (settings.compressorRatio - 1f) / 9f,
                            displayValue = String.format("%.1f:1", settings.compressorRatio),
                            onValueChange = { norm ->
                                val r = 1f + (norm * 9f)
                                viewModel.updateCompressor(true, settings.compressorThresholdDb, r, settings.compressorMakeupDb)
                            },
                            color = NeonPink
                        )

                        StudioSlider(
                            label = "MAKEUP GAIN",
                            value = settings.compressorMakeupDb / 12f,
                            displayValue = String.format("+%.1f dB", settings.compressorMakeupDb),
                            onValueChange = { norm ->
                                val g = norm * 12f
                                viewModel.updateCompressor(true, settings.compressorThresholdDb, settings.compressorRatio, g)
                            },
                            color = NeonPink
                        )
                    }
                }

                // 4. Reverb & Echo Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(StudioCard)
                        .border(1.dp, Color(0xFF2D2942), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SurroundSound,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SPATIAL REVERB & ECHO",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }

                    StudioSlider(
                        label = "REVERB SPACE WET",
                        value = settings.reverbWet,
                        displayValue = "${(settings.reverbWet * 100).toInt()}%",
                        onValueChange = { viewModel.updateReverb(it, settings.reverbRoomSize, settings.reverbDamping) },
                        color = NeonCyan
                    )

                    StudioSlider(
                        label = "REVERB ROOM SIZE",
                        value = settings.reverbRoomSize,
                        displayValue = "${(settings.reverbRoomSize * 100).toInt()}%",
                        onValueChange = { viewModel.updateReverb(settings.reverbWet, it, settings.reverbDamping) },
                        color = NeonCyan
                    )

                    StudioSlider(
                        label = "ECHO / DELAY WET",
                        value = settings.delayWet,
                        displayValue = "${(settings.delayWet * 100).toInt()}%",
                        subLabel = "Delay time: ${settings.delayTimeMs.toInt()}ms • Feedback: ${(settings.delayFeedback * 100).toInt()}%",
                        onValueChange = { viewModel.updateDelay(it, settings.delayTimeMs, settings.delayFeedback, settings.delayDamping) },
                        color = NeonPink
                    )
                }

                // 5. Vocal Gains Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(StudioCard)
                        .border(1.dp, Color(0xFF2D2942), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "VOCAL GAIN & STAGE LEVELS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = InTuneGreen
                    )

                    StudioSlider(
                        label = "PRE-AMP INPUT GAIN",
                        value = (settings.inputGainDb + 12f) / 30f,
                        displayValue = String.format("%+.1f dB", settings.inputGainDb),
                        onValueChange = { norm ->
                            val db = -12f + (norm * 30f)
                            viewModel.updateGains(db, settings.masterGainDb)
                        },
                        color = InTuneGreen
                    )
                }
            }
        }

        // Bypass and Live Monitor toggles
        StudioRackQuickToggles(
            isBypass = settings.isBypass,
            onToggleBypass = { viewModel.toggleBypass() },
            isLiveMonitoring = settings.isLiveMonitoring,
            onToggleMonitoring = { viewModel.toggleLiveMonitoring() }
        )

        Spacer(modifier = Modifier.height(20.dp))
    }

    if (showSavePresetDialog) {
        SavePresetDialog(
            onDismiss = { showSavePresetDialog = false },
            onSave = { name ->
                viewModel.saveCustomPreset(name)
                showSavePresetDialog = false
            }
        )
    }
}
