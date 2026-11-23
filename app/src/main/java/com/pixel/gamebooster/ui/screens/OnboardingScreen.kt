package com.pixel.gamebooster.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.ui.core.cards.IconToggleItem
import com.pixel.gamebooster.ui.core.containers.RoundedCardContainer
import com.pixel.gamebooster.ui.core.pickers.SegmentedPicker
import com.pixel.gamebooster.utils.HapticUtil
import kotlinx.coroutines.launch

private data class OnboardingSlide(
    val icon: ImageVector,
    val title: String,
    val badge: String,
    val description: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onNavigateToPermissions: () -> Unit,
    modifier: Modifier = Modifier,
    repo: SettingsRepository? = null,
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 4 })

    var selectedMode by remember { mutableStateOf("Shizuku") }
    var hapticsEnabled by remember { mutableStateOf(true) }

    val slides = listOf(
        OnboardingSlide(
            icon = Icons.Rounded.SportsEsports,
            title = "Pixel Game Booster",
            badge = "Google Pixel System Tuning",
            description = "Engineered specifically for Google Pixel devices. Automates physical 120Hz display locking, kernel memory purging, and background app suspension to deliver sustained peak frame rates without thermal throttling.",
        ),
        OnboardingSlide(
            icon = Icons.Rounded.Speed,
            title = "120Hz Display Lock",
            badge = "Hardware Panel Rate",
            description = "Eliminates stutter and frame rate jitter by locking Pixel's LTPO display panel to a rock-solid 120Hz hardware refresh rate.\n\nStabilizes physical display panel timing to prevent aggressive 60Hz/30Hz drops without spoofing in-game engine FPS caps.",
        ),
        OnboardingSlide(
            icon = Icons.Rounded.AcUnit,
            title = "Selective App Freezing",
            badge = "Resource Prioritization",
            description = "Suspends resource-heavy background apps using Android's native package suspension (`cmd package suspend --user 0`) and temporarily pauses background Google sync.\n\nFrees up RAM and CPU cores exclusively for game rendering without altering or uninstalling your apps.",
        ),
        OnboardingSlide(
            icon = Icons.Rounded.Tune,
            title = "Initial Preferences",
            badge = "Setup Optimization",
            description = "Configure your privileged execution backend and tactile feedback preferences before proceeding to permissions setup.",
        ),
    )

    // Trigger subtle tactile haptic when swiping between slides
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect {
            HapticUtil.performLightHaptic(view)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pixel Game Booster",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    if (pagerState.currentPage > 0) {
                        IconButton(
                            onClick = {
                                HapticUtil.performClick(view)
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            HapticUtil.performClick(view)
                            onNavigateToPermissions()
                        },
                    ) {
                        Text(
                            text = "Skip",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Expressive animated pill page indicator
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(slides.size) { index ->
                        val isSelected = index == pagerState.currentPage
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 28.dp else 8.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow,
                            ),
                            label = "indicatorWidth",
                        )
                        val color by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            label = "indicatorColor",
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(color),
                        )
                    }
                }

                // Bottom Action Button
                if (pagerState.currentPage < slides.size - 1) {
                    Button(
                        onClick = {
                            HapticUtil.performClick(view)
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                    ) {
                        Text(
                            text = "Next",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            HapticUtil.performClick(view)
                            onNavigateToPermissions()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                    ) {
                        Text(
                            text = "Configure Permissions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) { pageIndex ->
            val slide = slides[pageIndex]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Hero Illustration Surface with Native M3 Expressive Iconography
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = slide.icon,
                            contentDescription = slide.title,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp),
                        )
                    }
                }

                // Subtitle Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = slide.badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }

                // Title
                Text(
                    text = slide.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )

                // Description Container
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = slide.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp),
                    )
                }

                // Slide 4 Interactive Preferences: Privileged Mode & Tactile Feedback
                if (pageIndex == 3) {
                    RoundedCardContainer {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceBright)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = "Privileged Execution Backend",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            SegmentedPicker(
                                items = listOf("Shizuku", "Root"),
                                selectedItem = selectedMode,
                                onItemSelected = { selectedMode = it },
                                labelProvider = { if (it == "Shizuku") "Shizuku (Wireless ADB)" else "Root (su)" },
                            )
                        }

                        IconToggleItem(
                            title = "Tactile Haptic Feedback",
                            subtitle = "Interactive vibration ticks on clicks and slider adjustments",
                            icon = Icons.Rounded.Vibration,
                            isChecked = hapticsEnabled,
                            onCheckedChange = { hapticsEnabled = it },
                            index = 1,
                            count = 2,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
