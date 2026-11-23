package com.pixel.gamebooster.ui.screens

import android.graphics.Color as AndroidColor
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewSidebar
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
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
import com.pixel.gamebooster.ui.components.PullTitleHeader
import com.pixel.gamebooster.ui.components.rememberPullTitleHeaderState
import com.pixel.gamebooster.ui.core.cards.IconToggleItem
import com.pixel.gamebooster.ui.core.containers.RoundedCardContainer
import com.pixel.gamebooster.ui.theme.GoogleSansFlexRounded
import com.pixel.gamebooster.ui.util.rememberAdaptiveLayoutInfo
import com.pixel.gamebooster.utils.HapticUtil

/**
 * Dedicated In-Game Overlay Settings screen.
 * Formatted to match Pixel Game Booster's design language:
 * - Expressive gestural PullTitleHeader
 * - Live interactive preview card of the in-game floating HUD
 * - Sectional RoundedCardContainer and IconToggleItem components
 * - Clear clearance padding for the bottom floating navigation bar
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OverlaySettingsScreen(
    repo: SettingsRepository,
    modifier: Modifier = Modifier,
    onNavigateToScreenRecording: () -> Unit = {},
) {
    val view = LocalView.current
    val isOverlayEnabled by repo.isOverlayEnabled.collectAsState()
    val overlayStyle by repo.overlayStyle.collectAsState()
    val overlayOpacity by repo.overlayOpacity.collectAsState()
    val showLiveFps by repo.showLiveFps.collectAsState()
    val overlayColorMode by repo.overlayColorMode.collectAsState()
    val overlayCustomColor by repo.overlayCustomColor.collectAsState()
    val recordAudioSource by repo.recordAudioSource.collectAsState()
    val recordResolution by repo.recordResolution.collectAsState()
    val currentAudio = RecordAudioSource.fromValue(recordAudioSource)
    val currentResolution = RecordResolution.fromValue(recordResolution)

    val headerState = rememberPullTitleHeaderState()
    val adaptive = rememberAdaptiveLayoutInfo()
    val bottomPadding = if (adaptive.isCompactHeight) 68.dp else 108.dp
    var isColorPickerExpanded by remember { mutableStateOf(false) }

    // Dynamic Monet Lottie Animation for In-Game Overlay
    val lottieRes = if (overlayStyle == 1) {
        R.raw.lottie_overlay_controller
    } else {
        R.raw.lottie_overlay_edge
    }
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(lottieRes))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
    )
    val accentColor = if (isOverlayEnabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR_FILTER,
            value = PorterDuffColorFilter(accentColor.toArgb(), PorterDuff.Mode.SRC_ATOP),
            keyPath = arrayOf("**"),
        ),
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(headerState.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Gestural Pull Title Header
            PullTitleHeader(
                title = stringResource(R.string.overlay_settings_title),
                state = headerState,
            )

            // Screen Subtitle
            Text(
                text = stringResource(R.string.overlay_settings_subtitle),
                style = TextStyle(
                    fontFamily = GoogleSansFlexRounded,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            // Full-width Lottie Hero Animation with Dynamic Monet Accent Tinting
            val lottieHeight = if (adaptive.isCompactHeight) 140.dp else 190.dp
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(lottieHeight),
                )
            }

            // Section 1: General Activation
            Text(
                text = stringResource(R.string.overlay_section_general),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp),
            )

            RoundedCardContainer {
                IconToggleItem(
                    title = stringResource(R.string.overlay_settings_enable),
                    subtitle = stringResource(R.string.overlay_settings_enable_desc),
                    icon = Icons.Rounded.Layers,
                    checked = isOverlayEnabled,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        repo.setOverlayEnabled(it)
                    },
                    index = 0,
                    count = 1,
                )
            }

            // Section 2: Trigger Style Selection
            AnimatedVisibility(
                visible = isOverlayEnabled,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.overlay_style_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp),
                    )

                    RoundedCardContainer {
                        // Option 0: Edge Bar
                        val isBarStyle = overlayStyle == 0
                        IconToggleItem(
                            title = stringResource(R.string.overlay_style_bar),
                            subtitle = stringResource(R.string.overlay_style_bar_desc),
                            icon = Icons.AutoMirrored.Rounded.ViewSidebar,
                            checked = isBarStyle,
                            showToggle = false,
                            onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                repo.setOverlayStyle(0)
                            },
                            index = 0,
                            count = 2,
                            trailingContent = if (isBarStyle) {
                                {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            } else null,
                        )

                        // Option 1: Game Controller Floating Circle Button
                        val isCircleStyle = overlayStyle == 1
                        IconToggleItem(
                            title = stringResource(R.string.overlay_style_circle),
                            subtitle = stringResource(R.string.overlay_style_circle_desc),
                            icon = Icons.Rounded.SportsEsports,
                            checked = isCircleStyle,
                            showToggle = false,
                            onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                repo.setOverlayStyle(1)
                            },
                            index = 1,
                            count = 2,
                            trailingContent = if (isCircleStyle) {
                                {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            } else null,
                        )
                    }

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
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val isFloatingButton = overlayStyle == 1
                                    Text(
                                        text = if (isFloatingButton) {
                                            stringResource(R.string.overlay_opacity_button_title)
                                        } else {
                                            stringResource(R.string.overlay_opacity_bar_title)
                                        },
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = GoogleSansFlexRounded,
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = if (isFloatingButton) {
                                            stringResource(R.string.overlay_opacity_button_desc)
                                        } else {
                                            stringResource(R.string.overlay_opacity_bar_desc)
                                        },
                                        style = TextStyle(
                                            fontFamily = GoogleSansFlexRounded,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 12.sp,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    text = "${(overlayOpacity * 100).toInt()}%",
                                    style = TextStyle(
                                        fontFamily = GoogleSansFlexRounded,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Slider(
                                value = overlayOpacity,
                                onValueChange = {
                                    HapticUtil.performSliderHaptic(view)
                                    repo.setOverlayOpacity(it)
                                },
                                valueRange = 0.20f..1.0f,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    RoundedCardContainer {
                        IconToggleItem(
                            title = stringResource(R.string.overlay_show_live_fps_title),
                            subtitle = stringResource(R.string.overlay_show_live_fps_desc),
                            icon = Icons.Rounded.Speed,
                            checked = showLiveFps,
                            onCheckedChange = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                repo.setShowLiveFps(it)
                            },
                            index = 0,
                            count = 1,
                        )
                    }

                    Text(
                        text = stringResource(R.string.overlay_color_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp),
                    )

                    RoundedCardContainer {
                        val isMonet = overlayColorMode == 0
                        IconToggleItem(
                            title = stringResource(R.string.overlay_color_monet),
                            subtitle = stringResource(R.string.overlay_color_monet_desc),
                            icon = Icons.Rounded.Palette,
                            checked = isMonet,
                            showToggle = false,
                            onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                isColorPickerExpanded = false
                                repo.setOverlayColorMode(0)
                            },
                            index = 0,
                            count = 2,
                            trailingContent = if (isMonet) {
                                {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            } else null,
                        )

                        val isCustom = overlayColorMode == 1
                        IconToggleItem(
                            title = stringResource(R.string.overlay_color_custom),
                            subtitle = stringResource(R.string.overlay_color_custom_desc),
                            icon = Icons.Rounded.ColorLens,
                            checked = isCustom,
                            showToggle = false,
                            onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                if (!isCustom) {
                                    repo.setOverlayColorMode(1)
                                    isColorPickerExpanded = true
                                } else {
                                    isColorPickerExpanded = !isColorPickerExpanded
                                }
                            },
                            index = 1,
                            count = 2,
                            trailingContent = if (isCustom) {
                                {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(overlayCustomColor)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = if (AndroidColor.luminance(overlayCustomColor) > 0.5f) Color.Black else Color.White,
                                            modifier = Modifier.size(15.dp),
                                        )
                                    }
                                }
                            } else null,
                        )
                    }

                    AnimatedVisibility(
                        visible = overlayColorMode == 1,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        val savedHexCode = String.format("#%06X", 0xFFFFFF and overlayCustomColor)

                        if (!isColorPickerExpanded) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                ),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color(overlayCustomColor)),
                                        )
                                        Column {
                                            Text(
                                                text = stringResource(R.string.overlay_color_selected),
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontFamily = GoogleSansFlexRounded,
                                                    fontWeight = FontWeight.SemiBold,
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                            Text(
                                                text = savedHexCode,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontFamily = GoogleSansFlexRounded,
                                                    fontWeight = FontWeight.Medium,
                                                ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            HapticUtil.performVirtualKeyHaptic(view)
                                            isColorPickerExpanded = true
                                        },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.overlay_color_change),
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontFamily = GoogleSansFlexRounded,
                                                fontWeight = FontWeight.SemiBold,
                                            ),
                                        )
                                    }
                                }
                            }
                        } else {
                            val presetColors = listOf(
                                0xFFFFFFFF.toInt(), // White
                                0xFF9AA0A6.toInt(), // Grey
                                0xFF444746.toInt(), // Charcoal
                                0xFF4285F4.toInt(), // Pixel Blue
                                0xFF34A853.toInt(), // Mint
                                0xFFFBBC04.toInt(), // Amber
                                0xFFEA4335.toInt(), // Coral
                                0xFFA142F4.toInt(), // Lavender
                            )
                            var draftColor by remember(overlayCustomColor, isColorPickerExpanded) {
                                mutableIntStateOf(overlayCustomColor)
                            }
                            val initialHsv = remember(overlayCustomColor, isColorPickerExpanded) {
                                FloatArray(3).also { AndroidColor.colorToHSV(overlayCustomColor, it) }
                            }
                            var hue by remember(overlayCustomColor, isColorPickerExpanded) { mutableFloatStateOf(initialHsv[0]) }
                            var sat by remember(overlayCustomColor, isColorPickerExpanded) { mutableFloatStateOf(initialHsv[1]) }
                            var brightness by remember(overlayCustomColor, isColorPickerExpanded) { mutableFloatStateOf(initialHsv[2]) }

                            val draftHexCode = String.format("#%06X", 0xFFFFFF and draftColor)

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
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = stringResource(R.string.overlay_color_picker_title),
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontFamily = GoogleSansFlexRounded,
                                                fontWeight = FontWeight.SemiBold,
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Text(
                                                text = draftHexCode,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontFamily = GoogleSansFlexRounded,
                                                    fontWeight = FontWeight.Medium,
                                                ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(draftColor)),
                                            )
                                        }
                                    }

                                    // 2D Saturation (left->right) x Brightness (bottom->top) Spectrum Box
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(156.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                            .pointerInput(Unit) {
                                                detectTapGestures { offset ->
                                                    sat = (offset.x / size.width).coerceIn(0f, 1f)
                                                    brightness = (1f - offset.y / size.height).coerceIn(0f, 1f)
                                                    draftColor = AndroidColor.HSVToColor(floatArrayOf(hue, sat, brightness))
                                                }
                                            }
                                            .pointerInput(Unit) {
                                                detectDragGestures { change, _ ->
                                                    change.consume()
                                                    sat = (change.position.x / size.width).coerceIn(0f, 1f)
                                                    brightness = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                                                    draftColor = AndroidColor.HSVToColor(floatArrayOf(hue, sat, brightness))
                                                }
                                            },
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val pureHue = Color.hsv(hue, 1f, 1f)
                                            drawRect(brush = Brush.horizontalGradient(listOf(Color.White, pureHue)))
                                            drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))

                                            val cx = sat * size.width
                                            val cy = (1f - brightness) * size.height
                                            drawCircle(
                                                color = Color.Black.copy(alpha = 0.45f),
                                                radius = 9.dp.toPx(),
                                                center = Offset(cx, cy),
                                                style = Stroke(width = 3.dp.toPx()),
                                            )
                                            drawCircle(
                                                color = Color.White,
                                                radius = 9.dp.toPx(),
                                                center = Offset(cx, cy),
                                                style = Stroke(width = 2.dp.toPx()),
                                            )
                                        }
                                    }

                                    // Rainbow Hue Bar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(24.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .pointerInput(Unit) {
                                                detectTapGestures { offset ->
                                                    hue = ((offset.x / size.width) * 360f).coerceIn(0f, 360f)
                                                    draftColor = AndroidColor.HSVToColor(floatArrayOf(hue, sat, brightness))
                                                }
                                            }
                                            .pointerInput(Unit) {
                                                detectDragGestures { change, _ ->
                                                    change.consume()
                                                    hue = ((change.position.x / size.width) * 360f).coerceIn(0f, 360f)
                                                    draftColor = AndroidColor.HSVToColor(floatArrayOf(hue, sat, brightness))
                                                }
                                            },
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val hueColors = listOf(
                                                Color.Red,
                                                Color.Yellow,
                                                Color.Green,
                                                Color.Cyan,
                                                Color.Blue,
                                                Color.Magenta,
                                                Color.Red,
                                            )
                                            drawRect(brush = Brush.horizontalGradient(hueColors))
                                            val tx = (hue / 360f).coerceIn(0f, 1f) * size.width
                                            val ty = size.height / 2f
                                            drawCircle(
                                                color = Color.Black.copy(alpha = 0.45f),
                                                radius = 9.dp.toPx(),
                                                center = Offset(tx, ty),
                                                style = Stroke(width = 3.dp.toPx()),
                                            )
                                            drawCircle(
                                                color = Color.White,
                                                radius = 9.dp.toPx(),
                                                center = Offset(tx, ty),
                                                style = Stroke(width = 2.dp.toPx()),
                                            )
                                        }
                                    }

                                    // Quick Swatches (including White, Grey, Charcoal)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        presetColors.forEach { argb ->
                                            val selected = draftColor == argb
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(argb))
                                                    .clickable {
                                                        HapticUtil.performVirtualKeyHaptic(view)
                                                        val pickedHsv = FloatArray(3)
                                                        AndroidColor.colorToHSV(argb, pickedHsv)
                                                        if (pickedHsv[1] > 0f) hue = pickedHsv[0]
                                                        sat = pickedHsv[1]
                                                        brightness = pickedHsv[2]
                                                        draftColor = argb
                                                    },
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                if (selected) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = null,
                                                        tint = if (AndroidColor.luminance(argb) > 0.5f) Color.Black else Color.White,
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Confirm / Cancel Actions
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                HapticUtil.performVirtualKeyHaptic(view)
                                                isColorPickerExpanded = false
                                            },
                                        ) {
                                            Text(
                                                text = stringResource(R.string.overlay_color_cancel),
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontFamily = GoogleSansFlexRounded,
                                                    fontWeight = FontWeight.Medium,
                                                ),
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                HapticUtil.performVirtualKeyHaptic(view)
                                                repo.setOverlayCustomColor(draftColor)
                                                isColorPickerExpanded = false
                                            },
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = stringResource(R.string.overlay_color_confirm),
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontFamily = GoogleSansFlexRounded,
                                                    fontWeight = FontWeight.SemiBold,
                                                ),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Screen Recording (hidden when overlay is disabled)
            AnimatedVisibility(
                visible = isOverlayEnabled,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.screen_recording_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp),
                    )

                    RoundedCardContainer {
                        IconToggleItem(
                            title = stringResource(R.string.screen_recording_title),
                            subtitle = "${currentResolution.label} • ${currentAudio.label}",
                            icon = Icons.Rounded.Videocam,
                            showToggle = false,
                            onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                onNavigateToScreenRecording()
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            index = 0,
                            count = 1,
                        )
                    }
                }
            }
        }
    }
}

