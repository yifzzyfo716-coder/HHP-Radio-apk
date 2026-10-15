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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ConnectionStatus
import com.example.model.RadioChannel
import com.example.model.RadioUser
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
fun ChannelListScreen(
    viewModel: RadioViewModel,
    onChannelSelected: (String) -> Unit
) {
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val users by viewModel.users.collectAsStateWithLifecycle()
    val currentChannelId by viewModel.currentChannelId.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val activeSpeaker by viewModel.activeSpeaker.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(RadioNavyDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "ช่องสัญญาณวิทยุ (Channels)",
                color = RadioTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "เลือกช่องความถี่เพื่อรับฟังและส่งสัญญาณเสียงในกลุ่ม",
                color = RadioTextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (connectionStatus != ConnectionStatus.CONNECTED) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = RadioNavyCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = null,
                            tint = RadioCyanHighlight,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "ยังไม่ได้เชื่อมต่อเซิร์ฟเวอร์",
                            color = RadioTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "กรุณาไปที่หน้าเซิร์ฟเวอร์เพื่อเชื่อมต่อไปยังสถานีวิทยุหรือเปิดสถานีท้องถิ่น รายชื่อช่องและผู้ใช้งานจะแสดงเมื่อเชื่อมต่อกับเซิร์ฟเวอร์จริง",
                            color = RadioTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        } else if (channels.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = RadioNavyCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "กำลังโหลดช่องสัญญาณจากเซิร์ฟเวอร์…",
                            color = RadioTextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            items(channels) { channel ->
                val isSelected = channel.id == currentChannelId
                val channelUsers = users.filter { it.channelId == channel.id }

                ChannelCard(
                    channel = channel,
                    isSelected = isSelected,
                    users = channelUsers,
                    activeSpeaker = if (isSelected) activeSpeaker else null,
                    onSelect = {
                        viewModel.switchChannel(channel.id)
                        onChannelSelected(channel.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun ChannelCard(
    channel: RadioChannel,
    isSelected: Boolean,
    users: List<RadioUser>,
    activeSpeaker: String?,
    onSelect: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) RadioNavyElevated else RadioNavyCard
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) RadioCyanHighlight else RadioNavyElevated,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onSelect() }
            .testTag("channel_item_${channel.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = channel.name,
                            color = if (isSelected) RadioCyanHighlight else RadioTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Active Channel",
                                tint = RadioCyanHighlight,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = channel.frequencyMhz,
                        color = RadioTextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = RadioTextDim,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${users.size} คน",
                        color = RadioTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (channel.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = channel.description,
                    color = RadioTextDim,
                    fontSize = 12.sp
                )
            }

            // Active Speaker in this channel
            if (activeSpeaker != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(RadioRXAmber.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = RadioRXAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "กำลังพูด: $activeSpeaker",
                        color = RadioRXAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // User list chips in this channel
            if (users.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    users.take(5).forEach { u ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(RadioNavyDark)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${u.callsign} (${u.username})",
                                color = if (u.isSelf) RadioCyanHighlight else RadioTextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    if (users.size > 5) {
                        Text(
                            text = "+${users.size - 5}",
                            color = RadioTextDim,
                            fontSize = 10.sp,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }
        }
    }
}
