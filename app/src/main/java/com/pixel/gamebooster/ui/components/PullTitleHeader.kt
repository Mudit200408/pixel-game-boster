package com.pixel.gamebooster.ui.components

import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixel.gamebooster.R
import com.pixel.gamebooster.utils.HapticUtil
import kotlin.math.roundToInt

private const val PULL_TITLE_FONT_STEPS = 20
private val pullTitleFontFamilyCache = arrayOfNulls<FontFamily>(PULL_TITLE_FONT_STEPS + 1)

@OptIn(ExperimentalTextApi::class)
private fun getPullTitleFontFamily(step: Int): FontFamily {
    pullTitleFontFamilyCache[step]?.let { return it }
    val quantizedFraction = step / PULL_TITLE_FONT_STEPS.toFloat()
    val created = FontFamily(
        Font(
            R.font.google_sans_flex,
            variationSettings = FontVariation.Settings(
                FontVariation.width((100f + quantizedFraction * 20f).coerceIn(100f, 120f)),
                FontVariation.weight((600 + (quantizedFraction * 100).toInt()).coerceIn(600, 700)),
                FontVariation.Setting("ROND", 0f),
            ),
        ),
    )
    pullTitleFontFamilyCache[step] = created
    return created
}

@Stable
class PullTitleHeaderState(
    val minHeight: Dp = 44.dp,
    val maxHeight: Dp = 76.dp,
    initialHeight: Dp = minHeight,
    private val density: Density,
    private val view: View? = null,
) {
    var height by mutableStateOf(initialHeight)
        internal set

    val fraction: Float
        get() = if (maxHeight > minHeight) {
            ((height - minHeight) / (maxHeight - minHeight)).coerceIn(0f, 1f)
        } else 0f

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.y
            // When user scrolls down (content moves up), collapse header first
            if (delta < 0 && height > minHeight) {
                val oldHeight = height
                height = with(density) { (oldHeight.toPx() + delta).toDp() }.coerceAtLeast(minHeight)
                val consumed = oldHeight - height
                return Offset(0f, with(density) { -consumed.toPx() })
            }
            return Offset.Zero
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            val delta = available.y
            // When user scrolls up (content moves down) and list is at top, expand header
            if (delta > 0 && source == NestedScrollSource.UserInput) {
                val oldHeight = height
                height = with(density) { (oldHeight.toPx() + delta).toDp() }.coerceAtMost(maxHeight)
                if (height == maxHeight && oldHeight < maxHeight && view != null) {
                    HapticUtil.performLightHaptic(view)
                }
                val produced = height - oldHeight
                return Offset(0f, with(density) { produced.toPx() })
            }
            return Offset.Zero
        }
    }
}

@Composable
fun rememberPullTitleHeaderState(
    minHeight: Dp = 44.dp,
    maxHeight: Dp = 76.dp,
    initialHeight: Dp = maxHeight,
): PullTitleHeaderState {
    val density = LocalDensity.current
    val view = LocalView.current
    return remember(minHeight, maxHeight, initialHeight, density, view) {
        PullTitleHeaderState(
            minHeight = minHeight,
            maxHeight = maxHeight,
            initialHeight = initialHeight,
            density = density,
            view = view,
        )
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun PullTitleHeader(
    title: String,
    state: PullTitleHeaderState,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    val fraction = state.fraction
    val safeFraction = fraction.coerceIn(0f, 1f)
    val fontStep = (safeFraction * PULL_TITLE_FONT_STEPS).roundToInt().coerceIn(0, PULL_TITLE_FONT_STEPS)
    val fontFamily = remember(fontStep) { getPullTitleFontFamily(fontStep) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(state.height)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = fontFamily,
                fontSize = (22f + safeFraction * 12f).sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            ),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Start,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
