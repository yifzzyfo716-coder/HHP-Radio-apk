package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.ConnectionStatus
import com.example.model.ServerProfile
import com.example.network.HhpLocalStationServer
import com.example.ui.RadioViewModel
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
fun ServerConfigScreen(
    viewModel: RadioViewModel
) {
    val profiles by viewModel.serverProfiles.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val lastError by viewModel.lastError.collectAsStateWithLifecycle()
    val isHostingStation by viewModel.isHostingStation.collectAsStateWithLifecycle()
    val pingMs by viewModel.pingMs.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    val localIp = remember { HhpLocalStationServer.getLocalIpAddress() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(RadioNavyDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "การเชื่อมต่อเซิร์ฟเวอร์",
                        color = RadioTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "จัดการโฮสต์ เครือข่าย และโปรไฟล์การเชื่อมต่อ",
                        color = RadioTextSecondary,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RadioCyanHighlight,
                        contentColor = RadioNavyDark
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_server_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "เพิ่มเซิร์ฟเวอร์", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Active Connection Status Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = RadioNavyCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "สถานะปัจจุบัน",
                            color = RadioTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
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
                                text = when (connectionStatus) {
                                    ConnectionStatus.CONNECTED -> stringResource(R.string.status_connected)
                                    ConnectionStatus.CONNECTING -> stringResource(R.string.status_connecting)
                                    ConnectionStatus.ERROR -> stringResource(R.string.status_error)
                                    else -> if (activeProfile == null) stringResource(R.string.status_no_server) else stringResource(R.string.status_disconnected)
                                },
                                color = when (connectionStatus) {
                                    ConnectionStatus.CONNECTED -> RadioTXGreen
                                    ConnectionStatus.CONNECTING -> RadioRXAmber
                                    ConnectionStatus.ERROR -> RadioBusyRed
                                    else -> RadioTextDim
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (activeProfile != null) {
                        Text(
                            text = activeProfile!!.name,
                            color = RadioTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${activeProfile!!.host}:${activeProfile!!.port} (${activeProfile!!.callsign})",
                            color = RadioCyanHighlight,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        if (pingMs > 0 && connectionStatus == ConnectionStatus.CONNECTED) {
                            Text(
                                text = "ความหน่วงเครือข่าย (Latency): ${pingMs} ms",
                                color = RadioTextDim,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        Text(
                            text = "ยังไม่ได้เชื่อมต่อไปยังเซิร์ฟเวอร์ใด",
                            color = RadioTextDim,
                            fontSize = 14.sp
                        )
                    }

                    if (lastError != null && connectionStatus == ConnectionStatus.ERROR) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(RadioBusyRed.copy(alpha = 0.15f))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = RadioBusyRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = lastError!!,
                                    color = RadioBusyRed,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (connectionStatus == ConnectionStatus.CONNECTED) {
                            Button(
                                onClick = { viewModel.disconnectServer() },
                                colors = ButtonDefaults.buttonColors(containerColor = RadioBusyRed),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("disconnect_server_btn")
                            ) {
                                Icon(imageVector = Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "ตัดการเชื่อมต่อ", fontWeight = FontWeight.Bold)
                            }
                        } else if (activeProfile != null) {
                            Button(
                                onClick = { viewModel.connectServer(activeProfile!!) },
                                colors = ButtonDefaults.buttonColors(containerColor = RadioCyanHighlight, contentColor = RadioNavyDark),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("reconnect_server_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "เชื่อมต่อใหม่", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Embedded Local Station Host Card (Run local PTT server!)
        item {
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Router,
                                contentDescription = null,
                                tint = RadioCyanHighlight,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "เปิดสถานีแม่ข่าย (Local Station Host)",
                                    color = RadioTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "ให้อุปกรณ์นี้ทำหน้าที่เป็นเซิร์ฟเวอร์วิทยุในวง Wi-Fi/Hotspot",
                                    color = RadioTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Switch(
                            checked = isHostingStation,
                            onCheckedChange = { viewModel.toggleHostLocalStation() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = RadioCyanHighlight,
                                checkedTrackColor = RadioNavyElevated
                            ),
                            modifier = Modifier.testTag("host_local_station_switch")
                        )
                    }

                    if (isHostingStation) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(RadioTXGreen.copy(alpha = 0.15f))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "สถานีแม่ข่ายเปิดทำงานแล้ว (Active)",
                                    color = RadioTXGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "ให้อุปกรณ์เครื่องอื่นเชื่อมต่อมาที่ IP: $localIp พอร์ต: 8765 เพื่อสื่อสารร่วมกัน",
                                    color = RadioTextPrimary,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // Saved Profiles Section
        item {
            Text(
                text = "โปรไฟล์เซิร์ฟเวอร์ที่บันทึกไว้",
                color = RadioTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(profiles) { profile ->
            ServerProfileCard(
                profile = profile,
                isActive = activeProfile?.id == profile.id && connectionStatus == ConnectionStatus.CONNECTED,
                onConnect = { viewModel.connectServer(profile) },
                onDelete = { viewModel.deleteServerProfile(profile.id) }
            )
        }
    }

    if (showAddDialog) {
        AddServerDialog(
            onDismiss = { showAddDialog = false },
            onSave = { newProfile ->
                viewModel.saveServerProfile(newProfile)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ServerProfileCard(
    profile: ServerProfile,
    isActive: Boolean,
    onConnect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = RadioNavyCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) RadioCyanHighlight else RadioNavyElevated,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.name,
                        color = RadioTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (profile.isLocalStationHost) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(RadioCyanHighlight.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = "LOCAL", color = RadioCyanHighlight, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(
                    text = "${profile.host}:${profile.port}",
                    color = RadioTextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "สัญญาณเรียกขาน: ${profile.callsign} (${profile.username})",
                    color = RadioTextDim,
                    fontSize = 11.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isActive) {
                    Text(
                        text = "กำลังใช้",
                        color = RadioTXGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                } else {
                    Button(
                        onClick = onConnect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RadioCyanHighlight,
                            contentColor = RadioNavyDark
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("connect_profile_${profile.id}")
                    ) {
                        Text(text = "เชื่อมต่อ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = RadioTextDim,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddServerDialog(
    onDismiss: () -> Unit,
    onSave: (ServerProfile) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("127.0.0.1") }
    var portText by remember { mutableStateOf("8765") }
    var username by remember { mutableStateOf("Operator-1") }
    var callsign by remember { mutableStateOf("HHP-01") }
    var authToken by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RadioNavyCard,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "เพิ่มเซิร์ฟเวอร์วิทยุใหม่",
                color = RadioTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (errorMessage != null) {
                    Text(text = errorMessage!!, color = RadioBusyRed, fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("ชื่อเซิร์ฟเวอร์ (เช่น หน่วยปฏิบัติการ)", color = RadioTextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RadioCyanHighlight,
                        unfocusedBorderColor = RadioNavyElevated,
                        focusedTextColor = RadioTextPrimary,
                        unfocusedTextColor = RadioTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_server_name")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("IP หรือ Domain", color = RadioTextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RadioCyanHighlight,
                            unfocusedBorderColor = RadioNavyElevated,
                            focusedTextColor = RadioTextPrimary,
                            unfocusedTextColor = RadioTextPrimary
                        ),
                        modifier = Modifier.weight(2f).testTag("input_server_host")
                    )

                    OutlinedTextField(
                        value = portText,
                        onValueChange = { portText = it },
                        label = { Text("พอร์ต", color = RadioTextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RadioCyanHighlight,
                            unfocusedBorderColor = RadioNavyElevated,
                            focusedTextColor = RadioTextPrimary,
                            unfocusedTextColor = RadioTextPrimary
                        ),
                        modifier = Modifier.weight(1f).testTag("input_server_port")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = callsign,
                        onValueChange = { callsign = it },
                        label = { Text("สัญญาณเรียกขาน (Callsign)", color = RadioTextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RadioCyanHighlight,
                            unfocusedBorderColor = RadioNavyElevated,
                            focusedTextColor = RadioTextPrimary,
                            unfocusedTextColor = RadioTextPrimary
                        ),
                        modifier = Modifier.weight(1f).testTag("input_callsign")
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("ชื่อผู้ใช้งาน", color = RadioTextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RadioCyanHighlight,
                            unfocusedBorderColor = RadioNavyElevated,
                            focusedTextColor = RadioTextPrimary,
                            unfocusedTextColor = RadioTextPrimary
                        ),
                        modifier = Modifier.weight(1f).testTag("input_username")
                    )
                }

                OutlinedTextField(
                    value = authToken,
                    onValueChange = { authToken = it },
                    label = { Text("รหัสผ่านหรือ Token (ถ้ามี)", color = RadioTextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RadioCyanHighlight,
                        unfocusedBorderColor = RadioNavyElevated,
                        focusedTextColor = RadioTextPrimary,
                        unfocusedTextColor = RadioTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_token")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val port = portText.toIntOrNull()
                    if (name.isBlank()) {
                        errorMessage = "กรุณาระบุชื่อเซิร์ฟเวอร์"
                    } else if (host.isBlank()) {
                        errorMessage = "กรุณาระบุ IP หรือ Domain"
                    } else if (port == null || port <= 0 || port > 65535) {
                        errorMessage = "พอร์ตต้องอยู่ระหว่าง 1 - 65535"
                    } else {
                        onSave(
                            ServerProfile(
                                name = name.trim(),
                                host = host.trim(),
                                port = port,
                                username = username.trim(),
                                callsign = callsign.trim(),
                                authToken = authToken.trim(),
                                isLocalStationHost = host == "127.0.0.1" || host == "localhost"
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = RadioCyanHighlight,
                    contentColor = RadioNavyDark
                ),
                modifier = Modifier.testTag("save_server_btn")
            ) {
                Text(text = "บันทึก", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "ยกเลิก", color = RadioTextSecondary)
            }
        }
    )
}
