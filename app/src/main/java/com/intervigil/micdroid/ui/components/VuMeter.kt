package com.intervigil.micdroid.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.intervigil.micdroid.ui.theme.MeterGreen
import com.intervigil.micdroid.ui.theme.MeterRed
import com.intervigil.micdroid.ui.theme.MeterYellow
import com.intervigil.micdroid.ui.theme.StudioCard
import com.intervigil.micdroid.ui.theme.TextMuted
import com.intervigil.micdroid.ui.theme.TextSecondary

@Composable
fun VuMeter(
    levelRms: Float,
    levelDb: Float,
    isVoiced: Boolean,
    modifier: Modifier = Modifier
) {
    // Smooth level animation
    val animatedLevel by animateFloatAsState(
        targetValue = (levelRms * 3.5f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 60),
        label = "vuMeterLevel"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StudioCard)
            .border(1.dp, Color(0xFF2D2942), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isVoiced) Color(0xFF10B981) else Color(0xFF475569))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = if (isVoiced) "VOICE" else "MIC IN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    text = "LIVE AUDIO LEVEL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    letterSpacing = 0.8.sp
                )
            }

            Text(
                text = String.format("%.1f dB", levelDb),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (levelDb > -6f) MeterRed else if (levelDb > -18f) MeterYellow else MeterGreen
            )
        }

        // Multi-segment LED Bar
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(10.dp)
        ) {
            val totalSegments = 28
            val segmentSpacing = 3.dp.toPx()
            val totalSpacing = segmentSpacing * (totalSegments - 1)
            val segmentWidth = (size.width - totalSpacing) / totalSegments
            val activeSegments = (animatedLevel * totalSegments).toInt().coerceIn(0, totalSegments)

            for (i in 0 until totalSegments) {
                val x = i * (segmentWidth + segmentSpacing)
                val isActive = i < activeSegments

                val segmentColor = when {
                    i >= totalSegments - 4 -> MeterRed // High / Clipping
                    i >= totalSegments - 10 -> MeterYellow // Medium / Strong
                    else -> MeterGreen // Normal
                }

                val color = if (isActive) segmentColor else segmentColor.copy(alpha = 0.15f)

                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, 0f),
                    size = Size(segmentWidth, size.height),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }
        }
    }
}
