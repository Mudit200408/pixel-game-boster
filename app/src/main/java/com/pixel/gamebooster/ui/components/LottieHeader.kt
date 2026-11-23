package com.pixel.gamebooster.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Deprecated("Replaced by PixelGamingHeroCard and PullTitleHeader per D-06")
@Composable
fun LottieHeader(
    resId: Int = 0,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    cornerRadius: Dp = 28.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(0.dp),
    )
}
