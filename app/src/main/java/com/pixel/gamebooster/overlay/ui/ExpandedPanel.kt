package com.pixel.gamebooster.overlay.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.ui.unit.DpSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource

import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixel.gamebooster.R
import com.pixel.gamebooster.overlay.OverlayViewModel
import com.pixel.gamebooster.overlay.model.OverlayUiState
import com.pixel.gamebooster.ui.theme.GoogleSansFlexRounded
import com.pixel.gamebooster.utils.HapticUtil
import kotlin.math.roundToInt

@Composable
fun ExpandedPanel(
    state: OverlayUiState,
    viewModel: OverlayViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val isRight = state.isDockedOnRight

    // Touch scrim outside the card to allow one-tap dismiss without obscuring or blurring the game
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = if (isRight) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        // Floating Frosted HUD Card docked on the respective side (left or right)
        Box(
            modifier = modifier
                .padding(
                    start = if (isRight) 0.dp else 24.dp,
                    end = if (isRight) 24.dp else 0.dp,
                    top = 16.dp,
                    bottom = 16.dp,
                )
                .width(348.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(28.dp),
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = false,
                ) {}
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 1. Real-time Telemetry Header: [60 FPS]  [🌡 38°C]  [⚡ 4.2W]  [⇄ Side]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.overlay_fps_badge, state.currentFps),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = GoogleSansFlexRounded,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Thermostat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "${state.temperatureC}°C",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = GoogleSansFlexRounded,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ElectricBolt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "${state.wattageW}W",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = GoogleSansFlexRounded,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        // Switch Side Button
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .clickable {
                                    val target = !isRight
                                    viewModel.setDockedOnRight(target)
                                    com.pixel.gamebooster.overlay.GameOverlayManager.snapToSide(target)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SwapHoriz,
                                contentDescription = "Switch Side",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }

                // 2. Segmented Pill Mode Switcher: [ Battery | Balanced | Boost ]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val modes = listOf(
                            3 to stringResource(R.string.overlay_mode_battery),
                            1 to stringResource(R.string.overlay_mode_balanced),
                            2 to stringResource(R.string.overlay_mode_boost),
                        )
                        modes.forEach { (mode, label) ->
                            val isSelected = state.interventionMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable {
                                        HapticUtil.performToggle(view)
                                        viewModel.setInterventionMode(mode)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = GoogleSansFlexRounded,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp,
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                // 3. Action Tiles Grid (3x2)
                // Row 1: [ Lock 120Hz ]  [ Game DND ]  [ Record ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    M3EActionTile(
                        icon = Icons.Rounded.Speed,
                        label = "Lock 120Hz",
                        isActive = state.lockRefreshRate,
                        onClick = { viewModel.toggle120Hz() },
                        modifier = Modifier.weight(1f),
                    )

                    M3EActionTile(
                        icon = Icons.Rounded.Bedtime,
                        label = stringResource(R.string.overlay_dnd_title),
                        isActive = state.dndEnabled,
                        onClick = { viewModel.toggleDnd() },
                        modifier = Modifier.weight(1f),
                    )

                    M3EActionTile(
                        icon = Icons.Rounded.Videocam,
                        label = stringResource(R.string.overlay_record),
                        isActive = state.isRecording,
                        onClick = { viewModel.toggleRecording() },
                        modifier = Modifier.weight(1f),
                    )
                }

                // Row 2: [ Brightness Lock ]  [ Net Boost ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    M3EActionTile(
                        icon = Icons.Rounded.LightMode,
                        label = stringResource(R.string.overlay_brightness_lock_title),
                        isActive = state.autoBrightnessLocked,
                        onClick = { viewModel.toggleAutoBrightness() },
                        modifier = Modifier.weight(1f),
                    )

                    M3EActionTile(
                        icon = Icons.Rounded.Bolt,
                        label = stringResource(R.string.overlay_net_boost_title),
                        isActive = state.netBoostEnabled,
                        onClick = { viewModel.toggleNetBoost() },
                        modifier = Modifier.weight(1f),
                    )
                }

                // 4. Expressive System Sliders: Brightness & Volume (Official Material 3 Expressive Design)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Brightness Slider
                    ExpressiveSystemSlider(
                        value = state.brightnessPercent,
                        onValueChange = { viewModel.setBrightness(it) },
                        icon = Icons.Rounded.LightMode,
                        contentDescription = stringResource(R.string.overlay_brightness_title),
                    )

                    // Media Volume Slider
                    ExpressiveSystemSlider(
                        value = state.mediaVolumePercent,
                        onValueChange = { viewModel.setMediaVolume(it) },
                        icon = if (state.mediaVolumePercent <= 0.02f) {
                            Icons.AutoMirrored.Rounded.VolumeOff
                        } else {
                            Icons.AutoMirrored.Rounded.VolumeUp
                        },
                        contentDescription = stringResource(R.string.overlay_volume_title),
                    )
                }
            }
        }
    }
}

@Composable
private fun M3EActionTile(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current

    val cornerRadius by animateDpAsState(
        targetValue = if (isActive) 26.dp else 12.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "action_tile_radius",
    )
    val containerColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(durationMillis = 200),
        label = "action_tile_bg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 200),
        label = "action_tile_tint",
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable {
                HapticUtil.performToggle(view)
                onClick()
            }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(cornerRadius))
                .background(containerColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp),
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = GoogleSansFlexRounded,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 11.sp,
            ),
            color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
                .basicMarquee(),
        )
    }
}

/**
 * Official Material 3 Expressive system slider (AOSP standard):
 * - 40dp capsule track with 12dp corner size
 * - 4dp x 52dp vertical bar thumb with 6dp gap
 * - Icon placed at the start of the track
 * - No percentage text per design specification
 * - Subtle discrete haptic ticks on 5% intervals
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveSystemSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    var lastHapticStep by remember { mutableIntStateOf((value * 20).roundToInt()) }
    val sliderState = remember {
        SliderState(
            value = value,
            valueRange = 0f..1f,
        )
    }
    LaunchedEffect(value) { sliderState.value = value }
    sliderState.onValueChange = { newValue ->
        val newStep = (newValue * 20).roundToInt()
        if (newStep != lastHapticStep) {
            lastHapticStep = newStep
            HapticUtil.performSliderHaptic(view)
        }
        onValueChange(newValue)
    }

    val interactionSource = remember { MutableInteractionSource() }

    Slider(
        state = sliderState,
        modifier = modifier.fillMaxWidth(),
        interactionSource = interactionSource,
        thumb = {
            SliderDefaults.Thumb(
                interactionSource = interactionSource,
                thumbSize = DpSize(width = 4.dp, height = 52.dp),
            )
        },
        track = { state ->
            Box(contentAlignment = Alignment.CenterStart) {
                SliderDefaults.Track(
                    sliderState = state,
                    modifier = Modifier.height(40.dp),
                    trackCornerSize = 12.dp,
                    thumbTrackGapSize = 6.dp,
                    drawStopIndicator = null,
                )
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = if (state.value > 0.12f) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(20.dp),
                )
            }
        },
    )
}

