package com.intervigil.micdroid.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intervigil.micdroid.dsp.MusicalKey
import com.intervigil.micdroid.dsp.ScaleType
import com.intervigil.micdroid.model.SavedRecording
import com.intervigil.micdroid.ui.VocalTuneViewModel
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
import com.intervigil.micdroid.ui.theme.StudioDarkBackground
import com.intervigil.micdroid.ui.theme.TextMuted
import com.intervigil.micdroid.ui.theme.TextPrimary
import com.intervigil.micdroid.ui.theme.TextSecondary

@Composable
fun LibraryScreen(
    viewModel: VocalTuneViewModel,
    modifier: Modifier = Modifier
) {
    val recordings by viewModel.recordings.collectAsState()
    val playingFile by viewModel.playingFile.collectAsState()
    val playbackProgress by viewModel.playbackProgress.collectAsState()
    val isPlayingRaw by viewModel.isPlayingRawComparison.collectAsState()
    val currentRecording by viewModel.currentPlayingRecording.collectAsState()

    val isOfflineProcessing by viewModel.isOfflineProcessing.collectAsState()
    val offlineProgress by viewModel.offlineProgress.collectAsState()

    var recordingToDelete by remember { mutableStateOf<SavedRecording?>(null) }
    var recordingToRetune by remember { mutableStateOf<SavedRecording?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioDarkBackground)
            .padding(16.dp)
    ) {
        // Library Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "RECORDED CLIPS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${recordings.size} vocal recordings • Dual-archived",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            IconButton(
                onClick = { viewModel.loadRecordings() },
                modifier = Modifier.testTag("refresh_recordings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = NeonPurpleBright
                )
            }
        }

        // Offline Re-tuning progress banner
        if (isOfflineProcessing) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF3B0764))
                    .border(1.dp, NeonCyan, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = NeonCyan,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "DSP RE-TUNING IN PROGRESS...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "${(offlineProgress * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { offlineProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = NeonCyan,
                        trackColor = Color(0xFF1E1035)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (recordings.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(StudioCard)
                        .border(1.dp, Color(0xFF2D2942), RoundedCornerShape(16.dp))
                        .padding(28.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF26203D)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = ElectricViolet,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "No Recordings Found",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Switch to the Studio tab, start the engine, and press Record to save auto-tuned vocals with raw archives.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(recordings, key = { it.file.absolutePath }) { recording ->
                    val isPlayingThis = currentRecording?.file?.absolutePath == recording.file.absolutePath

                    RecordingCard(
                        recording = recording,
                        isPlaying = isPlayingThis,
                        isPlayingRaw = isPlayingThis && isPlayingRaw,
                        playbackProgress = if (isPlayingThis) playbackProgress else 0f,
                        onPlayToggle = { viewModel.playRecording(recording, playRaw = false) },
                        onToggleComparison = { viewModel.toggleComparisonPlayback() },
                        onRetune = { recordingToRetune = recording },
                        onShare = { viewModel.shareRecording(recording) },
                        onDelete = { recordingToDelete = recording }
                    )
                }
            }
        }
    }

    // Confirmation dialog for delete
    recordingToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { recordingToDelete = null },
            title = {
                Text(
                    text = "Delete Recording?",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Delete '${target.name}.wav' and its raw audio archive?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRecording(target)
                        recordingToDelete = null
                    }
                ) {
                    Text(text = "DELETE", color = SharpRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordingToDelete = null }) {
                    Text(text = "CANCEL", color = TextSecondary)
                }
            },
            containerColor = StudioCardElevated
        )
    }

    // Re-Tune Sheet Dialog
    recordingToRetune?.let { target ->
        RetuneVocalDialog(
            recording = target,
            onDismiss = { recordingToRetune = null },
            onRetune = { key, scale, strength, retuneSpeed ->
                recordingToRetune = null
                viewModel.retuneRecording(
                    recording = target,
                    newKey = key,
                    newScale = scale,
                    newStrength = strength,
                    newRetuneSpeed = retuneSpeed
                )
            }
        )
    }
}

@Composable
fun RecordingCard(
    recording: SavedRecording,
    isPlaying: Boolean,
    isPlayingRaw: Boolean,
    playbackProgress: Float,
    onPlayToggle: () -> Unit,
    onToggleComparison: () -> Unit,
    onRetune: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isPlaying) Color(0xFF261D40) else StudioCard)
            .border(
                1.dp,
                if (isPlaying) {
                    if (isPlayingRaw) FlatAmber else NeonPurpleBright
                } else Color(0xFF2D2942),
                RoundedCornerShape(14.dp)
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPlaying) {
                                if (isPlayingRaw) FlatAmber else NeonPurplePrimary
                            } else Color(0xFF28233C)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onPlayToggle,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = if (isPlaying && isPlayingRaw) Color.Black else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = recording.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${recording.durationFormatted} • ${recording.sizeFormatted}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = NeonCyan
                        )
                        if (recording.hasRawVersion) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF1E293B))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "RAW ARCHIVE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRetune) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = "Re-Tune Vocal",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = ElectricViolet,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Active Playback Controls: A/B comparison and progress bar
        if (isPlaying) {
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { playbackProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (isPlayingRaw) FlatAmber else NeonCyan,
                trackColor = Color(0xFF3B2E5C)
            )

            if (recording.hasRawVersion) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isPlayingRaw) "LISTENING TO: ORIGINAL RAW" else "LISTENING TO: AUTO-TUNED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPlayingRaw) FlatAmber else NeonCyan
                    )

                    ElevatedButton(
                        onClick = onToggleComparison,
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = if (isPlayingRaw) Color(0xFF451A03) else Color(0xFF3B0764),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CompareArrows,
                            contentDescription = null,
                            tint = if (isPlayingRaw) FlatAmber else NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPlayingRaw) "HEAR TUNED" else "HEAR RAW (A/B)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RetuneVocalDialog(
    recording: SavedRecording,
    onDismiss: () -> Unit,
    onRetune: (MusicalKey, ScaleType, Float, Float) -> Unit
) {
    var selectedKey by remember { mutableStateOf(MusicalKey.C) }
    var selectedScale by remember { mutableStateOf(ScaleType.MAJOR) }
    var strength by remember { mutableStateOf(0.90f) }
    var retuneSpeed by remember { mutableStateOf(0.15f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoFixHigh,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Re-Tune Vocal Clip",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Apply real-time DSP pitch correction to '${recording.name}' with a new Key, Scale, and Auto-Tune strength.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                // Key Picker
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "TARGET KEY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricViolet
                        )
                        Text(
                            text = selectedKey.displayName,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MusicalKey.values().forEach { key ->
                            val isSel = key == selectedKey
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) NeonPurplePrimary else StudioCard)
                                    .border(1.dp, if (isSel) NeonCyan else Color(0xFF2D2942), RoundedCornerShape(8.dp))
                                    .clickable { selectedKey = key }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = key.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) Color.White else TextSecondary
                                )
                            }
                        }
                    }
                }

                // Scale Picker
                Column {
                    Text(
                        text = "SCALE TYPE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricViolet
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ScaleType.values().forEach { scale ->
                            val isSel = scale == selectedScale
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Color(0xFF4C1D95) else StudioCard)
                                    .border(1.dp, if (isSel) NeonPurpleBright else Color(0xFF2D2942), RoundedCornerShape(8.dp))
                                    .clickable { selectedScale = scale }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = scale.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) Color.White else TextSecondary
                                )
                            }
                        }
                    }
                }

                // Strength Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "CORRECTION STRENGTH",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${(strength * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = NeonPurplePrimary
                        )
                    }
                    Slider(
                        value = strength,
                        onValueChange = { strength = it },
                        colors = SliderDefaults.colors(
                            thumbColor = NeonPurplePrimary,
                            activeTrackColor = NeonPurplePrimary,
                            inactiveTrackColor = Color(0xFF252136)
                        )
                    )
                }
            }
        },
        confirmButton = {
            ElevatedButton(
                onClick = { onRetune(selectedKey, selectedScale, strength, retuneSpeed) },
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = NeonPurplePrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "RENDER TUNED WAV", fontWeight = FontWeight.Bold)
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
