package com.pixel.gamebooster.ui.screens

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.pixel.gamebooster.R
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.services.AppDetectionService
import com.pixel.gamebooster.ui.components.GameLauncherGrid
import com.pixel.gamebooster.ui.components.PermissionHealthBanner
import com.pixel.gamebooster.ui.components.PixelGamingHeroCard
import com.pixel.gamebooster.ui.core.sliders.ConfigSliderItem
import com.pixel.gamebooster.ui.util.AdaptiveLayoutInfo
import com.pixel.gamebooster.ui.util.rememberAdaptiveLayoutInfo
import com.pixel.gamebooster.utils.GameModeManager
import com.pixel.gamebooster.utils.HapticUtil
import com.pixel.gamebooster.utils.PermissionHelper

/**
 * Determines whether the Dashboard should render in two-pane mode.
 */
fun isDashboardTwoPane(adaptive: AdaptiveLayoutInfo): Boolean = adaptive.isWideScreen

/**
 * Computes bottom padding clearance to avoid floating navigation toolbar overlap.
 */
fun getDashboardBottomPadding(adaptive: AdaptiveLayoutInfo): Dp =
    if (adaptive.isCompactHeight) 68.dp else 96.dp

/**
 * Determines the scaled height for the Monet Lottie hero animation.
 */
fun getDashboardLottieHeight(adaptive: AdaptiveLayoutInfo): Dp =
    if (!adaptive.isWideScreen) 285.dp
    else if (adaptive.isCompactHeight) 160.dp
    else 280.dp

@Composable
fun DashboardScreen(
    repo: SettingsRepository,
    onAddGameClick: () -> Unit = {},
    onFixPermissionsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isBoosterEnabled by repo.isBoosterEnabled.collectAsState()
    val mode by repo.interventionMode.collectAsState()
    val delay by repo.restoreDelay.collectAsState()

    val adaptive = rememberAdaptiveLayoutInfo()
    val isTwoPane = isDashboardTwoPane(adaptive)
    val bottomPadding = getDashboardBottomPadding(adaptive)

    // Dynamic Monet Lottie Animation for Game Modes (Above Main Enable Hero Card, NOT inside a container)
    val lottieRes = if (!isBoosterEnabled) {
        R.raw.lottie_disabled
    } else {
        when (mode) {
            2 -> R.raw.lottie_performance
            1 -> R.raw.lottie_standard
            3 -> R.raw.lottie_battery
            else -> R.raw.lottie_performance
        }
    }
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(lottieRes))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
    )
    val monetColor = if (!isBoosterEnabled) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.primary
    }
    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR_FILTER,
            value = PorterDuffColorFilter(monetColor.toArgb(), PorterDuff.Mode.SRC_ATOP),
            keyPath = arrayOf("**"),
        ),
    )

    if (isTwoPane) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Full-width Shizuku Warning Banner
            PermissionHealthBanner(
                onFixPermissionsClick = onFixPermissionsClick,
                modifier = Modifier.fillMaxWidth(),
            )

            // Full-width Lottie Hero Animation
            val lottieHeight = getDashboardLottieHeight(adaptive)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(lottieHeight),
                contentAlignment = Alignment.Center,
            ) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    dynamicProperties = dynamicProperties,
                    modifier = Modifier.size(lottieHeight),
                )
            }

            // Full-width Master Game Booster Switch Hero Card
            PixelGamingHeroCard(
                repo = repo,
                onToggleMaster = { enabled ->
                    repo.setBoosterEnabled(enabled)
                    if (enabled) {
                        if (PermissionHelper.hasUsageStatsPermission(context)) {
                            AppDetectionService.start(context)
                        }
                    } else {
                        AppDetectionService.stop(context)
                        if (GameModeManager.isGameModeActive) {
                            GameModeManager.onGameBackground(context, GameModeManager.activeGamePackage, null, repo)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            // Split below Master Switch: Left pane controls (mode + delay), Right pane game launcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.Top,
            ) {
                // Left Column: mode buttons, delay slider
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    DashboardPerformanceProfileCard(
                        mode = mode,
                        view = view,
                        repo = repo,
                    )

                    DashboardRestorationDelayCard(
                        delay = delay,
                        context = context,
                        repo = repo,
                    )
                }

                // Right Column: My Games launcher grid
                Column(
                    modifier = Modifier.weight(1.1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GameLauncherGrid(
                        repo = repo,
                        onAddGameClick = onAddGameClick,
                        isTwoPane = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    } else {
        // Single-column Portrait layout
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Permission Health Banner (visible if Shizuku or Accessibility is revoked)
            PermissionHealthBanner(onFixPermissionsClick = onFixPermissionsClick)

            // Lottie Hero + Master Pixel Game Booster Button with reduced tight padding
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(285.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LottieAnimation(
                        composition = composition,
                        progress = { progress },
                        dynamicProperties = dynamicProperties,
                        modifier = Modifier.size(280.dp),
                    )
                }

                PixelGamingHeroCard(
                    repo = repo,
                    onToggleMaster = { enabled ->
                        repo.setBoosterEnabled(enabled)
                        if (enabled) {
                            if (PermissionHelper.hasUsageStatsPermission(context)) {
                                AppDetectionService.start(context)
                            }
                        } else {
                            AppDetectionService.stop(context)
                            if (GameModeManager.isGameModeActive) {
                                GameModeManager.onGameBackground(context, GameModeManager.activeGamePackage, null, repo)
                            }
                        }
                    },
                )
            }

            DashboardPerformanceProfileCard(
                mode = mode,
                view = view,
                repo = repo,
            )

            DashboardRestorationDelayCard(
                delay = delay,
                context = context,
                repo = repo,
            )

            // Section: My Games Launcher Grid (at bottom of homepage)
            GameLauncherGrid(
                repo = repo,
                onAddGameClick = onAddGameClick,
                isTwoPane = false,
            )
        }
    }
}

@Composable
private fun DashboardPerformanceProfileCard(
    mode: Int,
    view: View,
    repo: SettingsRepository,
) {
    Text(
        text = stringResource(R.string.dash_performance_profile),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp),
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.dash_game_manager_intervention),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.dash_game_manager_intervention_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // 3 Separate Selectable Cards (Icon on top, label on bottom) - NO BORDER, NO OVERFLOW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val modeOptions = listOf(
                    Triple(2, stringResource(R.string.dash_mode_performance), Icons.Rounded.Speed),
                    Triple(1, stringResource(R.string.dash_mode_standard), Icons.Rounded.Tune),
                    Triple(3, stringResource(R.string.dash_mode_battery), Icons.Rounded.BatteryChargingFull),
                )
                modeOptions.forEach { (targetMode, label, icon) ->
                    val isSelected = mode == targetMode
                    Card(
                        onClick = {
                            if (!isSelected) {
                                HapticUtil.performUIHaptic(view)
                                repo.setInterventionMode(targetMode)
                            }
                        },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(82.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardRestorationDelayCard(
    delay: Int,
    context: Context,
    repo: SettingsRepository,
) {
    Text(
        text = stringResource(R.string.dash_restoration_delay),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp),
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        val immediateLabel = stringResource(R.string.dash_delay_immediate)
        ConfigSliderItem(
            title = stringResource(R.string.dash_restoration_delay),
            subtitle = stringResource(R.string.dash_restoration_delay_desc),
            value = delay.toFloat(),
            onValueChange = { repo.setRestoreDelay(it.toInt()) },
            valueRange = 0f..120f,
            steps = 23,
            icon = Icons.Rounded.Timer,
            valueFormatter = {
                if (it.toInt() == 0) immediateLabel
                else context.getString(R.string.dash_delay_seconds, it.toInt())
            },
        )
    }
}
