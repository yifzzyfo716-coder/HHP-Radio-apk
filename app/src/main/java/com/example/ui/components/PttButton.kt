package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.PttState
import com.example.ui.theme.RadioBusyRed
import com.example.ui.theme.RadioCyanHighlight
import com.example.ui.theme.RadioNavyCard
import com.example.ui.theme.RadioNavyDark
import com.example.ui.theme.RadioNavyElevated
import com.example.ui.theme.RadioRXAmber
import com.example.ui.theme.RadioTXGreen
import com.example.ui.theme.RadioTextDim
import com.example.ui.theme.RadioTextPrimary

@Composable
fun PttButton(
    pttState: PttState,
    isToggleMode: Boolean,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    onToggleClick: () -> Unit,
    hasMicPermission: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (pttState == PttState.TRANSMITTING) 1.08f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val primaryColor by animateColorAsState(
        targetValue = when (pttState) {
            PttState.READY -> RadioCyanHighlight
            PttState.PRESSED -> RadioRXAmber
            PttState.TRANSMITTING -> RadioTXGreen
            PttState.RECEIVING -> RadioRXAmber
            PttState.BUSY -> RadioBusyRed
            PttState.ERROR -> if (!hasMicPermission) RadioRXAmber else RadioBusyRed
        },
        animationSpec = tween(150),
        label = "ptt_color"
    )

    val statusText = when (pttState) {
        PttState.READY -> if (isToggleMode) stringResource(R.string.ptt_tap_to_toggle) else stringResource(R.string.ptt_ready)
        PttState.PRESSED -> stringResource(R.string.ptt_pressed)
        PttState.TRANSMITTING -> stringResource(R.string.ptt_transmitting)
        PttState.RECEIVING -> stringResource(R.string.ptt_receiving)
        PttState.BUSY -> stringResource(R.string.ptt_busy)
        PttState.ERROR -> if (!hasMicPermission) stringResource(R.string.ptt_mic_permission_needed) else stringResource(R.string.ptt_error)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(200.dp)
                .scale(pulseScale)
        ) {
            // Outer glow ring
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                primaryColor.copy(alpha = if (pttState == PttState.TRANSMITTING) 0.35f else 0.12f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Outer metallic rim
            Box(
                modifier = Modifier
                    .size(165.dp)
                    .clip(CircleShape)
                    .background(RadioNavyElevated)
                    .border(3.dp, primaryColor, CircleShape)
            )

            // Inner tactile button surface
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(145.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                RadioNavyCard,
                                RadioNavyDark
                            )
                        )
                    )
                    .border(
                        1.5.dp,
                        primaryColor.copy(alpha = 0.6f),
                        CircleShape
                    )
                    .testTag("ptt_button")
                    .pointerInput(isToggleMode, pttState) {
                        if (isToggleMode) {
                            detectTapGestures {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleClick()
                            }
                        } else {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                down.consume()
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onPressStart()

                                val up = waitForUpOrCancellation()
                                up?.consume()
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onPressEnd()
                            }
                        }
                    }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = when (pttState) {
                            PttState.TRANSMITTING -> Icons.Default.Mic
                            PttState.BUSY, PttState.ERROR -> Icons.Default.MicOff
                            else -> Icons.Default.Mic
                        },
                        contentDescription = "Push To Talk",
                        tint = primaryColor,
                        modifier = Modifier.size(46.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "PTT",
                        color = primaryColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = if (isToggleMode) "TOGGLE" else "HOLD",
                        color = RadioTextDim,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = statusText,
            color = primaryColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}
