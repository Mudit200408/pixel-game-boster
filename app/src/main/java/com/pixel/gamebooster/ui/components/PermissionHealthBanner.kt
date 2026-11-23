package com.pixel.gamebooster.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pixel.gamebooster.R
import com.pixel.gamebooster.utils.HapticUtil
import com.pixel.gamebooster.utils.PermissionHelper
import com.pixel.gamebooster.utils.ShellUtils

@Composable
fun PermissionHealthBanner(
    onFixPermissionsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasShizuku by remember { mutableStateOf(ShellUtils.hasPermission(context)) }
    var hasAccessibility by remember { mutableStateOf(PermissionHelper.isAccessibilityServiceEnabled(context)) }
    var hasUsageStats by remember { mutableStateOf(PermissionHelper.hasUsageStatsPermission(context)) }
    var hasDnd by remember { mutableStateOf(PermissionHelper.hasDndAccess(context)) }

    fun refreshHealth() {
        hasShizuku = ShellUtils.hasPermission(context)
        hasAccessibility = PermissionHelper.isAccessibilityServiceEnabled(context)
        hasUsageStats = PermissionHelper.hasUsageStatsPermission(context)
        hasDnd = PermissionHelper.hasDndAccess(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshHealth()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // If critical permissions are satisfied, banner is hidden
    if (hasShizuku && hasAccessibility && hasDnd) {
        return
    }

    val (title, description) = when {
        !hasDnd -> Pair(
            stringResource(R.string.banner_dnd_title),
            stringResource(R.string.banner_dnd_desc),
        )
        !hasShizuku -> Pair(
            stringResource(R.string.banner_shizuku_revoked_title),
            stringResource(R.string.banner_shizuku_revoked_desc),
        )
        !hasAccessibility -> Pair(
            stringResource(R.string.banner_accessibility_disabled_title),
            stringResource(R.string.banner_accessibility_disabled_desc),
        )
        else -> Pair(
            stringResource(R.string.banner_permissions_incomplete_title),
            stringResource(R.string.banner_permissions_incomplete_desc),
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            Button(
                onClick = {
                    HapticUtil.performClick(view)
                    onFixPermissionsClick()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.banner_fix_permissions),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
