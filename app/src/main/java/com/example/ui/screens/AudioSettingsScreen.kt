package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.AudioRoute
import com.example.model.AudioSettingsData
import com.example.ui.RadioViewModel
import com.example.ui.components.AudioVuMeter
import com.example.ui.theme.RadioBusyRed
import com.example.ui.theme.RadioCyanHighlight
import com.example.ui.theme.RadioNavyCard
import com.example.ui.theme.RadioNavyDark
import com.example.ui.theme.RadioNavyElevated
import com.example.ui.theme.RadioRXAmber
import com.example.ui.theme.RadioTXGreen
import com.example.ui.theme.RadioTextDim
import com.example.ui.theme.RadioTextPrimary
import com.example.ui.theme.RadioTextSecondary

@Composable
fun AudioSettingsScreen(
    viewModel: RadioViewModel,
    onRequestMicPermission: () -> Unit
) {
    val hasMicPermission by viewModel.hasMicPermission.collectAsStateWithLifecycle()
    val audioSettings by viewModel.audioSettings.collectAsStateWithLifecycle()
    val currentRoute by viewModel.currentRoute.collectAsStateWithLifecycle()
    val isTestingMic by viewModel.isTestingMic.collectAsStateWithLifecycle()
    val txLevel by viewModel.txAudioLevel.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            if (isTestingMic) {
                viewModel.stopMicTest()
            }
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RadioNavyDark)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "การตั้งค่าระบบเสียง (Audio)",
                color = RadioTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "ปรับแต่งไมโครโฟน ลำโพง การตัดเสียงรบกวน และเส้นทางสัญญาณเสียง",
                color = RadioTextSecondary,
                fontSize = 12.sp
            )
        }

        // Permission Card
        Card(
            colors = CardDefaults.cardColors(containerColor = RadioNavyCard),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (hasMicPermission) RadioTXGreen.copy(alpha = 0.15f)
                                else RadioBusyRed.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (hasMicPermission) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = null,
                            tint = if (hasMicPermission) RadioTXGreen else RadioBusyRed,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "สิทธิ์การเข้าถึงไมโครโฟน",
                            color = RadioTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (hasMicPermission) stringResource(R.string.permission_status_granted)
                            else stringResource(R.string.permission_status_denied),
                            color = if (hasMicPermission) RadioTXGreen else RadioBusyRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (!hasMicPermission) {
                    Button(
                        onClick = onRequestMicPermission,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RadioCyanHighlight,
                            contentColor = RadioNavyDark
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("request_mic_perm_button")
                    ) {
                        Text(text = "ขอสิทธิ์", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Interactive Live Microphone Test Card
        Card(
            colors = CardDefaults.cardColors(containerColor = RadioNavyCard),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, RadioCyanHighlight.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ทดสอบการรับเสียงไมโครโฟน (Mic Test)",
                            color = RadioTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "พูดใส่ไมโครโฟนเพื่อตรวจระดับสัญญาณและความชัดเจน",
                            color = RadioTextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (isTestingMic) {
                                viewModel.stopMicTest()
                            } else {
                                if (!hasMicPermission) {
                                    onRequestMicPermission()
                                } else {
                                    viewModel.startMicTest()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTestingMic) RadioBusyRed else RadioCyanHighlight,
                            contentColor = RadioNavyDark
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("toggle_mic_test_btn")
                    ) {
                        Icon(
                            imageVector = if (isTestingMic) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isTestingMic) "หยุดทดสอบ" else "เริ่มทดสอบ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Real Live VU Meter
                AudioVuMeter(
                    label = if (isTestingMic) "MIC INPUT LEVEL (TESTING)" else "MIC STANDBY",
                    level = if (isTestingMic) txLevel else 0f,
                    isTx = true
                )
            }
        }

        // Audio Output Routing Card
        Card(
            colors = CardDefaults.cardColors(containerColor = RadioNavyCard),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "เส้นทางอุปกรณ์เสียง (Audio Routing)",
                    color = RadioTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "เลือกอุปกรณ์ส่งออกเสียงลำโพงหรือชุดหูฟัง",
                    color = RadioTextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                AudioRouteSelector(
                    route = AudioRoute.SPEAKER,
                    title = "ลำโพงภายนอก (Speakerphone)",
                    icon = Icons.Default.VolumeUp,
                    isSelected = currentRoute == AudioRoute.SPEAKER,
                    onSelect = {
                        viewModel.updateAudioSettings(audioSettings.copy(audioRoute = AudioRoute.SPEAKER))
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                AudioRouteSelector(
                    route = AudioRoute.EARPIECE,
                    title = "ลำโพงแนบหู (Earpiece Receiver)",
                    icon = Icons.Default.Hearing,
                    isSelected = currentRoute == AudioRoute.EARPIECE,
                    onSelect = {
                        viewModel.updateAudioSettings(audioSettings.copy(audioRoute = AudioRoute.EARPIECE))
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                AudioRouteSelector(
                    route = AudioRoute.BLUETOOTH,
                    title = "ชุดหูฟังบลูทูธ (Bluetooth SCO / PTT)",
                    icon = Icons.Default.Bluetooth,
                    isSelected = currentRoute == AudioRoute.BLUETOOTH,
                    onSelect = {
                        viewModel.updateAudioSettings(audioSettings.copy(audioRoute = AudioRoute.BLUETOOTH))
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                AudioRouteSelector(
                    route = AudioRoute.WIRED_HEADSET,
                    title = "ชุดหูฟังมีสาย (Wired Headset / Mic)",
                    icon = Icons.Default.Headphones,
                    isSelected = currentRoute == AudioRoute.WIRED_HEADSET,
                    onSelect = {
                        viewModel.updateAudioSettings(audioSettings.copy(audioRoute = AudioRoute.WIRED_HEADSET))
                    }
                )
            }
        }

        // Gain Controls & Enhancements Card
        Card(
            colors = CardDefaults.cardColors(containerColor = RadioNavyCard),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "การประมวลผลและการปรับเกนเสียง (DSP)",
                    color = RadioTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Input Gain Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "ระดับเกนไมโครโฟนส่ง (TX Gain)", color = RadioTextSecondary, fontSize = 12.sp)
                    Text(
                        text = "${(audioSettings.inputGain * 100).toInt()}%",
                        color = RadioCyanHighlight,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = audioSettings.inputGain,
                    onValueChange = {
                        viewModel.updateAudioSettings(audioSettings.copy(inputGain = it))
                    },
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = RadioCyanHighlight,
                        activeTrackColor = RadioCyanHighlight,
                        inactiveTrackColor = RadioNavyElevated
                    ),
                    modifier = Modifier.testTag("tx_gain_slider")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Output Volume Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "ระดับเสียงขับลำโพงรับ (RX Boost)", color = RadioTextSecondary, fontSize = 12.sp)
                    Text(
                        text = "${(audioSettings.outputVolume * 100).toInt()}%",
                        color = RadioCyanHighlight,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = audioSettings.outputVolume,
                    onValueChange = {
                        viewModel.updateAudioSettings(audioSettings.copy(outputVolume = it))
                    },
                    valueRange = 0.2f..1.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = RadioCyanHighlight,
                        activeTrackColor = RadioCyanHighlight,
                        inactiveTrackColor = RadioNavyElevated
                    ),
                    modifier = Modifier.testTag("rx_boost_slider")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Audio DSP Toggles
                DspToggleRow(
                    title = "ตัดเสียงสะท้อน (Acoustic Echo Cancellation)",
                    subtitle = "ป้องกันเสียงสะท้อนกลับจากลำโพงเข้าไมโครโฟน",
                    checked = audioSettings.echoCancellation,
                    onCheckedChange = {
                        viewModel.updateAudioSettings(audioSettings.copy(echoCancellation = it))
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                DspToggleRow(
                    title = "ลดเสียงรบกวนรอบข้าง (Noise Suppression)",
                    subtitle = "กรองเสียงลม เสียงเครื่องยนต์ และเสียงสภาพแวดล้อม",
                    checked = audioSettings.noiseSuppression,
                    onCheckedChange = {
                        viewModel.updateAudioSettings(audioSettings.copy(noiseSuppression = it))
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                DspToggleRow(
                    title = "ปรับระดับเสียงอัตโนมัติ (Auto Gain Control)",
                    subtitle = "รักษาระดับเสียงพูดให้สม่ำเสมอแม้พูดเบาหรือห่างไมค์",
                    checked = audioSettings.autoGainControl,
                    onCheckedChange = {
                        viewModel.updateAudioSettings(audioSettings.copy(autoGainControl = it))
                    }
                )
            }
        }
    }
}

@Composable
private fun AudioRouteSelector(
    route: AudioRoute,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) RadioNavyElevated else RadioNavyDark)
            .border(
                1.dp,
                if (isSelected) RadioCyanHighlight else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .clickable { onSelect() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) RadioCyanHighlight else RadioTextDim,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    color = if (isSelected) RadioCyanHighlight else RadioTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(RadioCyanHighlight)
                )
            }
        }
    }
}

@Composable
private fun DspToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = RadioTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = RadioTextDim, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = RadioCyanHighlight,
                checkedTrackColor = RadioNavyElevated
            )
        )
    }
}
