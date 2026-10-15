package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.ConnectionStatus
import com.example.model.PttState
import com.example.ui.RadioViewModel
import com.example.ui.components.AudioVuMeter
import com.example.ui.components.PttButton
import com.example.ui.components.RadioLcdDisplay
import com.example.ui.theme.RadioBusyRed
import com.example.ui.theme.RadioCyanHighlight
import com.example.ui.theme.RadioNavyCard
import com.example.ui.theme.RadioNavyDark
import com.example.ui.theme.RadioNavyElevated
import com.example.ui.theme.RadioNavySurface
import com.example.ui.theme.RadioRXAmber
import com.example.ui.theme.RadioTXGreen
import com.example.ui.theme.RadioTextDim
import com.example.ui.theme.RadioTextPrimary
import com.example.ui.theme.RadioTextSecondary

@Composable
fun MainRadioScreen(
    viewModel: RadioViewModel,
    onRequestMicPermission: () -> Unit,
    onNavigateToChannels: () -> Unit,
    onNavigateToServers: () -> Unit,
    onNavigateToAudio: () -> Unit,
    onNavigateToKeys: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val pttState by viewModel.pttState.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val serverName by viewModel.serverDisplayName.collectAsStateWithLifecycle()
    val lastError by viewModel.lastError.collectAsStateWithLifecycle()
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val currentChannelId by viewModel.currentChannelId.collectAsStateWithLifecycle()
    val activeSpeaker by viewModel.activeSpeaker.collectAsStateWithLifecycle()
    val pingMs by viewModel.pingMs.collectAsStateWithLifecycle()
    val hasMicPermission by viewModel.hasMicPermission.collectAsStateWithLifecycle()
    val txLevel by viewModel.txAudioLevel.collectAsStateWithLifecycle()
    val rxLevel by viewModel.rxAudioLevel.collectAsStateWithLifecycle()
    val audioRoute by viewModel.currentRoute.collectAsStateWithLifecycle()
    val keyBindings by viewModel.keyBindings.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()

    val currentChannel = channels.find { it.id == currentChannelId }
    val displayFrequency = currentChannel?.frequencyMhz ?: "144.150 MHz"
    val displayChannelName = currentChannel?.name ?: "CH-01 ช่องเรียกขานหลัก"
    val displayChannelDesc = currentChannel?.description ?: "ความถี่มาตรฐานสำหรับประสานงานทั่วไป"

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RadioNavyDark)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(RadioCyanHighlight.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = "HHP Radio",
                        tint = RadioCyanHighlight,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        color = RadioTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "VHF/UHF IP TRANSCEIVER",
                        color = RadioTextDim,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Connection Status Pill
            val (statusText, statusBg, statusColor) = when (connectionStatus) {
                ConnectionStatus.CONNECTED -> Triple(stringResource(R.string.status_connected), RadioTXGreen.copy(alpha = 0.2f), RadioTXGreen)
                ConnectionStatus.CONNECTING -> Triple(stringResource(R.string.status_connecting), RadioRXAmber.copy(alpha = 0.2f), RadioRXAmber)
                ConnectionStatus.ERROR -> Triple(stringResource(R.string.status_error), RadioBusyRed.copy(alpha = 0.2f), RadioBusyRed)
                ConnectionStatus.DISCONNECTED -> {
                    if (activeProfile == null) {
                        Triple(stringResource(R.string.status_no_server), RadioNavyElevated, RadioTextDim)
                    } else {
                        Triple(stringResource(R.string.status_disconnected), RadioNavyElevated, RadioTextSecondary)
                    }
                }
                ConnectionStatus.RECONNECTING -> Triple("กำลังต่อใหม่…", RadioRXAmber.copy(alpha = 0.2f), RadioRXAmber)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(statusBg)
                    .clickable { onNavigateToServers() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("connection_status_badge")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Microphone Permission Banner (if not granted)
        if (!hasMicPermission) {
            Card(
                colors = CardDefaults.cardColors(containerColor = RadioBusyRed.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRequestMicPermission() }
                    .testTag("mic_permission_banner")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MicOff,
                        contentDescription = null,
                        tint = RadioBusyRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ยังไม่ได้รับสิทธิ์ไมโครโฟน",
                            color = RadioTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "แตะเพื่อดูคำชี้แจงและขอสิทธิ์ใช้งานเสียงพูด",
                            color = RadioTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Connection Error Card (if any)
        if (connectionStatus == ConnectionStatus.ERROR && lastError != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = RadioNavyElevated),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = RadioBusyRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = lastError ?: "",
                        color = RadioBusyRed,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Tactical LCD Display
        RadioLcdDisplay(
            frequency = displayFrequency,
            channelName = displayChannelName,
            channelDescription = displayChannelDesc,
            serverName = if (connectionStatus == ConnectionStatus.CONNECTED) serverName else (activeProfile?.name ?: ""),
            connectionStatus = connectionStatus,
            pttState = pttState,
            activeSpeaker = activeSpeaker,
            pingMs = pingMs,
            audioRoute = audioRoute
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Audio VU Meter (shows TX when transmitting, RX when receiving)
        if (pttState == PttState.TRANSMITTING) {
            AudioVuMeter(
                label = "TX MIC MODULATION",
                level = txLevel,
                isTx = true
            )
            Spacer(modifier = Modifier.height(14.dp))
        } else if (pttState == PttState.RECEIVING || rxLevel > 0.05f) {
            AudioVuMeter(
                label = "RX SPEAKER LEVEL",
                level = rxLevel,
                isTx = false
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Main Tactile Push-to-Talk Button
        PttButton(
            pttState = pttState,
            isToggleMode = keyBindings.isToggleMode,
            onPressStart = {
                if (!hasMicPermission) {
                    onRequestMicPermission()
                } else if (connectionStatus != ConnectionStatus.CONNECTED) {
                    onNavigateToServers()
                } else {
                    viewModel.startPtt()
                }
            },
            onPressEnd = {
                viewModel.stopPtt()
            },
            onToggleClick = {
                if (!hasMicPermission) {
                    onRequestMicPermission()
                } else if (connectionStatus != ConnectionStatus.CONNECTED) {
                    onNavigateToServers()
                } else {
                    viewModel.togglePtt()
                }
            },
            hasMicPermission = hasMicPermission,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Function Action Buttons Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionButton(
                icon = Icons.Default.Tune,
                label = "เลือกช่อง",
                sublabel = displayChannelName.take(8),
                modifier = Modifier.weight(1f),
                onClick = onNavigateToChannels,
                testTag = "quick_channels_btn"
            )
            QuickActionButton(
                icon = Icons.Default.Dns,
                label = "เซิร์ฟเวอร์",
                sublabel = if (connectionStatus == ConnectionStatus.CONNECTED) "เชื่อมต่อ" else "ตั้งค่า",
                modifier = Modifier.weight(1f),
                onClick = onNavigateToServers,
                testTag = "quick_servers_btn"
            )
            QuickActionButton(
                icon = Icons.Default.GraphicEq,
                label = "ระบบเสียง",
                sublabel = audioRoute.name,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToAudio,
                testTag = "quick_audio_btn"
            )
            QuickActionButton(
                icon = Icons.Default.Keyboard,
                label = "ปุ่ม PTT",
                sublabel = if (keyBindings.isToggleMode) "สลับ" else "กดค้าง",
                modifier = Modifier.weight(1f),
                onClick = onNavigateToKeys,
                testTag = "quick_keys_btn"
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    sublabel: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = RadioNavyCard),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = RadioCyanHighlight,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = RadioTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = sublabel,
                color = RadioTextDim,
                fontSize = 9.sp,
                maxLines = 1
            )
        }
    }
}
