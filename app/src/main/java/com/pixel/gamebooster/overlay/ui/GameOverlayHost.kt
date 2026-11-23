package com.pixel.gamebooster.overlay.ui

import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.pixel.gamebooster.overlay.GameOverlayManager
import com.pixel.gamebooster.overlay.OverlayViewModel
import com.pixel.gamebooster.overlay.model.OverlayDisplayState
import kotlinx.coroutines.delay

@Composable
fun GameOverlayHost(
    viewModel: OverlayViewModel,
    onUpdateWindowParams: (((WindowManager.LayoutParams) -> Unit) -> Unit)? = null,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.displayState) {
        if (uiState.displayState == OverlayDisplayState.EXPANDED_PANEL) {
            GameOverlayManager.updateLayout(OverlayDisplayState.EXPANDED_PANEL)
        } else {
            // Keep window MATCH_PARENT during the exit slide/fade animation, then shrink
            delay(220)
            GameOverlayManager.updateLayout(OverlayDisplayState.COLLAPSED_PILL)
        }
    }

    AnimatedContent(
        targetState = uiState.displayState,
        transitionSpec = {
            if (targetState == OverlayDisplayState.EXPANDED_PANEL) {
                (slideInHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    initialOffsetX = { if (uiState.isDockedOnRight) it / 2 else -it / 2 },
                ) + fadeIn(animationSpec = tween(220)))
                    .togetherWith(
                        fadeOut(animationSpec = tween(100))
                    )
            } else {
                fadeIn(animationSpec = tween(150, delayMillis = 80))
                    .togetherWith(
                        slideOutHorizontally(
                            animationSpec = tween(200, easing = FastOutSlowInEasing),
                            targetOffsetX = { if (uiState.isDockedOnRight) it / 2 else -it / 2 },
                        ) + fadeOut(animationSpec = tween(200))
                    )
            }
        },
        label = "overlay_display_state",
        modifier = modifier,
    ) { state ->
        when (state) {
            OverlayDisplayState.COLLAPSED_PILL -> {
                CollapsedPill(
                    isDockedOnRight = uiState.isDockedOnRight,
                    overlayStyle = uiState.overlayStyle,
                    onExpand = { viewModel.setDisplayState(OverlayDisplayState.EXPANDED_PANEL) },
                    onDrag = { deltaX, deltaY -> GameOverlayManager.updatePillPosition(deltaX, deltaY) },
                    onDragEnd = { GameOverlayManager.snapToNearestEdge() }
                )
            }
            OverlayDisplayState.TOOLBAR,
            OverlayDisplayState.EXPANDED_PANEL -> {
                ExpandedPanel(
                    state = uiState,
                    viewModel = viewModel,
                    onDismiss = { viewModel.setDisplayState(OverlayDisplayState.COLLAPSED_PILL) }
                )
            }
        }
    }
}

