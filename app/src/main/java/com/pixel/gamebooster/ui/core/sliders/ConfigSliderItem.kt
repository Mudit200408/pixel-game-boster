package com.pixel.gamebooster.ui.core.sliders

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pixel.gamebooster.utils.HapticUtil
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConfigSliderItem(
    title: String,
    subtitle: String = "",
    value: Float = 0f,
    onValueChange: (Float) -> Unit = {},
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    steps: Int = 0,
    increment: Float = 1f,
    valueFormatter: (Float) -> String = { it.toInt().toString() },
    onValueChangeFinished: (() -> Unit)? = null,
    enabled: Boolean = true,
    index: Int? = null,
    count: Int? = null,
    description: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val view = LocalView.current
    val effectiveSubtitle = description ?: subtitle
    var showValueEntryDialog by remember { mutableStateOf(false) }

    val itemShape = when {
        index == null || count == null || count == 1 -> RoundedCornerShape(24.dp)
        index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        index == count - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
        else -> RoundedCornerShape(4.dp)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(itemShape)
            .background(MaterialTheme.colorScheme.surfaceBright)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (effectiveSubtitle.isNotBlank()) {
                    Text(
                        text = effectiveSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Value badge with long-press support for manual numeric entry
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .combinedClickable(
                        enabled = enabled,
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            showValueEntryDialog = true
                        },
                        onLongClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            showValueEntryDialog = true
                        },
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = valueFormatter(value),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            trailingContent?.invoke()
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Precision Stepper Row: [-] Stepper, Slider, [+] Stepper
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Decrement Button (-)
            IconButton(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    val newValue = (
                        BigDecimal.valueOf(value.toDouble())
                            .subtract(BigDecimal.valueOf(increment.toDouble()))
                            .setScale(2, RoundingMode.HALF_UP)
                    ).toFloat().coerceIn(valueRange)
                    onValueChange(newValue)
                    onValueChangeFinished?.invoke()
                },
                enabled = enabled && value > valueRange.start,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = "Decrease",
                    tint = if (enabled && value > valueRange.start) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                )
            }

            // Interactive Slider with Tick Haptics
            Slider(
                value = value,
                onValueChange = {
                    if (it != value) {
                        HapticUtil.performSliderHaptic(view)
                    }
                    onValueChange(it)
                },
                valueRange = valueRange,
                steps = steps,
                onValueChangeFinished = onValueChangeFinished,
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
            )

            // Increment Button (+)
            IconButton(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    val newValue = (
                        BigDecimal.valueOf(value.toDouble())
                            .add(BigDecimal.valueOf(increment.toDouble()))
                            .setScale(2, RoundingMode.HALF_UP)
                    ).toFloat().coerceIn(valueRange)
                    onValueChange(newValue)
                    onValueChangeFinished?.invoke()
                },
                enabled = enabled && value < valueRange.endInclusive,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Increase",
                    tint = if (enabled && value < valueRange.endInclusive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                )
            }
        }
    }

    // Direct numeric input dialog on long-press
    if (showValueEntryDialog) {
        var textInput by remember(value) { mutableStateOf(value.toString()) }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showValueEntryDialog = false },
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Range: ${valueRange.start} – ${valueRange.endInclusive}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { input ->
                            textInput = input
                            isError = input.toFloatOrNull()?.let { it !in valueRange } ?: true
                        },
                        isError = isError,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                val parsed = textInput.toFloatOrNull()
                                if (parsed != null) {
                                    val coerced = parsed.coerceIn(valueRange)
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    onValueChange(coerced)
                                    onValueChangeFinished?.invoke()
                                    showValueEntryDialog = false
                                } else {
                                    isError = true
                                }
                            },
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsed = textInput.toFloatOrNull()
                        if (parsed != null) {
                            val coerced = parsed.coerceIn(valueRange)
                            HapticUtil.performVirtualKeyHaptic(view)
                            onValueChange(coerced)
                            onValueChangeFinished?.invoke()
                            showValueEntryDialog = false
                        } else {
                            isError = true
                        }
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showValueEntryDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
    }
}
