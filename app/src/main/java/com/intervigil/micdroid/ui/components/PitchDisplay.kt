package com.intervigil.micdroid.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intervigil.micdroid.ui.theme.ElectricViolet
import com.intervigil.micdroid.ui.theme.FlatAmber
import com.intervigil.micdroid.ui.theme.InTuneGreen
import com.intervigil.micdroid.ui.theme.NeonCyan
import com.intervigil.micdroid.ui.theme.NeonPink
import com.intervigil.micdroid.ui.theme.NeonPurpleBright
import com.intervigil.micdroid.ui.theme.SharpRed
import com.intervigil.micdroid.ui.theme.StudioCard
import com.intervigil.micdroid.ui.theme.StudioCardElevated
import com.intervigil.micdroid.ui.theme.TextMuted
import com.intervigil.micdroid.ui.theme.TextPrimary
import com.intervigil.micdroid.ui.theme.TextSecondary
import kotlin.math.abs

@Composable
fun PitchDisplay(
    pitchHz: Float,
    targetNote: String,
    octave: Int,
    targetHz: Float,
    centsOffset: Float,
    isVoiced: Boolean,
    modifier: Modifier = Modifier
) {
    val isInTune = isVoiced && abs(centsOffset) < 6f && pitchHz > 50f

    val statusColor by animateColorAsState(
        targetValue = when {
            !isVoiced -> Color(0xFF64748B)
            isInTune -> InTuneGreen
            centsOffset < 0 -> FlatAmber
            else -> SharpRed
        },
        label = "statusColor"
    )

    val animatedCents by animateFloatAsState(
        targetValue = if (isVoiced) centsOffset else 0f,
        animationSpec = tween(durationMillis = 80),
        label = "centsOffsetAnim"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1E1A2E),
                        Color(0xFF13101E)
                    )
                )
            )
            .border(1.5.dp, if (isInTune) InTuneGreen.copy(alpha = 0.6f) else Color(0xFF332D4E), RoundedCornerShape(18.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top header with status badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = ElectricViolet,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "PITCH DETECTOR & TUNER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricViolet,
                    letterSpacing = 1.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusColor.copy(alpha = 0.2f))
                    .border(1.dp, statusColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = when {
                        !isVoiced -> "LISTENING..."
                        isInTune -> "★ IN TUNE"
                        centsOffset < 0 -> "FLAT ♭"
                        else -> "SHARP ♯"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = statusColor
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Large Note Display
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isVoiced && targetNote != "--") targetNote else "--",
                fontSize = 58.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = if (isInTune) InTuneGreen else TextPrimary,
                letterSpacing = (-1).sp
            )

            if (isVoiced && octave > 0) {
                Text(
                    text = octave.toString(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = NeonPurpleBright,
                    modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
                )
            }
        }

        // Frequency readouts
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isVoiced && pitchHz > 40f) String.format("%.1f Hz", pitchHz) else "0.0 Hz",
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )

            if (isVoiced && targetHz > 40f) {
                Text(
                    text = " → ",
                    fontSize = 14.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
                Text(
                    text = String.format("Target: %.1f Hz", targetHz),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    color = NeonCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Cents Deviation Meter (-50 to +50)
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "-50¢", fontSize = 10.sp, color = FlatAmber, fontWeight = FontWeight.Bold)
                Text(
                    text = if (isVoiced) String.format("%+.0f¢", centsOffset) else "0¢",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                Text(text = "+50¢", fontSize = 10.sp, color = SharpRed, fontWeight = FontWeight.Bold)
            }

            // Meter Track with center tick & needle
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .height(16.dp)
            ) {
                val trackHeight = 6.dp.toPx()
                val topY = (size.height - trackHeight) / 2
                val centerX = size.width / 2

                // Background track
                drawRoundRect(
                    color = Color(0xFF1E1C2B),
                    topLeft = Offset(0f, topY),
                    size = Size(size.width, trackHeight),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )

                // Safe in-tune center zone (±6 cents)
                val inTuneZoneWidth = (size.width * (12f / 100f))
                drawRoundRect(
                    color = InTuneGreen.copy(alpha = 0.25f),
                    topLeft = Offset(centerX - inTuneZoneWidth / 2, topY),
                    size = Size(inTuneZoneWidth, trackHeight),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )

                // Center zero tick
                drawLine(
                    color = InTuneGreen,
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, size.height),
                    strokeWidth = 2.dp.toPx()
                )

                // Indicator needle
                if (isVoiced) {
                    val normalizedOffset = (animatedCents / 50f).coerceIn(-1f, 1f)
                    val needleX = centerX + (normalizedOffset * (size.width / 2))

                    // Draw needle bar
                    drawRoundRect(
                        color = statusColor,
                        topLeft = Offset(needleX - 2.5.dp.toPx(), 1.dp.toPx()),
                        size = Size(5.dp.toPx(), size.height - 2.dp.toPx()),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }
            }
        }
    }
}
