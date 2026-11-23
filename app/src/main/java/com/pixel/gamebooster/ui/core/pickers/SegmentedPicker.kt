package com.pixel.gamebooster.ui.core.pickers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixel.gamebooster.utils.HapticUtil

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> SegmentedPicker(
    options: List<T> = emptyList(),
    selectedOption: T? = null,
    onOptionSelected: (T) -> Unit = {},
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier,
    iconProvider: ((T) -> ImageVector?)? = null,
    items: List<T> = options,
    selectedItem: T? = selectedOption,
    onItemSelected: ((T) -> Unit)? = null,
    activeColor: Color = MaterialTheme.colorScheme.primary,
) {
    val view = LocalView.current
    val effectiveItems = if (items.isNotEmpty()) items else options
    val effectiveSelected = selectedItem ?: selectedOption
    val effectiveCallback = onItemSelected ?: onOptionSelected

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        effectiveItems.forEachIndexed { index, option ->
            val isSelected = option == effectiveSelected
            val shapes = when (index) {
                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                effectiveItems.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            }

            ToggleButton(
                checked = isSelected,
                onCheckedChange = {
                    if (!isSelected) {
                        HapticUtil.performUIHaptic(view)
                        effectiveCallback(option)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .semantics { role = Role.RadioButton },
                shapes = shapes,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    val icon = iconProvider?.invoke(option)
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = labelProvider(option),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
