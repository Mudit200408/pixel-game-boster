package com.pixel.gamebooster.ui.core.dialogs

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixel.gamebooster.utils.PermissionHelper

@Composable
fun ShizukuPairingDialog(
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Terminal,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        },
        title = {
            Text(
                text = "Shizuku Wireless Pairing",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Shizuku allows Pixel Game Booster to execute privileged system optimizations via Android's native Wireless Debugging without root.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Step 1: Developer Options
                PairingStepRow(
                    stepNumber = "1",
                    icon = Icons.Rounded.DeveloperMode,
                    title = "Enable Developer Options",
                    description = "Go to Settings > About Phone, and tap 'Build number' 7 times until unlocked.",
                )

                // Step 2: Wireless Debugging
                PairingStepRow(
                    stepNumber = "2",
                    icon = Icons.Rounded.Wifi,
                    title = "Enable Wireless Debugging",
                    description = "In Developer Options, turn ON 'Wireless Debugging' while connected to Wi-Fi.",
                )

                // Step 3: Pair Device
                PairingStepRow(
                    stepNumber = "3",
                    icon = Icons.Rounded.CheckCircle,
                    title = "Pair with 6-digit Code",
                    description = "Tap 'Pair device with pairing code' in Developer Options. Note the pairing code and port number.",
                )

                // Step 4: Start Shizuku
                PairingStepRow(
                    stepNumber = "4",
                    icon = Icons.AutoMirrored.Rounded.OpenInNew,
                    title = "Start Service in Shizuku",
                    description = "Open Shizuku app, tap 'Pairing' to enter the code, then tap 'Start' to activate the service.",
                )

                // Static ADB fallback command
                Text(
                    text = "Alternative: Computer USB ADB Command",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(12.dp),
                ) {
                    Text(
                        text = "adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    PermissionHelper.launchShizuku(context)
                    onDismissRequest()
                },
            ) {
                Text("Open Shizuku")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                        }
                    },
                ) {
                    Text("Dev Options")
                }
                TextButton(onClick = onDismissRequest) {
                    Text("Close")
                }
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}

@Composable
private fun PairingStepRow(
    stepNumber: String,
    icon: ImageVector,
    title: String,
    description: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
