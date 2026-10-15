package com.example.ui.screens

import android.view.KeyEvent
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.KeyBindingSettings
import com.example.ui.RadioViewModel
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
fun KeySettingsScreen(
    viewModel: RadioViewModel
) {
    val keyBindings by viewModel.keyBindings.collectAsStateWithLifecycle()
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
                text = "ตั้งค่าปุ่มกดและคีย์ลัด PTT",
                color = RadioTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "กำหนดปุ่มลัดฮาร์ดแวร์ ปุ่มบนหูฟัง และลักษณะการกดส่งเสียง",
                color = RadioTextSecondary,
                fontSize = 12.sp
            )
        }

        // PTT Interaction Mode Card
        Card(
            colors = CardDefaults.cardColors(containerColor = RadioNavyCard),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "พฤติกรรมการกดปุ่ม PTT (Interaction Mode)",
                    color = RadioTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "เลือกว่าจะใช้แบบกดค้างแล้วพูด หรือแตะครั้งเดียวเพื่อสลับ",
                    color = RadioTextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ModeOptionCard(
                        title = "กดค้างเพื่อส่งเสียง (Hold to Talk)",
                        description = "ปล่อยปุ่มเพื่อหยุดส่งเสียงทันที (มาตรฐานวิทยุ)",
                        isSelected = !keyBindings.isToggleMode,
                        modifier = Modifier.weight(1f),
                        onSelect = {
                            viewModel.updateKeyBindings(keyBindings.copy(isToggleMode = false))
                        }
                    )

                    ModeOptionCard(
                        title = "แตะสลับเปิด/ปิด (Toggle Mode)",
                        description = "แตะหนึ่งครั้งเพื่อเริ่ม และแตะซ้ำเพื่อหยุด",
                        isSelected = keyBindings.isToggleMode,
                        modifier = Modifier.weight(1f),
                        onSelect = {
                            viewModel.updateKeyBindings(keyBindings.copy(isToggleMode = true))
                        }
                    )
                }
            }
        }

        // Hardware Key Assignment Card
        Card(
            colors = CardDefaults.cardColors(containerColor = RadioNavyCard),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ปุ่มฮาร์ดแวร์ภายนอก (Physical PTT Key)",
                    color = RadioTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ปุ่มที่สามารถกดส่งสัญญาณวิทยุได้แม้ปิดหน้าจอหรือในแอป",
                    color = RadioTextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                KeySelectRow(
                    title = "ปุ่มลดเสียง (Volume Down)",
                    subtitle = "แนะนำ: เข้าถึงง่ายและรองรับบน Android เกือบทุกรุ่น",
                    isSelected = keyBindings.pttKeyCode == KeyEvent.KEYCODE_VOLUME_DOWN,
                    onSelect = {
                        viewModel.updateKeyBindings(keyBindings.copy(pttKeyCode = KeyEvent.KEYCODE_VOLUME_DOWN))
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                KeySelectRow(
                    title = "ปุ่มเพิ่มเสียง (Volume Up)",
                    subtitle = "เหมาะสำหรับผู้ที่ถนัดใช้นิ้วชี้หรือปุ่มบน",
                    isSelected = keyBindings.pttKeyCode == KeyEvent.KEYCODE_VOLUME_UP,
                    onSelect = {
                        viewModel.updateKeyBindings(keyBindings.copy(pttKeyCode = KeyEvent.KEYCODE_VOLUME_UP))
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                KeySelectRow(
                    title = "ปุ่มชุดหูฟัง / ไมค์สาย (Headset Hook / PTT)",
                    subtitle = "รองรับปุ่มกดบนสายสมอลทอล์คและไมค์หนีบเสื้อ",
                    isSelected = keyBindings.pttKeyCode == KeyEvent.KEYCODE_HEADSETHOOK,
                    onSelect = {
                        viewModel.updateKeyBindings(keyBindings.copy(pttKeyCode = KeyEvent.KEYCODE_HEADSETHOOK))
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "เปิดใช้ปุ่มปรับระดับเสียงเป็น PTT",
                            color = RadioTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "ดักจับสัญญาณปุ่ม Volume ขณะหน้าจอวิทยุเปิดอยู่",
                            color = RadioTextDim,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = keyBindings.allowVolumeKeyPtt,
                        onCheckedChange = {
                            viewModel.updateKeyBindings(keyBindings.copy(allowVolumeKeyPtt = it))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = RadioCyanHighlight,
                            checkedTrackColor = RadioNavyElevated
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "เปิดใช้ปุ่มหูฟังบลูทูธและสาย (Media / Hook)",
                            color = RadioTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "รองรับไมค์ PTT วิทยุสื่อสารไร้สายระบบบลูทูธ",
                            color = RadioTextDim,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = keyBindings.headsetHookEnabled,
                        onCheckedChange = {
                            viewModel.updateKeyBindings(keyBindings.copy(headsetHookEnabled = it))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = RadioCyanHighlight,
                            checkedTrackColor = RadioNavyElevated
                        )
                    )
                }
            }
        }

        // Hardware limitations note
        Card(
            colors = CardDefaults.cardColors(containerColor = RadioNavyElevated.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = RadioCyanHighlight,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "ข้อจำกัดฮาร์ดแวร์ Android: ปุ่ม Power และปุ่มสแกนนิ้วมือเป็นสิทธิ์ความปลอดภัยของระบบ ไม่สามารถนำมาเป็นปุ่ม PTT ได้ การใช้ปุ่ม Volume ทำงานได้สมบูรณ์แบบเมื่อหน้าจอแอปทำงานอยู่",
                    color = RadioTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        // Reset Defaults Button
        OutlinedButton(
            onClick = {
                viewModel.updateKeyBindings(KeyBindingSettings())
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = RadioCyanHighlight),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reset_keys_btn")
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "คืนค่าปุ่มกดเริ่มต้นทั้งหมด", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ModeOptionCard(
    title: String,
    description: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) RadioNavyElevated else RadioNavyDark)
            .border(
                1.5.dp,
                if (isSelected) RadioCyanHighlight else RadioNavyElevated,
                RoundedCornerShape(10.dp)
            )
            .clickable { onSelect() }
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = if (isSelected) RadioCyanHighlight else RadioTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(RadioCyanHighlight)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = RadioTextDim,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun KeySelectRow(
    title: String,
    subtitle: String,
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isSelected) RadioCyanHighlight else RadioTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = RadioTextDim,
                    fontSize = 11.sp
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = RadioCyanHighlight,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
