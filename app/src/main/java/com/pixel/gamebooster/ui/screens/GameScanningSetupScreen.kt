package com.pixel.gamebooster.ui.screens

import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.pixel.gamebooster.R
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.services.AppDetectionService
import com.pixel.gamebooster.ui.util.rememberAdaptiveLayoutInfo
import com.pixel.gamebooster.utils.GameDetectionUtils
import com.pixel.gamebooster.utils.HapticUtil
import com.pixel.gamebooster.utils.PackageFilterUtils
import com.pixel.gamebooster.utils.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class DetectedGameItem(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
)

enum class GameScanState {
    SCANNING,
    OPTIMIZING,
    READY,
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GameScanningSetupScreen(
    repo: SettingsRepository,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current

    var scanState by remember { mutableStateOf(GameScanState.SCANNING) }
    var detectedGames by remember { mutableStateOf<List<DetectedGameItem>>(emptyList()) }

    // Coroutine scanner
    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()

        // 1. Scan installed applications on IO thread
        val games = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val installed = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getInstalledApplications(PackageManager.GET_META_DATA)
                }
            }.getOrDefault(emptyList())

            val currentManualGames = repo.gameApps.value
            installed
                .filter { appInfo ->
                    val pkg = appInfo.packageName
                    if (pkg == context.packageName) return@filter false
                    if (PackageFilterUtils.isPackageIgnored(pkg, context, currentManualGames)) return@filter false

                    currentManualGames.contains(pkg) ||
                        GameDetectionUtils.isKnownGamePackage(pkg) ||
                        GameDetectionUtils.isGameApplicationInfo(appInfo.category, appInfo.flags)
                }
                .map { appInfo ->
                    val label = runCatching { pm.getApplicationLabel(appInfo).toString() }.getOrDefault(appInfo.packageName)
                    val icon = runCatching {
                        pm.getApplicationIcon(appInfo).toBitmap(96, 96).asImageBitmap()
                    }.getOrNull()
                    DetectedGameItem(appInfo.packageName, label, icon)
                }
                .sortedBy { it.label.lowercase() }
        }

        detectedGames = games

        // 2. Auto-add detected games to repository
        if (games.isNotEmpty()) {
            repo.addGames(games.map { it.packageName })
        }

        // 3. Pleasant delay of ~1.5s - 2.0s for an expressive, polished user experience
        val elapsed = System.currentTimeMillis() - startTime
        val remainingDelay = (1600L - elapsed).coerceAtLeast(600L)
        delay(remainingDelay)

        scanState = GameScanState.OPTIMIZING
        HapticUtil.performLightHaptic(view)

        delay(600L)
        scanState = GameScanState.READY
        HapticUtil.performVirtualKeyHaptic(view)

        // Enable booster and finalize onboarding
        repo.setBoosterEnabled(true)
        repo.setCompletedOnboarding(true)
        if (PermissionHelper.hasUsageStatsPermission(context)) {
            AppDetectionService.start(context)
        }

        delay(400L)
        onComplete()
    }

    // Expressive M3 Shape-Morphing Polygons (Max 5 shapes, max 2 cookies: Triangle, Cookie7, Pill, Cookie12, Sunny)
    val morphPolygons = remember {
        listOf(
            MaterialShapes.Triangle,
            MaterialShapes.Cookie7Sided,
            MaterialShapes.Pill,
            MaterialShapes.Cookie12Sided,
            MaterialShapes.Sunny,
        )
    }

    val adaptive = rememberAdaptiveLayoutInfo()

    val headlineContent = @Composable {
        // Headline
        AnimatedContent(
            targetState = scanState,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "scanHeadlineTransition",
        ) { state ->
            val headline = when (state) {
                GameScanState.SCANNING -> stringResource(R.string.scan_headline_scanning)
                GameScanState.OPTIMIZING -> stringResource(R.string.scan_headline_optimizing)
                GameScanState.READY -> if (detectedGames.isNotEmpty()) {
                    if (detectedGames.size == 1) {
                        stringResource(R.string.scan_headline_found_game, detectedGames.size)
                    } else {
                        stringResource(R.string.scan_headline_found_games, detectedGames.size)
                    }
                } else {
                    stringResource(R.string.scan_headline_ready)
                }
            }
            Text(
                text = headline,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }

    val subtitleContent = @Composable {
        // Subtitle
        AnimatedContent(
            targetState = scanState,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "scanSubtitleTransition",
        ) { state ->
            val subtitle = when (state) {
                GameScanState.SCANNING -> stringResource(R.string.scan_subtitle_scanning)
                GameScanState.OPTIMIZING -> stringResource(R.string.scan_subtitle_optimizing)
                GameScanState.READY -> if (detectedGames.isNotEmpty()) {
                    stringResource(R.string.scan_subtitle_ready_found)
                } else {
                    stringResource(R.string.scan_subtitle_ready_empty)
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }

    val carouselContent = @Composable {
        // Discovered Games Carousel
        AnimatedVisibility(
            visible = detectedGames.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (scanState == GameScanState.READY) Icons.Rounded.CheckCircle else Icons.Rounded.SportsEsports,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(R.string.scan_section_auto_configured),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        items(detectedGames, key = { it.packageName }) { game ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.width(60.dp),
                            ) {
                                if (game.icon != null) {
                                    Image(
                                        bitmap = game.icon,
                                        contentDescription = game.label,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.SportsEsports,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                                Text(
                                    text = game.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = if (adaptive.isCompactHeight) 16.dp else 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.weight(1f, fill = false))

            // Expressive Animated Hero: Very big Shape-Morphing Loading Indicator without any background
            val indicatorHeight = if (adaptive.isCompactHeight) 140.dp else 220.dp
            val indicatorScale = if (adaptive.isCompactHeight) 2.6f else 3.8f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(indicatorHeight),
                contentAlignment = Alignment.Center,
            ) {
                LoadingIndicator(
                    modifier = Modifier.scale(indicatorScale),
                    color = MaterialTheme.colorScheme.primary,
                    polygons = morphPolygons,
                )
            }

            Spacer(modifier = Modifier.height(if (adaptive.isCompactHeight) 16.dp else 28.dp))
            headlineContent()
            Spacer(modifier = Modifier.height(8.dp))
            subtitleContent()
            Spacer(modifier = Modifier.height(if (adaptive.isCompactHeight) 16.dp else 24.dp))
            carouselContent()
            Spacer(modifier = Modifier.weight(1f, fill = false))
        }
    }
}
