package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.RadioCyanHighlight
import com.example.ui.theme.RadioNavyCard
import com.example.ui.theme.RadioNavyDark
import com.example.ui.theme.RadioNavyElevated
import com.example.ui.theme.RadioTextPrimary
import com.example.ui.theme.RadioTextSecondary

@Composable
fun MicrophonePermissionExplanationDialog(
    isPermanentlyDenied: Boolean,
    onDismiss: () -> Unit,
    onConfirmRequest: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RadioNavyCard,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Icon(
                imageVector = if (isPermanentlyDenied) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = null,
                tint = if (isPermanentlyDenied) MaterialTheme.colorScheme.error else RadioCyanHighlight,
                modifier = Modifier.size(44.dp)
            )
        },
        title = {
            Text(
                text = if (isPermanentlyDenied) {
                    stringResource(R.string.permission_permanently_denied_title)
                } else {
                    stringResource(R.string.permission_mic_title)
                },
                color = RadioTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = if (isPermanentlyDenied) {
                    stringResource(R.string.permission_permanently_denied_desc)
                } else {
                    stringResource(R.string.permission_mic_desc)
                },
                color = RadioTextSecondary,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Start
            )
        },
        confirmButton = {
            if (isPermanentlyDenied) {
                Button(
                    onClick = {
                        openAppSettings(context)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RadioCyanHighlight,
                        contentColor = RadioNavyDark
                    ),
                    modifier = Modifier.testTag("open_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.permission_open_settings),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Button(
                    onClick = onConfirmRequest,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RadioCyanHighlight,
                        contentColor = RadioNavyDark
                    ),
                    modifier = Modifier.testTag("confirm_permission_button")
                ) {
                    Text(
                        text = stringResource(R.string.permission_allow_button),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_permission_button")
            ) {
                Text(
                    text = stringResource(R.string.permission_deny_button),
                    color = RadioTextSecondary
                )
            }
        }
    )
}

fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
