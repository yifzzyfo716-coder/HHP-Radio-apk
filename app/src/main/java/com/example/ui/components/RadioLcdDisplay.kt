package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioRoute
import com.example.model.ConnectionStatus
import com.example.model.PttState
import com.example.ui.theme.RadioBusyRed
import com.example.ui.theme.RadioCyanHighlight
import com.example.ui.theme.RadioNavyDark
import com.example.ui.theme.RadioRXAmber
import com.example.ui.theme.RadioTXGreen
import com.example.ui.theme.RadioTextDim
import com.example.ui.theme.RadioTextPrimary
import com.example.ui.theme.RadioTextSecondary

@Composable
fun RadioLcdDisplay(
    frequency: String,
    channelName: String,
    channelDescription: String,
    serverName: String,
    connectionStatus: ConnectionStatus,
    pttState: PttState,
    activeSpeaker: String?,
    pingMs: Long,
    audioRoute: AudioRoute,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(RadioNavyDark)
            .border(2.dp, RadioCyanHighlight.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Top Bar: Server Name + Signal / Latency + Audio Route
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (connectionStatus) {
                                    ConnectionStatus.CONNECTED -> RadioTXGreen
                                    ConnectionStatus.CONNECTING -> RadioRXAmber
                                    ConnectionStatus.ERROR -> RadioBusyRed
                                    else -> RadioTextDim
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (serverName.isNotBlank()) serverName else "HHP RADIO NET",
                        color = RadioTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (connectionStatus == ConnectionStatus.CONNECTED && pingMs > 0) {
                        Text(
                            text = "${pingMs}ms",
                            color = RadioCyanHighlight,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }

                    Icon(
                        imageVector = when (audioRoute) {
                            AudioRoute.SPEAKER -> Icons.Default.VolumeUp
                            AudioRoute.EARPIECE, AudioRoute.WIRED_HEADSET, AudioRoute.BLUETOOTH -> Icons.Default.Headphones
                        },
                        contentDescription = "Audio Route",
                        tint = RadioCyanHighlight,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Frequency Digits Display (Tactical LCD Style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = frequency.ifBlank { "144.150 MHz" },
                        color = RadioCyanHighlight,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = channelName.ifBlank { "CH-01 ช่องเรียกขานหลัก" },
                        color = RadioTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Status Badge (TX / RX / IDLE)
                val badgeColor = when (pttState) {
                    PttState.TRANSMITTING -> RadioTXGreen
                    PttState.RECEIVING -> RadioRXAmber
                    PttState.BUSY -> RadioBusyRed
                    else -> RadioTextDim
                }
                val badgeText = when (pttState) {
                    PttState.TRANSMITTING -> "TX ON"
                    PttState.RECEIVING -> "RX ON"
                    PttState.BUSY -> "BUSY"
                    PttState.PRESSED -> "WAIT"
                    else -> "STBY"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor.copy(alpha = 0.2f))
                        .border(1.dp, badgeColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Active Speaker Banner
            AnimatedVisibility(
                visible = activeSpeaker != null || pttState == PttState.TRANSMITTING,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (pttState == PttState.TRANSMITTING) RadioTXGreen.copy(alpha = 0.15f)
                            else RadioRXAmber.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = if (pttState == PttState.TRANSMITTING) RadioTXGreen else RadioRXAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (pttState == PttState.TRANSMITTING) {
                            "คุณกำลังส่งสัญญาณเสียง (Transmitting)"
                        } else {
                            "กำลังรับฟังเสียงจาก: $activeSpeaker"
                        },
                        color = if (pttState == PttState.TRANSMITTING) RadioTXGreen else RadioRXAmber,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (channelDescription.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = channelDescription,
                    color = RadioTextDim,
                    fontSize = 12.sp
                )
            }
        }
    }
}
