package com.pixel.gamebooster.overlay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoNotDisturbOff
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixel.gamebooster.R
import com.pixel.gamebooster.overlay.model.OverlayUiState
import com.pixel.gamebooster.ui.theme.GoogleSansFlexRounded
import com.pixel.gamebooster.utils.HapticUtil

@Composable
fun FloatingToolbar(
    state: OverlayUiState,
    onToggleDnd: () -> Unit,
    onExpandPanel: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onToggle120Hz: (() -> Unit)? = null,
) {
    val view = LocalView.current

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        modifier = modifier.padding(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            // Mode Badge
            val modeText = when (state.interventionMode) {
                3 -> stringResource(R.string.dash_mode_battery)
                1 -> stringResource(R.string.dash_mode_standard)
                else -> stringResource(R.string.dash_mode_performance)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = modeText,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = GoogleSansFlexRounded,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            // FPS Badge
            val fpsText = if (state.fpsOverride > 0) {
                stringResource(R.string.overlay_fps_badge, state.fpsOverride)
            } else {
                stringResource(R.string.overlay_fps_auto)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = fpsText,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = GoogleSansFlexRounded,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // 120Hz Toggle Button (if available)
            if (onToggle120Hz != null) {
                IconButton(
                    onClick = {
                        HapticUtil.performToggle(view)
                        onToggle120Hz()
                    },
                    modifier = Modifier.size(36.dp),
                    colors = if (state.lockRefreshRate) {
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    } else {
                        IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                ) {
                    Text(
                        text = "120",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = GoogleSansFlexRounded,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }

            // Quick DND Toggle Button
            IconButton(
                onClick = {
                    HapticUtil.performToggle(view)
                    onToggleDnd()
                },
                modifier = Modifier.size(36.dp),
                colors = if (state.dndEnabled) {
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                } else {
                    IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            ) {
                Icon(
                    imageVector = if (state.dndEnabled) Icons.Rounded.DoNotDisturbOn else Icons.Rounded.DoNotDisturbOff,
                    contentDescription = stringResource(R.string.overlay_dnd_title),
                    modifier = Modifier.size(20.dp),
                )
            }

            // Expand Chevron Button
            IconButton(
                onClick = {
                    HapticUtil.performUIHaptic(view)
                    onExpandPanel()
                },
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = stringResource(R.string.overlay_expand_cd),
                    modifier = Modifier.size(20.dp),
                )
            }

            // Collapse / Close Button
            IconButton(
                onClick = {
                    HapticUtil.performUIHaptic(view)
                    onDismiss()
                },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.overlay_collapse_cd),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
