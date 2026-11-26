package com.pixel.gamebooster.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.platform.LocalResources
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixel.gamebooster.R
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.ui.components.AddGameGridCard
import com.pixel.gamebooster.ui.components.GameLibraryCard
import com.pixel.gamebooster.ui.components.PermissionHealthBanner
import com.pixel.gamebooster.ui.components.PixelGamingHeroCard
import com.pixel.gamebooster.ui.components.PullTitleHeader
import com.pixel.gamebooster.ui.components.rememberPullTitleHeaderState
import com.pixel.gamebooster.ui.sheets.AppSelectionSheet
import com.pixel.gamebooster.ui.theme.GoogleSansFlexRounded
import com.pixel.gamebooster.ui.util.AppInfoCache
import com.pixel.gamebooster.ui.util.CachedAppInfo
import com.pixel.gamebooster.ui.util.rememberAdaptiveLayoutInfo
import com.pixel.gamebooster.utils.HapticUtil

/**
 * Pixel Game Library landing screen (REQ-01, D-01).
 * Features an edge-to-edge Material 3 Expressive game grid, master booster toggle,
 * interactive Add Game card, and global settings entry point.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameLibraryScreen(
    repo: SettingsRepository,
    onGameClick: (String) -> Unit,
    onOpenSettings: () -> Unit = {},
    onFixPermissionsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val pm = context.packageManager

    val gamePackages by repo.gameApps.collectAsState()
    val profilesList by repo.getAllProfilesFlow().collectAsState(initial = emptyList())
    val profilesMap = remember(profilesList) {
        profilesList.associateBy { it.packageName }
    }

    var showAppPicker by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }

    val adaptive = rememberAdaptiveLayoutInfo()
    val bottomPadding = 108.dp
    val headerState = rememberPullTitleHeaderState()

    val installedGames by produceState(
        initialValue = gamePackages.mapNotNull { AppInfoCache.peek(it, iconSizePx = 128) }
            .sortedBy { it.label.lowercase() },
        gamePackages,
    ) {
        value = AppInfoCache.getAll(context, gamePackages, iconSizePx = 128)
            .sortedBy { it.label.lowercase() }
    }

    val filteredGames = remember(installedGames, searchQuery) {
        if (searchQuery.isBlank()) {
            installedGames
        } else {
            installedGames.filter { game ->
                game.label.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    if (showAppPicker) {
        AppSelectionSheet(
            repo = repo,
            onDismissRequest = { showAppPicker = false },
            title = stringResource(R.string.app_select_add_game_title),
            isGamePicker = true,
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(headerState.nestedScrollConnection),
            ) {
                // Top Bar with PullTitleHeader and Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    PullTitleHeader(
                        title = stringResource(R.string.game_library_title),
                        state = headerState,
                    )
                }

                // Search toggle button
                IconButton(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) searchQuery = ""
                    },
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = stringResource(R.string.game_library_cd_search),
                        tint = if (isSearchActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            // Permission Health Banner (visible if Shizuku or Accessibility is revoked)
            PermissionHealthBanner(
                onFixPermissionsClick = onFixPermissionsClick,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )

            // Search Filter Bar (Expandable)
            AnimatedVisibility(
                visible = isSearchActive,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.game_library_search_games),
                            style = TextStyle(fontFamily = GoogleSansFlexRounded),
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    searchQuery = ""
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = stringResource(R.string.game_library_clear_search),
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }

            // Content Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = bottomPadding,
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                // Master Booster Hero Card spanning full width
                item(span = { GridItemSpan(maxLineSpan) }) {
                    PixelGamingHeroCard(repo = repo)
                }

                // First interactive item: Add Game Card (+)
                item {
                    AddGameGridCard(
                        onClick = { showAppPicker = true },
                    )
                }

                // Configured Games items
                items(filteredGames, key = { it.packageName }) { game ->
                    val pkg = game.packageName
                    val label = game.label
                    val profile = profilesMap[pkg] ?: repo.createDefaultEffectiveProfile(pkg)
                    GameLibraryCard(
                        profile = profile,
                        appName = label,
                        iconBitmap = game.icon,
                        onClick = { onGameClick(pkg) },
                        onLaunchClick = {
                            val launchIntent = pm.getLaunchIntentForPackage(pkg)
                            if (launchIntent != null) {
                                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(launchIntent)
                            } else {
                                Toast.makeText(
                                    context,
                                    context.resources.getString(R.string.game_library_cannot_launch, label),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                    )
                }

                // Empty state if no games are added yet
                if (installedGames.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SportsEsports,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp),
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = stringResource(R.string.game_library_empty_title),
                                    style = TextStyle(
                                        fontFamily = GoogleSansFlexRounded,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 18.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = stringResource(R.string.game_library_empty_desc),
                                    style = TextStyle(
                                        fontFamily = GoogleSansFlexRounded,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}
