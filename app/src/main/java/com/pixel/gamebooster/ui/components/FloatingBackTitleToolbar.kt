package com.pixel.gamebooster.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixel.gamebooster.R
import com.pixel.gamebooster.ui.theme.GoogleSansFlexRounded
import com.pixel.gamebooster.ui.util.rememberAdaptiveLayoutInfo
import com.pixel.gamebooster.utils.HapticUtil

/**
 * Reusable floating bottom navigation toolbar for detail and sub-setting screens,
 * matching Pixel Game Booster and Essentials design language:
 * - Floating vibrant pill container at bottom center with system navigation bar insets
 * - Circular back button pill with tactile haptic feedback
 * - Bold title in contrasting GoogleSansFlexRounded typography
 * - Optional trailing actions slot for custom buttons/icons
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FloatingBackTitleToolbar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    val view = LocalView.current
    val adaptive = rememberAdaptiveLayoutInfo()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = if (adaptive.isCompactHeight) 2.dp else 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        HorizontalFloatingToolbar(
            expanded = true,
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = if (adaptive.isCompactHeight) 2.dp else 8.dp,
            ),
            colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(
                toolbarContentColor = MaterialTheme.colorScheme.onSurface,
                toolbarContainerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Back button circular pill
                IconButton(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onBack()
                    },
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.game_tuning_cd_back),
                        modifier = Modifier.size(24.dp),
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = title,
                    style = TextStyle(
                        fontFamily = GoogleSansFlexRounded,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    ),
                    color = MaterialTheme.colorScheme.background,
                    maxLines = 1,
                    modifier = Modifier.padding(end = if (trailingContent != null) 8.dp else 16.dp),
                )

                if (trailingContent != null) {
                    trailingContent()
                }
            }
        }
    }
}
