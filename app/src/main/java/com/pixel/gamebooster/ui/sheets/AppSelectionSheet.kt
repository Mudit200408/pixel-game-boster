package com.pixel.gamebooster.ui.sheets

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixel.gamebooster.R
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.ui.util.AppInfoCache
import com.pixel.gamebooster.ui.util.rememberAdaptiveLayoutInfo
import com.pixel.gamebooster.utils.HapticUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledAppItem(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
    val isSystem: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionSheet(
    repo: SettingsRepository,
    onDismissRequest: () -> Unit,
    title: String = stringResource(R.string.app_select_default_title),
    isGamePicker: Boolean = false,
    selectedAppsOverride: Set<String>? = null,
    onToggleAppOverride: ((String) -> Unit)? = null,
    excludedPackages: Set<String> = emptySet(),
) {
    val context = LocalContext.current
    val view = LocalView.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var showSystemApps by remember { mutableStateOf(false) }
    var appsList by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val suspendedApps by repo.suspendedApps.collectAsState()
    val gameApps by repo.gameApps.collectAsState()
    val effectiveSuspendedApps = selectedAppsOverride ?: suspendedApps

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val items = packages
                .filter { it.packageName != context.packageName }
                .map { appInfo ->
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val cached = AppInfoCache.loadFromApplicationInfo(pm, appInfo, iconSizePx = 96)
                    InstalledAppItem(appInfo.packageName, cached.label, cached.icon, isSystem)
                }
                .sortedBy { it.label.lowercase() }
            appsList = items
            isLoading = false
        }
    }

    val filteredApps = remember(appsList, searchQuery, showSystemApps, excludedPackages) {
        appsList.filter { item ->
            val matchesSearch = item.label.contains(searchQuery, ignoreCase = true) ||
                item.packageName.contains(searchQuery, ignoreCase = true)
            val matchesSystem = showSystemApps || !item.isSystem
            val notExcluded = item.packageName !in excludedPackages
            matchesSearch && matchesSystem && notExcluded
        }
    }

    val adaptive = rememberAdaptiveLayoutInfo()
    val configuration = LocalConfiguration.current
    val maxListHeight = when {
        adaptive.isCompactHeight -> 240.dp
        adaptive.isTablet -> (configuration.screenHeightDp * 0.50f).dp
        else -> 450.dp
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        sheetMaxWidth = 600.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.app_select_search_hint)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Clear, contentDescription = stringResource(R.string.app_select_cd_clear))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.app_select_include_system),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Switch(
                    checked = showSystemApps,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        showSystemApps = it
                    },
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxListHeight),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val isSelected = if (isGamePicker) {
                            gameApps.contains(app.packageName)
                        } else {
                            effectiveSuspendedApps.contains(app.packageName)
                        }

                        val itemShape = if (isSelected) RoundedCornerShape(24.dp) else RoundedCornerShape(16.dp)
                        val containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        }
                        val titleColor = if (isSelected) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                        val subtitleColor = if (isSelected) {
                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(itemShape)
                                .background(containerColor)
                                .clickable {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    if (isGamePicker) {
                                        if (isSelected) repo.removeGame(app.packageName)
                                        else repo.addGame(app.packageName)
                                    } else if (onToggleAppOverride != null) {
                                        onToggleAppOverride(app.packageName)
                                    } else {
                                        repo.toggleSuspendedApp(app.packageName)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (app.icon != null) {
                                Image(
                                    bitmap = app.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = titleColor,
                                )
                                Text(
                                    text = app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = subtitleColor,
                                )
                            }

                            Switch(
                                checked = isSelected,
                                onCheckedChange = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    if (isGamePicker) {
                                        if (it) repo.addGame(app.packageName)
                                        else repo.removeGame(app.packageName)
                                    } else if (onToggleAppOverride != null) {
                                        onToggleAppOverride(app.packageName)
                                    } else {
                                        repo.toggleSuspendedApp(app.packageName)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
