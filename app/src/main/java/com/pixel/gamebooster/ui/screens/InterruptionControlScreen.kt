package com.pixel.gamebooster.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhoneDisabled
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pixel.gamebooster.R
import com.pixel.gamebooster.data.model.GameProfile
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.interruption.CallScreeningHelper
import com.pixel.gamebooster.interruption.DNDManager
import com.pixel.gamebooster.ui.core.containers.RoundedCardContainer
import com.pixel.gamebooster.ui.core.pickers.SegmentedPicker
import com.pixel.gamebooster.ui.theme.GoogleSansFlexRounded
import com.pixel.gamebooster.utils.GameModeManager
import kotlinx.coroutines.launch

/**
 * UI configuration screen/section for Do Not Disturb policy and In-Game Call Handling (REQ-05, D-05, D-06, D-07).
 */
@Composable
fun InterruptionControlScreen(
    repo: SettingsRepository,
    scopedPackage: String? = null,
    scopedProfile: GameProfile? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentProfileState = if (scopedPackage != null) {
        val initial = scopedProfile ?: repo.createDefaultEffectiveProfile(scopedPackage)
        repo.observeEffectiveProfile(scopedPackage).collectAsState(initial = initial)
    } else null
    val currentProfile = currentProfileState?.value

    var isCallRoleHeld by remember {
        mutableStateOf(CallScreeningHelper.isCallScreeningRoleHeld(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isCallRoleHeld = CallScreeningHelper.isCallScreeningRoleHeld(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val roleRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        isCallRoleHeld = CallScreeningHelper.isCallScreeningRoleHeld(context)
    }

    val interruptionMode = currentProfile?.interruptionMode ?: 1
    val callHandlingMode = currentProfile?.callHandlingMode ?: 0

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 1. Interruption Tier Card: DND Policy
        RoundedCardContainer(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DoNotDisturbOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.interruption_dnd_policy_title),
                            style = TextStyle(
                                fontFamily = GoogleSansFlexRounded,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.interruption_dnd_policy_desc),
                            style = TextStyle(
                                fontFamily = GoogleSansFlexRounded,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                SegmentedPicker(
                    options = listOf(0, 1, 2),
                    selectedOption = interruptionMode,
                    onOptionSelected = { newMode ->
                        if (currentProfile != null && scopedPackage != null) {
                            val updated = currentProfile.copy(customized = true, interruptionMode = newMode)
                            coroutineScope.launch {
                                repo.saveProfile(updated)
                            }
                            if (GameModeManager.isGameModeActive && GameModeManager.activeGamePackage == scopedPackage) {
                                if (newMode != 0) {
                                    DNDManager.applyInterruptionPolicy(context, newMode)
                                } else {
                                    DNDManager.restoreInterruptionPolicy(context)
                                }
                            }
                        }
                    },
                    labelProvider = { mode ->
                        when (mode) {
                            0 -> context.getString(R.string.dnd_mode_off)
                            1 -> context.getString(R.string.dnd_mode_priority)
                            2 -> context.getString(R.string.dnd_mode_full)
                            else -> context.getString(R.string.dnd_mode_priority)
                        }
                    },
                    iconProvider = { mode ->
                        when (mode) {
                            0 -> Icons.Rounded.Notifications
                            1 -> Icons.Rounded.Star
                            2 -> Icons.Rounded.DoNotDisturbOn
                            else -> null
                        }
                    },
                )

                Text(
                    text = when (interruptionMode) {
                        0 -> stringResource(R.string.dnd_mode_off_desc)
                        1 -> stringResource(R.string.dnd_mode_priority_desc)
                        2 -> stringResource(R.string.dnd_mode_full_desc)
                        else -> stringResource(R.string.dnd_mode_priority_desc)
                    },
                    style = TextStyle(
                        fontFamily = GoogleSansFlexRounded,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // 2. Call Handling Card: Floating, Silent, Auto-Reject
        RoundedCardContainer(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.call_handling_title),
                            style = TextStyle(
                                fontFamily = GoogleSansFlexRounded,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.call_handling_desc),
                            style = TextStyle(
                                fontFamily = GoogleSansFlexRounded,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                SegmentedPicker(
                    options = listOf(0, 1, 2),
                    selectedOption = callHandlingMode,
                    onOptionSelected = { newMode ->
                        if (currentProfile != null && scopedPackage != null) {
                            val updated = currentProfile.copy(customized = true, callHandlingMode = newMode)
                            coroutineScope.launch {
                                repo.saveProfile(updated)
                            }
                            if (GameModeManager.isGameModeActive && GameModeManager.activeGamePackage == scopedPackage) {
                                CallScreeningHelper.activeCallHandlingMode = newMode
                            }
                        }
                    },
                    labelProvider = { mode ->
                        when (mode) {
                            0 -> context.getString(R.string.call_handling_headsup_title)
                            1 -> context.getString(R.string.call_handling_silent_title)
                            2 -> context.getString(R.string.call_handling_reject_title)
                            else -> context.getString(R.string.call_handling_headsup_title)
                        }
                    },
                    iconProvider = { mode ->
                        when (mode) {
                            0 -> Icons.Rounded.PhoneInTalk
                            1 -> Icons.AutoMirrored.Rounded.VolumeOff
                            2 -> Icons.Rounded.PhoneDisabled
                            else -> null
                        }
                    },
                )

                Text(
                    text = when (callHandlingMode) {
                        0 -> stringResource(R.string.call_handling_headsup_desc)
                        1 -> stringResource(R.string.call_handling_silent_desc)
                        2 -> stringResource(R.string.call_handling_reject_desc)
                        else -> stringResource(R.string.call_handling_headsup_desc)
                    },
                    style = TextStyle(
                        fontFamily = GoogleSansFlexRounded,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // 3. Call Screening Role status card
        if (!isCallRoleHeld && (callHandlingMode == CallScreeningHelper.MODE_SILENT || callHandlingMode == CallScreeningHelper.MODE_REJECT)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            text = stringResource(R.string.call_screening_role_title),
                            style = TextStyle(
                                fontFamily = GoogleSansFlexRounded,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                            ),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                    Text(
                        text = stringResource(R.string.call_screening_role_desc),
                        style = TextStyle(
                            fontFamily = GoogleSansFlexRounded,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                        ),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Button(
                        onClick = {
                            val intent = CallScreeningHelper.getRequestRoleIntent(context)
                            roleRequestLauncher.launch(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.call_screening_role_grant_btn),
                            style = TextStyle(
                                fontFamily = GoogleSansFlexRounded,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                            ),
                        )
                    }
                }
            }
        } else if (isCallRoleHeld && callHandlingMode != CallScreeningHelper.MODE_HEADS_UP) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.call_screening_role_active),
                            style = TextStyle(
                                fontFamily = GoogleSansFlexRounded,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.call_screening_role_active_desc),
                            style = TextStyle(
                                fontFamily = GoogleSansFlexRounded,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
