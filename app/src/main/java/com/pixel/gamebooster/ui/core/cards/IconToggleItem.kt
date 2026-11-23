package com.pixel.gamebooster.ui.core.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixel.gamebooster.R
import com.pixel.gamebooster.ui.core.dialogs.FeatureInfoDialog
import com.pixel.gamebooster.utils.HapticUtil

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IconToggleItem(
    title: String,
    subtitle: String = "",
    icon: ImageVector? = null,
    checked: Boolean = false,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
    index: Int? = null,
    count: Int? = null,
    onInfoClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    description: String? = null,
    command: String? = null,
    isChecked: Boolean? = null,
    onClick: (() -> Unit)? = null,
    showToggle: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val view = LocalView.current
    val effectiveSubtitle = description ?: subtitle
    val effectiveChecked = isChecked ?: checked
    var showInfoDialog by remember { mutableStateOf(false) }

    // When checked, morph to 24dp rounded corners like Essentials
    val itemShape = if (effectiveChecked) {
        RoundedCornerShape(24.dp)
    } else {
        when {
            index == null || count == null || count == 1 -> RoundedCornerShape(24.dp)
            index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
            index == count - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
            else -> RoundedCornerShape(4.dp)
        }
    }

    val containerColor = if (effectiveChecked) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    val headlineColor = if (effectiveChecked) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val supportingColor = if (effectiveChecked) {
        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val hasInfoAction = onInfoClick != null || !command.isNullOrEmpty()

    ListItem(
        verticalAlignment = Alignment.CenterVertically,
        supportingContent = if (effectiveSubtitle.isNotBlank()) {
            {
                Text(
                    text = effectiveSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = supportingColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else null,
        leadingContent = if (icon != null) {
            {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (effectiveChecked) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (effectiveChecked) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        } else null,
        trailingContent = {
            if (showToggle && onCheckedChange != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasInfoAction) {
                        IconButton(
                            onClick = {
                                HapticUtil.performUIHaptic(view)
                                if (onInfoClick != null) {
                                    onInfoClick()
                                } else {
                                    showInfoDialog = true
                                }
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = stringResource(R.string.card_toggle_cd_details),
                                tint = if (effectiveChecked) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        VerticalDivider(
                            modifier = Modifier
                                .height(28.dp)
                                .width(1.dp)
                                .padding(horizontal = 4.dp),
                            color = if (effectiveChecked) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                    Switch(
                        checked = effectiveChecked,
                        onCheckedChange = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onCheckedChange(it)
                        },
                        enabled = enabled,
                    )
                }
            } else if (trailingContent != null) {
                trailingContent()
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = containerColor,
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(itemShape)
            .clickable(enabled = enabled) {
                HapticUtil.performVirtualKeyHaptic(view)
                if (onCheckedChange != null) {
                    onCheckedChange(!effectiveChecked)
                } else {
                    onClick?.invoke()
                }
            },
        content = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (effectiveChecked) FontWeight.SemiBold else FontWeight.Medium,
                color = headlineColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )

    if (showInfoDialog && !command.isNullOrEmpty()) {
        FeatureInfoDialog(
            title = title,
            description = effectiveSubtitle,
            command = command,
            onDismissRequest = { showInfoDialog = false },
        )
    }
}
