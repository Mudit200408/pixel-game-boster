package com.pixel.gamebooster.overlay.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpandedPanel(
    state: OverlayUiState,
    viewModel: OverlayViewModel,
    visible: Boolean = true,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val isRight = state.isDockedOnRight
    val anchorOrigin = if (isRight) TransformOrigin(1f, 0.5f) else TransformOrigin(0f, 0.5f)

    // Game Space translucent dimming scrim: fades smoothly without blocking game audio/state
    val scrimAlpha by animateFloatAsState(
        targetValue = if (visible) 0.22f else 0f,
        animationSpec = tween(durationMillis = if (visible) 200 else 180),
        label = "scrim_alpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = scrimAlpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = if (isRight) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = 380f,
                ),
                initialOffsetX = { fullWidth -> if (isRight) fullWidth else -fullWidth },
            ) + scaleIn(
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = 380f,
                ),
                initialScale = 0.94f,
                transformOrigin = anchorOrigin,
            ) + fadeIn(
                animationSpec = tween(durationMillis = 180, easing = LinearOutSlowInEasing),
            ),
            exit = slideOutHorizontally(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                targetOffsetX = { fullWidth -> if (isRight) fullWidth else -fullWidth },
            ) + scaleOut(
                animationSpec = tween(durationMillis = 160, easing = FastOutLinearInEasing),
                targetScale = 0.94f,
                transformOrigin = anchorOrigin,
            ) + fadeOut(
                animationSpec = tween(durationMillis = 160, easing = FastOutLinearInEasing),
            ),
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
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = false,
                ) {}
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val layout = state.layoutConfig
                val enabledBlocks = remember(layout.blocks) {
                    layout.blocks.filter { it.enabled }
                }
                enabledBlocks.forEach { block ->
                    when (block.type) {
                        com.pixel.gamebooster.overlay.model.OverlayBlockType.TELEMETRY_PILL -> {
                            ExpandedPanelTelemetryPill(
                                metrics = layout.telemetryMetrics,
                                currentFps = state.currentFps,
                                temperatureC = state.temperatureC,
                                wattageW = state.wattageW,
                                batteryPercent = state.batteryPercent,
                            )
                        }

                        com.pixel.gamebooster.overlay.model.OverlayBlockType.MODE_SWITCHER -> {
                            ExpandedPanelModeSwitcher(
                                selectedMode = state.interventionMode,
                                onModeSelected = { mode ->
                                    HapticUtil.performToggle(view)
                                    viewModel.setInterventionMode(mode)
                                },
                            )
                        }

                        com.pixel.gamebooster.overlay.model.OverlayBlockType.ACTION_TILES -> {
                            val tiles = layout.tiles
                            val chunkedTiles = remember(tiles) { tiles.chunked(3) }
                            if (tiles.isNotEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    chunkedTiles.forEach { rowTiles ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            rowTiles.forEach { tile ->
                                                M3EActionTile(
                                                    icon = tile.iconVector(),
                                                    label = stringResource(tile.labelRes()),
                                                    isActive = tile.isActiveIn(state),
                                                    onClick = { viewModel.onTileClick(tile) },
                                                    modifier = Modifier.weight(1f),
                                                )
                                            }
                                            repeat(3 - rowTiles.size) {
                                                androidx.compose.foundation.layout.Spacer(
                                                    modifier = Modifier.weight(1f),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        com.pixel.gamebooster.overlay.model.OverlayBlockType.BRIGHTNESS_SLIDER -> {
                            ExpressiveSystemSlider(
                                value = state.brightnessPercent,
                                onValueChange = { viewModel.setBrightness(it) },
                                icon = Icons.Rounded.LightMode,
                                contentDescription = stringResource(R.string.overlay_brightness_title),
                            )
                        }

                        com.pixel.gamebooster.overlay.model.OverlayBlockType.VOLUME_SLIDER -> {
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
    }
}
}

@Composable
fun ExpandedPanelTelemetryPill(
    metrics: List<com.pixel.gamebooster.overlay.model.TelemetryMetric>,
    currentFps: Int,
    temperatureC: Int,
    wattageW: Float,
    batteryPercent: Int,
    modifier: Modifier = Modifier,
) {
    if (metrics.isEmpty()) return
    val metricTextStyle = MaterialTheme.typography.titleLarge.copy(
        fontFamily = GoogleSansFlexRounded,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(74.dp)
            .clip(RoundedCornerShape(37.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            metrics.forEachIndexed { idx, metric ->
                if (idx > 0) {
                    Text(
                        text = "•",
                        style = metricTextStyle,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                    )
                }
                when (metric) {
                    com.pixel.gamebooster.overlay.model.TelemetryMetric.FPS -> Text(
                        text = stringResource(R.string.overlay_fps_badge, currentFps),
                        style = metricTextStyle,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false,
                    )
                    com.pixel.gamebooster.overlay.model.TelemetryMetric.TEMPERATURE -> Text(
                        text = "${temperatureC}°C",
                        style = metricTextStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                    )
                    com.pixel.gamebooster.overlay.model.TelemetryMetric.WATTAGE -> Text(
                        text = "${wattageW}W",
                        style = metricTextStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                    )
                    com.pixel.gamebooster.overlay.model.TelemetryMetric.BATTERY -> Text(
                        text = "${batteryPercent}%",
                        style = metricTextStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpandedPanelModeSwitcher(
    selectedMode: Int,
    onModeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val modes = listOf(
            3 to stringResource(R.string.overlay_mode_battery),
            1 to stringResource(R.string.overlay_mode_balanced),
            2 to stringResource(R.string.overlay_mode_boost),
        )
        modes.forEachIndexed { index, (mode, label) ->
            val isSelected = selectedMode == mode
            ToggleButton(
                checked = isSelected,
                onCheckedChange = { onModeSelected(mode) },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    modes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = ToggleButtonDefaults.toggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                    checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = GoogleSansFlexRounded,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = if (label.length > 12) 11.sp else if (label.length > 10) 12.sp else 13.sp,
                    ),
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun M3EActionTile(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
) {
    val view = LocalView.current

    Column(
        modifier = modifier.padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FilledIconToggleButton(
            checked = isActive,
            onCheckedChange = {
                if (interactive) {
                    HapticUtil.performToggle(view)
                    onClick()
                }
            },
            modifier = Modifier.size(52.dp),
            shapes = IconButtonDefaults.toggleableShapes(
                shape = IconButtonDefaults.mediumSquareShape,
                checkedShape = IconButtonDefaults.mediumRoundShape,
            ),
            colors = IconButtonDefaults.filledIconToggleButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
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
                .basicMarquee()
                .then(
                    if (interactive) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            HapticUtil.performToggle(view)
                            onClick()
                        }
                    } else {
                        Modifier
                    },
                ),
        )
    }
}

/**
 * Official Material 3 Expressive system slider (AOSP standard):
 * - 40dp capsule track with 12dp corner size
 * - 4dp x 52dp vertical bar thumb with 6dp gap
 * - Icon hugs the thumb (left in active track, springs to right in inactive track near 0%)
 * - Subtle discrete haptic ticks on 5% intervals
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveSystemSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
) {
    val view = LocalView.current
    var lastHapticStep by remember { mutableIntStateOf((value * 20).roundToInt()) }
    val sliderState = remember {
        SliderState(
            value = value,
            valueRange = 0f..1f,
        )
    }
    LaunchedEffect(value) {
        sliderState.value = value
        lastHapticStep = (value * 20).roundToInt()
    }
    SideEffect {
        sliderState.onValueChange = { newValue ->
            if (!interactive) {
                sliderState.value = value
            } else {
                val newStep = (newValue * 20).roundToInt()
                if (newStep != lastHapticStep) {
                    lastHapticStep = newStep
                    HapticUtil.performSliderHaptic(view)
                }
                onValueChange(newValue)
            }
        }
    }

    val interactionSource = remember { MutableInteractionSource() }

    // Geometry constants matching your M3E tokens
    val thumbWidth = 4.dp
    val thumbTrackGap = 6.dp
    val iconSize = 20.dp
    val iconThumbGap = 10.dp // Little gap between the thumb and the icon
    val edgePadding = 10.dp  // Minimum breathing room from the track's outer edge

    val spatialSpring = MaterialTheme.motionScheme.fastSpatialSpec<Dp>()
    val colorSpec = MaterialTheme.motionScheme.fastEffectsSpec<androidx.compose.ui.graphics.Color>()

    Slider(
        state = sliderState,
        modifier = modifier.fillMaxWidth(),
        interactionSource = interactionSource,
        thumb = {
            SliderDefaults.Thumb(
                interactionSource = interactionSource,
                thumbSize = DpSize(width = thumbWidth, height = 52.dp),
            )
        },
        track = { state ->
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                SliderDefaults.Track(
                    sliderState = state,
                    modifier = Modifier.height(40.dp),
                    trackCornerSize = 12.dp,
                    trackInsideCornerSize = 2.dp,
                    thumbTrackGapSize = thumbTrackGap,
                    drawStopIndicator = null,
                )

                // Material 3 Slider centers the thumb within [thumbWidth/2 .. maxWidth - thumbWidth/2]
                val thumbHalf = thumbWidth / 2
                val availableTravel = (maxWidth - thumbWidth).coerceAtLeast(0.dp)
                val thumbCenterX = thumbHalf + (availableTravel * state.coercedValueAsFraction)

                // Total clearance from thumb center to icon's nearest edge
                val clearanceFromThumbCenter = thumbHalf + thumbTrackGap + iconThumbGap

                // Flip to the right of the thumb when there isn't enough room on the left
                val isIconOnRight = (thumbCenterX - clearanceFromThumbCenter - iconSize) < edgePadding

                // Compute exact X position: hugs the left side of the thumb, or jumps to the right side
                val rawTargetX = if (isIconOnRight) {
                    thumbCenterX + clearanceFromThumbCenter
                } else {
                    thumbCenterX - clearanceFromThumbCenter - iconSize
                }

                // Clamp so the icon never clips past the track's rounded ends
                val clampedTargetX = rawTargetX.coerceIn(
                    minimumValue = edgePadding,
                    maximumValue = (maxWidth - iconSize - edgePadding).coerceAtLeast(edgePadding)
                )

                // Animate only the side-flip with a spring, while tracking thumb drags 1:1
                val sideFlipSpringOffset by animateDpAsState(
                    targetValue = if (isIconOnRight) {
                        (clearanceFromThumbCenter * 2) + iconSize
                    } else {
                        0.dp
                    },
                    animationSpec = spatialSpring,
                    label = "iconSideFlipSpring"
                )

                // Base position follows the thumb immediately without drag lag; spring adds the side jump
                val leftAnchoredX = (thumbCenterX - clearanceFromThumbCenter - iconSize)
                val finalIconX = (leftAnchoredX + sideFlipSpringOffset).coerceIn(
                    minimumValue = edgePadding,
                    maximumValue = (maxWidth - iconSize - edgePadding).coerceAtLeast(edgePadding)
                )

                val iconTint by animateColorAsState(
                    targetValue = if (isIconOnRight) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    },
                    animationSpec = colorSpec,
                    label = "iconTint"
                )

                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = iconTint,
                    modifier = Modifier
                        .offset(x = finalIconX)
                        .size(iconSize),
                )
            }
        },
    )
}

