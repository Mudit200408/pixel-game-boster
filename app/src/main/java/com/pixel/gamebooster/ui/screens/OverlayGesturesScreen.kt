package com.pixel.gamebooster.ui.screens

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixel.gamebooster.R
import com.pixel.gamebooster.data.model.OverlayGestureAction
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.ui.components.FloatingBackTitleToolbar
import com.pixel.gamebooster.ui.core.cards.IconToggleItem
import com.pixel.gamebooster.ui.core.containers.RoundedCardContainer
import com.pixel.gamebooster.ui.core.sliders.ConfigSliderItem
import com.pixel.gamebooster.ui.theme.GoogleSansFlexRounded
import com.pixel.gamebooster.ui.util.rememberAdaptiveLayoutInfo
import com.pixel.gamebooster.utils.HapticUtil
import kotlin.math.roundToInt

private enum class OverlayGestureTab {
    DOUBLE_TAP,
    LONG_TAP,
}

private val SELECTABLE_GESTURE_ACTIONS = listOf(
    OverlayGestureAction.REVEAL_OVERLAY,
    OverlayGestureAction.TOGGLE_RECORDING,
    OverlayGestureAction.TAKE_SCREENSHOT,
    OverlayGestureAction.TOGGLE_FPS,
    OverlayGestureAction.PURGE_RAM,
    OverlayGestureAction.CYCLE_PERFORMANCE_MODE,
)

/**
 * Dedicated Overlay Gestures configuration screen for Edge Bar & Floating Button.
 * Uses M3E connected ToggleButton tabs (Double Tap / Long Tap) and IconToggleItem switch tiles.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OverlayGesturesScreen(
    repo: SettingsRepository,
    onBack: () -> Unit,
    isAnimationPlaying: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val overlayStyle by repo.overlayStyle.collectAsState()
    val doubleTapAction by repo.overlayDoubleTapAction.collectAsState()
    val longTapAction by repo.overlayLongTapAction.collectAsState()
    val doubleTapDelayMs by repo.overlayDoubleTapDelayMs.collectAsState()
    val recordDoubleTapStop by repo.recordDoubleTapStop.collectAsState()

    var selectedTab by remember { mutableStateOf(OverlayGestureTab.DOUBLE_TAP) }

    val adaptive = rememberAdaptiveLayoutInfo()
    val bottomPadding = if (adaptive.isCompactHeight) 80.dp else 104.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val isFloatingButton = overlayStyle == 1
            val lottieRes = when (selectedTab) {
                OverlayGestureTab.DOUBLE_TAP -> if (isFloatingButton) {
                    R.raw.lottie_overlay_controller_double_tap
                } else {
                    R.raw.lottie_overlay_edge_double_tap
                }
                OverlayGestureTab.LONG_TAP -> if (isFloatingButton) {
                    R.raw.lottie_overlay_controller_long_tap
                } else {
                    R.raw.lottie_overlay_edge_long_tap
                }
            }
            val accentColor = MaterialTheme.colorScheme.primary.toArgb()
            val accentFilter = remember(accentColor) {
                PorterDuffColorFilter(accentColor, PorterDuff.Mode.SRC_ATOP)
            }
            val dynamicProperties = rememberLottieDynamicProperties(
                rememberLottieDynamicProperty(LottieProperty.COLOR_FILTER, accentFilter, "edgeBar", "**"),
                rememberLottieDynamicProperty(LottieProperty.COLOR_FILTER, accentFilter, "controllerButton", "FAB Group", "**"),
                rememberLottieDynamicProperty(LottieProperty.COLOR_FILTER, accentFilter, "controllerButton", "Buttons Group", "**"),
                rememberLottieDynamicProperty(LottieProperty.COLOR_FILTER, accentFilter, "controllerButton", "Dpad Group", "**"),
                rememberLottieDynamicProperty(LottieProperty.COLOR_FILTER, accentFilter, "holdRing", "**"),
                rememberLottieDynamicProperty(LottieProperty.COLOR_FILTER, accentFilter, "rippleRing", "**"),
                rememberLottieDynamicProperty(LottieProperty.COLOR_FILTER, accentFilter, "tap1Ring", "**"),
                rememberLottieDynamicProperty(LottieProperty.COLOR_FILTER, accentFilter, "tap2Ring", "**"),
                rememberLottieDynamicProperty(LottieProperty.COLOR_FILTER, accentFilter, "touchDot", "**"),
            )
            val lottieHeight = if (adaptive.isCompactHeight) 130.dp else 170.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(lottieHeight),
                contentAlignment = Alignment.Center,
            ) {
                key(lottieRes) {
                    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(lottieRes))
                    val progress by animateLottieCompositionAsState(
                        composition = composition,
                        isPlaying = isAnimationPlaying,
                        iterations = LottieConstants.IterateForever,
                    )
                    LottieAnimation(
                        composition = composition,
                        progress = { progress },
                        dynamicProperties = dynamicProperties,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(lottieHeight),
                    )
                }
            }

            // Connected Toggle Button: Double Tap <-> Long Tap
            val tabs = listOf(
                OverlayGestureTab.DOUBLE_TAP to stringResource(R.string.overlay_gestures_double_tap_section),
                OverlayGestureTab.LONG_TAP to stringResource(R.string.overlay_gestures_long_tap_section),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { idx, (tab, label) ->
                    val isSelected = selectedTab == tab
                    ToggleButton(
                        checked = isSelected,
                        onCheckedChange = {
                            HapticUtil.performToggle(view)
                            selectedTab = tab
                        },
                        shapes = when (idx) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            else -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        },
                        colors = ToggleButtonDefaults.toggleButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            checkedContainerColor = MaterialTheme.colorScheme.primary,
                            checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .semantics { role = Role.RadioButton },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = GoogleSansFlexRounded,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "gesture_tab_content",
            ) { tab ->
                when (tab) {
                    OverlayGestureTab.DOUBLE_TAP -> {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = stringResource(R.string.overlay_gestures_double_tap_action_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp),
                            )

                            GestureActionSwitchList(
                                selectedAction = doubleTapAction,
                                onToggleAction = { action, enabled ->
                                    HapticUtil.performToggle(view)
                                    repo.setOverlayDoubleTapAction(
                                        if (enabled) action else OverlayGestureAction.NONE,
                                    )
                                },
                            )

                            RoundedCardContainer {
                                ConfigSliderItem(
                                    title = stringResource(R.string.overlay_gestures_double_tap_delay_title),
                                    subtitle = stringResource(R.string.overlay_gestures_double_tap_delay_desc),
                                    icon = Icons.Rounded.Timer,
                                    value = doubleTapDelayMs.toFloat(),
                                    onValueChange = { raw ->
                                        val snapped = ((raw / 25f).roundToInt() * 25).coerceIn(100, 500)
                                        if (snapped != doubleTapDelayMs) {
                                            repo.setOverlayDoubleTapDelayMs(snapped)
                                        }
                                    },
                                    valueRange = 100f..500f,
                                    steps = 15,
                                    increment = 25f,
                                    valueFormatter = { "${it.roundToInt()} ms" },
                                    index = 0,
                                    count = 2,
                                )

                                IconToggleItem(
                                    title = stringResource(R.string.screen_recording_double_tap_title),
                                    subtitle = stringResource(R.string.screen_recording_double_tap_desc),
                                    icon = Icons.Rounded.StopCircle,
                                    checked = recordDoubleTapStop,
                                    onCheckedChange = { repo.setRecordDoubleTapStop(it) },
                                    index = 1,
                                    count = 2,
                                )
                            }
                        }
                    }

                    OverlayGestureTab.LONG_TAP -> {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = stringResource(R.string.overlay_gestures_long_tap_action_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp),
                            )

                            Text(
                                text = stringResource(R.string.overlay_gestures_long_tap_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )

                            GestureActionSwitchList(
                                selectedAction = longTapAction,
                                onToggleAction = { action, enabled ->
                                    HapticUtil.performToggle(view)
                                    repo.setOverlayLongTapAction(
                                        if (enabled) action else OverlayGestureAction.NONE,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }

        FloatingBackTitleToolbar(
            title = stringResource(R.string.overlay_gestures_screen_title),
            onBack = onBack,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun GestureActionSwitchList(
    selectedAction: OverlayGestureAction,
    onToggleAction: (OverlayGestureAction, Boolean) -> Unit,
) {
    RoundedCardContainer {
        SELECTABLE_GESTURE_ACTIONS.forEachIndexed { index, action ->
            IconToggleItem(
                title = stringResource(gestureActionLabelRes(action)),
                icon = gestureActionIcon(action),
                checked = selectedAction == action,
                onCheckedChange = { enabled -> onToggleAction(action, enabled) },
                index = index,
                count = SELECTABLE_GESTURE_ACTIONS.size,
            )
        }
    }
}

private fun gestureActionLabelRes(action: OverlayGestureAction): Int = when (action) {
    OverlayGestureAction.NONE -> R.string.overlay_gesture_action_none
    OverlayGestureAction.REVEAL_OVERLAY -> R.string.overlay_gesture_action_reveal_overlay
    OverlayGestureAction.TOGGLE_RECORDING -> R.string.overlay_gesture_action_toggle_recording
    OverlayGestureAction.TAKE_SCREENSHOT -> R.string.overlay_gesture_action_take_screenshot
    OverlayGestureAction.TOGGLE_FPS -> R.string.overlay_gesture_action_toggle_fps
    OverlayGestureAction.PURGE_RAM -> R.string.overlay_gesture_action_purge_ram
    OverlayGestureAction.CYCLE_PERFORMANCE_MODE -> R.string.overlay_gesture_action_cycle_mode
}

private fun gestureActionIcon(action: OverlayGestureAction): ImageVector = when (action) {
    OverlayGestureAction.NONE -> Icons.Rounded.Layers
    OverlayGestureAction.REVEAL_OVERLAY -> Icons.Rounded.Layers
    OverlayGestureAction.TOGGLE_RECORDING -> Icons.Rounded.Videocam
    OverlayGestureAction.TAKE_SCREENSHOT -> Icons.Rounded.Screenshot
    OverlayGestureAction.TOGGLE_FPS -> Icons.Rounded.Speed
    OverlayGestureAction.PURGE_RAM -> Icons.Rounded.Memory
    OverlayGestureAction.CYCLE_PERFORMANCE_MODE -> Icons.Rounded.Bolt
}
