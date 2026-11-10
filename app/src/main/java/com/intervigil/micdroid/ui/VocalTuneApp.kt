package com.intervigil.micdroid.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.intervigil.micdroid.ui.screens.LibraryScreen
import com.intervigil.micdroid.ui.screens.StudioScreen
import com.intervigil.micdroid.ui.theme.ElectricViolet
import com.intervigil.micdroid.ui.theme.FlatAmber
import com.intervigil.micdroid.ui.theme.InTuneGreen
import com.intervigil.micdroid.ui.theme.NeonCyan
import com.intervigil.micdroid.ui.theme.NeonPurpleBright
import com.intervigil.micdroid.ui.theme.NeonPurplePrimary
import com.intervigil.micdroid.ui.theme.SharpRed
import com.intervigil.micdroid.ui.theme.StudioCardElevated
import com.intervigil.micdroid.ui.theme.StudioDarkBackground
import com.intervigil.micdroid.ui.theme.StudioSurface
import com.intervigil.micdroid.ui.theme.TextMuted
import com.intervigil.micdroid.ui.theme.TextPrimary
import com.intervigil.micdroid.ui.theme.TextSecondary

enum class AppTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    STUDIO("Studio", Icons.Default.Tune),
    LIBRARY("Recordings", Icons.Default.LibraryMusic)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocalTuneApp(
    viewModel: VocalTuneViewModel = viewModel()
) {
    var currentTab by remember { mutableStateOf(AppTab.STUDIO) }
    var showHelpDialog by remember { mutableStateOf(false) }

    val audioState by viewModel.audioState.collectAsState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioDarkBackground),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (audioState.isEngineRunning) InTuneGreen else Color(0xFF64748B))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "VOCALTUNE STUDIO",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            color = TextPrimary
                        )
                    }
                },
                actions = {
                    if (audioState.isRecording) {
                        val isPaused = audioState.isRecordingPaused
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isPaused) FlatAmber.copy(alpha = 0.2f) else SharpRed.copy(alpha = 0.2f))
                                .border(1.dp, if (isPaused) FlatAmber else SharpRed, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isPaused) "⏸ PAUSED" else "● REC",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPaused) FlatAmber else SharpRed
                            )
                        }
                    }

                    IconButton(
                        onClick = { showHelpDialog = true },
                        modifier = Modifier.testTag("help_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Help & Guide",
                            tint = ElectricViolet
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StudioSurface,
                    titleContentColor = TextPrimary
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = StudioSurface,
                modifier = Modifier.navigationBarsPadding()
            ) {
                AppTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) NeonCyan else TextMuted
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextMuted
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color(0xFF2E1065)
                        ),
                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.STUDIO -> StudioScreen(viewModel = viewModel)
                AppTab.LIBRARY -> LibraryScreen(viewModel = viewModel)
            }
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Text(
                    text = "VocalTune Studio Guide",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "1. Wear Headphones: When Live Monitoring is ON, use wired or Bluetooth headphones to prevent acoustic feedback loop between speaker and microphone.\n\n" +
                                "2. Select Key & Scale: Set the key (e.g. C, D, G#) and scale (Major / Minor) to match the song you are singing.\n\n" +
                                "3. Auto-Tune Strength: Set to 80-100% with Fast speed for the popular modern T-Pain / Travis Scott robotic effect. Lower to 40-60% with Slow speed for natural pitch correction.\n\n" +
                                "4. Lossless WAV Recording: Tap the record button to capture high-definition pitch-corrected audio directly to your device.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text(text = "GOT IT", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = StudioCardElevated
        )
    }
}
