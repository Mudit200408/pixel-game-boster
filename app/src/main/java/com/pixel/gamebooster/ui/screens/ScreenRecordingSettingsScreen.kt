package com.pixel.gamebooster.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Hd
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixel.gamebooster.R
import com.pixel.gamebooster.data.model.RecordAudioSource
import com.pixel.gamebooster.data.model.RecordResolution
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.ui.components.FloatingBackTitleToolbar
import com.pixel.gamebooster.ui.core.cards.IconToggleItem
import com.pixel.gamebooster.ui.core.containers.RoundedCardContainer
import com.pixel.gamebooster.ui.theme.GoogleSansFlexRounded
import com.pixel.gamebooster.ui.util.rememberAdaptiveLayoutInfo
import com.pixel.gamebooster.utils.HapticUtil

/**
 * Dedicated Screen Recording Settings screen.
 * Formatted to match Pixel Game Booster and Essentials design language:
 * - Clean content layout with no top app bar, title or description
 * - Floating bottom navigation toolbar with back navigation and title pill
 * - Sectional RoundedCardContainer and IconToggleItem components
 * - ExposedDropdownMenuBox cards with rounded corners and dynamic Monet tinting
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScreenRecordingSettingsScreen(
    repo: SettingsRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val recordAudioSource by repo.recordAudioSource.collectAsState()
    val recordResolution by repo.recordResolution.collectAsState()
    val recordShowTouches by repo.recordShowTouches.collectAsState()
    val recordDoubleTapStop by repo.recordDoubleTapStop.collectAsState()

    val adaptive = rememberAdaptiveLayoutInfo()
    val bottomPadding = if (adaptive.isCompactHeight) 80.dp else 104.dp

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Section 1: Audio Capture Source
            Text(
                text = stringResource(R.string.screen_recording_section_audio),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp),
            )

            var audioDropdownExpanded by remember { mutableStateOf(false) }
            val currentAudio = RecordAudioSource.fromValue(recordAudioSource)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.screen_recording_audio_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = GoogleSansFlexRounded,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    ExposedDropdownMenuBox(
                        expanded = audioDropdownExpanded,
                        onExpandedChange = { audioDropdownExpanded = it },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = when (currentAudio) {
                                        RecordAudioSource.MUTE -> Icons.AutoMirrored.Rounded.VolumeOff
                                        RecordAudioSource.MIC -> Icons.Rounded.Mic
                                        RecordAudioSource.DEVICE -> Icons.AutoMirrored.Rounded.VolumeUp
                                        RecordAudioSource.BOTH -> Icons.Rounded.RecordVoiceOver
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = currentAudio.label,
                                    style = TextStyle(
                                        fontFamily = GoogleSansFlexRounded,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = when (currentAudio) {
                                        RecordAudioSource.MUTE -> "Captures video only with no sound"
                                        RecordAudioSource.MIC -> "Records external voice via microphone"
                                        RecordAudioSource.DEVICE -> "Captures internal game soundtrack"
                                        RecordAudioSource.BOTH -> "Mixes internal game sound & mic"
                                    },
                                    style = TextStyle(
                                        fontFamily = GoogleSansFlexRounded,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 12.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = audioDropdownExpanded)
                        }

                        ExposedDropdownMenu(
                            expanded = audioDropdownExpanded,
                            onDismissRequest = { audioDropdownExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            shape = RoundedCornerShape(20.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = null,
                        ) {
                            RecordAudioSource.entries.forEach { source ->
                                val isSelected = source.value == recordAudioSource
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = source.label,
                                                style = TextStyle(
                                                    fontFamily = GoogleSansFlexRounded,
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                    fontSize = 15.sp,
                                                ),
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            )
                                            Text(
                                                text = when (source) {
                                                    RecordAudioSource.MUTE -> "Captures video only with no sound"
                                                    RecordAudioSource.MIC -> "Records external voice via microphone"
                                                    RecordAudioSource.DEVICE -> "Captures internal game soundtrack directly"
                                                    RecordAudioSource.BOTH -> "Simultaneously mixes internal game sound & mic"
                                                },
                                                style = TextStyle(
                                                    fontFamily = GoogleSansFlexRounded,
                                                    fontWeight = FontWeight.Normal,
                                                    fontSize = 12.sp,
                                                ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = when (source) {
                                                RecordAudioSource.MUTE -> Icons.AutoMirrored.Rounded.VolumeOff
                                                RecordAudioSource.MIC -> Icons.Rounded.Mic
                                                RecordAudioSource.DEVICE -> Icons.AutoMirrored.Rounded.VolumeUp
                                                RecordAudioSource.BOTH -> Icons.Rounded.RecordVoiceOver
                                            },
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    },
                                    trailingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    } else null,
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        repo.setRecordAudioSource(source.value)
                                        audioDropdownExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Video Resolution & Framerate
            Text(
                text = stringResource(R.string.screen_recording_section_video),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp),
            )

            var resolutionDropdownExpanded by remember { mutableStateOf(false) }
            val currentResolution = RecordResolution.fromValue(recordResolution)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.screen_recording_resolution_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = GoogleSansFlexRounded,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    ExposedDropdownMenuBox(
                        expanded = resolutionDropdownExpanded,
                        onExpandedChange = { resolutionDropdownExpanded = it },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = when (currentResolution) {
                                        RecordResolution.FHD_60 -> Icons.Rounded.Videocam
                                        RecordResolution.FHD_30 -> Icons.Rounded.Hd
                                        RecordResolution.HD_60 -> Icons.Rounded.Speed
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = currentResolution.label,
                                    style = TextStyle(
                                        fontFamily = GoogleSansFlexRounded,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = when (currentResolution) {
                                        RecordResolution.FHD_60 -> "1920x1080 @ 60 FPS • 16 Mbps high quality"
                                        RecordResolution.FHD_30 -> "1920x1080 @ 30 FPS • 10 Mbps balanced"
                                        RecordResolution.HD_60 -> "1280x720 @ 60 FPS • 8 Mbps performance"
                                    },
                                    style = TextStyle(
                                        fontFamily = GoogleSansFlexRounded,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 12.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = resolutionDropdownExpanded)
                        }

                        ExposedDropdownMenu(
                            expanded = resolutionDropdownExpanded,
                            onDismissRequest = { resolutionDropdownExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            shape = RoundedCornerShape(20.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = null,
                        ) {
                            RecordResolution.entries.forEach { res ->
                                val isSelected = res.value == recordResolution
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = res.label,
                                                style = TextStyle(
                                                    fontFamily = GoogleSansFlexRounded,
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                    fontSize = 15.sp,
                                                ),
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            )
                                            Text(
                                                text = when (res) {
                                                    RecordResolution.FHD_60 -> "1920x1080 @ 60 FPS • 16 Mbps high quality"
                                                    RecordResolution.FHD_30 -> "1920x1080 @ 30 FPS • 10 Mbps balanced"
                                                    RecordResolution.HD_60 -> "1280x720 @ 60 FPS • 8 Mbps performance"
                                                },
                                                style = TextStyle(
                                                    fontFamily = GoogleSansFlexRounded,
                                                    fontWeight = FontWeight.Normal,
                                                    fontSize = 12.sp,
                                                ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = when (res) {
                                                RecordResolution.FHD_60 -> Icons.Rounded.Videocam
                                                RecordResolution.FHD_30 -> Icons.Rounded.Hd
                                                RecordResolution.HD_60 -> Icons.Rounded.Speed
                                            },
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    },
                                    trailingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    } else null,
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        repo.setRecordResolution(res.value)
                                        resolutionDropdownExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Recording Options Switch Tiles
            Text(
                text = stringResource(R.string.screen_recording_section_options),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp),
            )

            RoundedCardContainer {
                IconToggleItem(
                    title = stringResource(R.string.screen_recording_double_tap_title),
                    subtitle = stringResource(R.string.screen_recording_double_tap_desc),
                    icon = Icons.Rounded.StopCircle,
                    checked = recordDoubleTapStop,
                    onCheckedChange = { repo.setRecordDoubleTapStop(it) },
                    index = 0,
                    count = 2,
                )
                IconToggleItem(
                    title = stringResource(R.string.screen_recording_show_touches_title),
                    subtitle = stringResource(R.string.screen_recording_show_touches_desc),
                    icon = Icons.Rounded.TouchApp,
                    checked = recordShowTouches,
                    onCheckedChange = { repo.setRecordShowTouches(it) },
                    index = 1,
                    count = 2,
                )
            }
        }

        // Reusable Floating Bottom Navigation Bar
        FloatingBackTitleToolbar(
            title = stringResource(R.string.screen_recording_title),
            onBack = onBack,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
