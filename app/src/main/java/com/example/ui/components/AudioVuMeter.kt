package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RadioBusyRed
import com.example.ui.theme.RadioCyanHighlight
import com.example.ui.theme.RadioNavyCard
import com.example.ui.theme.RadioNavyDark
import com.example.ui.theme.RadioRXAmber
import com.example.ui.theme.RadioTXGreen
import com.example.ui.theme.RadioTextDim
import com.example.ui.theme.RadioTextSecondary

@Composable
fun AudioVuMeter(
    label: String,
    level: Float, // 0.0f to 1.0f
    isTx: Boolean = true,
    modifier: Modifier = Modifier
) {
    val animatedLevel by animateFloatAsState(
        targetValue = level.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 60),
        label = "vu_meter"
    )

    val segmentsCount = 16
    val activeSegments = (animatedLevel * segmentsCount).toInt()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RadioNavyDark.copy(alpha = 0.8f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = if (isTx) RadioTXGreen else RadioRXAmber,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            val db = if (animatedLevel > 0.001f) {
                "${(20 * kotlin.math.log10(animatedLevel.toDouble())).toInt()} dB"
            } else {
                "-INF"
            }
            Text(
                text = db,
                color = RadioTextDim,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            for (i in 0 until segmentsCount) {
                val isActive = i < activeSegments
                val segmentColor = when {
                    i >= 13 -> RadioBusyRed
                    i >= 9 -> RadioRXAmber
                    else -> RadioTXGreen
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (isActive) segmentColor else segmentColor.copy(alpha = 0.15f)
                        )
                )
            }
        }
    }
}
