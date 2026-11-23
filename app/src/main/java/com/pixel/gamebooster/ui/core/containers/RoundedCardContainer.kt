package com.pixel.gamebooster.ui.core.containers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun RoundedCardContainer(
    modifier: Modifier = Modifier,
    spacing: Dp = 2.dp,
    cornerRadius: Dp = 24.dp,
    containerColor: Color = Color.Transparent,
    backgroundColor: Color = containerColor,
    content: @Composable ColumnScope.() -> Unit,
) {
    val effectiveColor = if (containerColor != Color.Transparent) containerColor else backgroundColor
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(cornerRadius))
                .background(effectiveColor),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}
