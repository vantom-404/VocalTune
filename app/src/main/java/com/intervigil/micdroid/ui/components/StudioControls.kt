package com.intervigil.micdroid.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intervigil.micdroid.dsp.MusicalKey
import com.intervigil.micdroid.dsp.ScaleType
import com.intervigil.micdroid.model.VocalPreset
import com.intervigil.micdroid.ui.theme.ElectricViolet
import com.intervigil.micdroid.ui.theme.FlatAmber
import com.intervigil.micdroid.ui.theme.InTuneGreen
import com.intervigil.micdroid.ui.theme.NeonCyan
import com.intervigil.micdroid.ui.theme.NeonPink
import com.intervigil.micdroid.ui.theme.NeonPurpleBright
import com.intervigil.micdroid.ui.theme.NeonPurplePrimary
import com.intervigil.micdroid.ui.theme.SharpRed
import com.intervigil.micdroid.ui.theme.StudioCard
import com.intervigil.micdroid.ui.theme.StudioCardElevated
import com.intervigil.micdroid.ui.theme.TextMuted
import com.intervigil.micdroid.ui.theme.TextPrimary
import com.intervigil.micdroid.ui.theme.TextSecondary

@Composable
fun MasterEngineButton(
    isRunning: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRunning) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(96.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    if (isRunning) {
                        Brush.radialGradient(
                            listOf(
                                NeonPurpleBright,
                                NeonPurplePrimary,
                                Color(0xFF3B0764)
                            )
                        )
                    } else {
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF262238),
                                Color(0xFF161424)
                            )
                        )
                    }
                )
                .border(
                    width = 3.dp,
                    color = if (isRunning) NeonCyan else Color(0xFF3B3556),
                    shape = CircleShape
                )
                .clickable { onToggle() }
                .testTag("master_engine_button")
        ) {
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = if (isRunning) "Stop Audio Processing" else "Start Audio Processing",
                tint = if (isRunning) Color.White else TextMuted,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isRunning) "AUTO-TUNE ACTIVE" else "TAP TO ACTIVATE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isRunning) NeonCyan else TextSecondary,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun PresetSelectorCarousel(
    presets: List<VocalPreset>,
    selectedPreset: VocalPreset,
    onPresetSelected: (VocalPreset) -> Unit,
    onSaveCustomPresetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BookmarkBorder,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "VOCAL PRESETS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricViolet,
                    letterSpacing = 0.5.sp
                )
            }

            TextButton(
                onClick = onSaveCustomPresetClick,
                modifier = Modifier.testTag("save_custom_preset_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "SAVE PRESET",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                val isSelected = preset.id == selectedPreset.id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) NeonPurplePrimary else StudioCard)
                        .border(
                            1.dp,
                            if (isSelected) NeonCyan else Color(0xFF2D2942),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onPresetSelected(preset) }
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                        .testTag("preset_chip_${preset.id}")
                ) {
                    Column {
                        Text(
                            text = preset.name,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                            color = if (isSelected) Color.White else TextPrimary
                        )
                        if (preset.isCustom) {
                            Text(
                                text = "CUSTOM",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) FlatAmber else TextMuted
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = selectedPreset.description,
            fontSize = 11.sp,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun RecordControlBar(
    isRecording: Boolean,
    isPaused: Boolean,
    recordingDurationSec: Int,
    onStartRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onStopRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    val minutes = recordingDurationSec / 60
    val seconds = recordingDurationSec % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(StudioCard)
            .border(
                1.dp,
                if (isRecording) {
                    if (isPaused) FlatAmber else SharpRed
                } else Color(0xFF2D2942),
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        if (isRecording) {
                            if (isPaused) FlatAmber else SharpRed
                        } else Color(0xFF64748B)
                    )
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = if (isRecording) {
                        if (isPaused) "RECORDING PAUSED" else "RECORDING (RAW + TUNED)"
                    } else "DUAL WAV RECORDER",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isRecording) {
                        if (isPaused) FlatAmber else SharpRed
                    } else TextPrimary
                )
                Text(
                    text = if (isRecording) "Saves Raw & Tuned WAVs • $formattedTime" else "Preserves original vocal for re-tuning",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (isRecording) {
                // Pause / Resume button
                IconButton(
                    onClick = {
                        if (isPaused) onResumeRecording() else onPauseRecording()
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isPaused) FlatAmber else Color(0xFF2E2745))
                        .testTag("record_pause_resume_button")
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume Recording" else "Pause Recording",
                        tint = if (isPaused) Color.Black else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Stop button
                ElevatedButton(
                    onClick = onStopRecording,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = SharpRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("record_stop_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "STOP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                ElevatedButton(
                    onClick = onStartRecording,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xFF3B0764),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("record_start_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FiberManualRecord,
                        contentDescription = null,
                        tint = SharpRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "RECORD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MusicalKeySelector(
    selectedKey: MusicalKey,
    onKeySelected: (MusicalKey) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "MUSICAL KEY (TONIC)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricViolet,
                letterSpacing = 0.5.sp
            )
            Text(
                text = selectedKey.displayName,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = NeonCyan
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MusicalKey.values().forEach { key ->
                val isSelected = key == selectedKey
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) NeonPurplePrimary else StudioCard)
                        .border(
                            1.dp,
                            if (isSelected) NeonCyan else Color(0xFF2D2942),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onKeySelected(key) }
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                        .testTag("key_chip_${key.name}")
                ) {
                    Text(
                        text = key.displayName,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                        color = if (isSelected) Color.White else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun ScaleTypeSelector(
    selectedScale: ScaleType,
    onScaleSelected: (ScaleType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "MUSICAL SCALE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = ElectricViolet,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScaleType.values().forEach { scale ->
                val isSelected = scale == selectedScale
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0xFF4C1D95) else StudioCard)
                        .border(
                            1.dp,
                            if (isSelected) NeonPurpleBright else Color(0xFF2D2942),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onScaleSelected(scale) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("scale_chip_${scale.name}")
                ) {
                    Text(
                        text = scale.displayName,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun StudioSlider(
    label: String,
    value: Float,
    displayValue: String,
    subLabel: String? = null,
    onValueChange: (Float) -> Unit,
    color: Color = NeonPurplePrimary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StudioCard)
            .border(1.dp, Color(0xFF2D2942), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (subLabel != null) {
                    Text(
                        text = subLabel,
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }
            Text(
                text = displayValue,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = Color(0xFF252136)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
        )
    }
}

@Composable
fun SavePresetDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var presetName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Save Custom Vocal Preset",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = "Save current Auto-Tune settings, EQ, Compressor, Reverb, and Delay parameters.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("Preset Name") },
                    placeholder = { Text("e.g. My Lead Vocal") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0xFF3E3660),
                        focusedLabelColor = NeonCyan
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (presetName.isNotBlank()) {
                        onSave(presetName.trim())
                    }
                },
                enabled = presetName.isNotBlank()
            ) {
                Text(text = "SAVE", color = NeonCyan, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "CANCEL", color = TextSecondary)
            }
        },
        containerColor = StudioCardElevated
    )
}

@Composable
fun StudioRackQuickToggles(
    isBypass: Boolean,
    onToggleBypass: () -> Unit,
    isLiveMonitoring: Boolean,
    onToggleMonitoring: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Bypass Toggle Card
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isBypass) Color(0xFF451A03) else StudioCard)
                .border(
                    1.dp,
                    if (isBypass) FlatAmber else Color(0xFF2D2942),
                    RoundedCornerShape(12.dp)
                )
                .clickable { onToggleBypass() }
                .padding(12.dp)
                .testTag("toggle_bypass_button")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "BYPASS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isBypass) FlatAmber else TextSecondary
                    )
                    Text(
                        text = if (isBypass) "Raw Voice" else "Auto-Tuned",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
                Switch(
                    checked = isBypass,
                    onCheckedChange = { onToggleBypass() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = FlatAmber,
                        checkedTrackColor = Color(0xFF78350F)
                    )
                )
            }
        }

        // Live Monitor Toggle Card
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isLiveMonitoring) Color(0xFF064E3B) else StudioCard)
                .border(
                    1.dp,
                    if (isLiveMonitoring) InTuneGreen else Color(0xFF2D2942),
                    RoundedCornerShape(12.dp)
                )
                .clickable { onToggleMonitoring() }
                .padding(12.dp)
                .testTag("toggle_monitor_button")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MONITOR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLiveMonitoring) InTuneGreen else TextSecondary
                    )
                    Text(
                        text = if (isLiveMonitoring) "Hear in Audio" else "Muted",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
                Switch(
                    checked = isLiveMonitoring,
                    onCheckedChange = { onToggleMonitoring() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = InTuneGreen,
                        checkedTrackColor = Color(0xFF047857)
                    )
                )
            }
        }
    }
}
