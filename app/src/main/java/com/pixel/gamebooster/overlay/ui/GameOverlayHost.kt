package com.pixel.gamebooster.overlay.ui

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.pixel.gamebooster.overlay.GameOverlayManager
import com.pixel.gamebooster.overlay.OverlayViewModel
import com.pixel.gamebooster.overlay.model.OverlayDisplayState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GameOverlayHost(
    viewModel: OverlayViewModel,
    onUpdateWindowParams: (((WindowManager.LayoutParams) -> Unit) -> Unit)? = null,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var panelVisible by remember { mutableStateOf(false) }
    var pillVisible by remember { mutableStateOf(true) }
    var isWindowExpanded by remember { mutableStateOf(false) }
    var isTransitioning by remember { mutableStateOf(false) }

    val handleExpand: () -> Unit = {
        if (!isTransitioning && !isWindowExpanded) {
            scope.launch {
                isTransitioning = true
                pillVisible = false
                delay(150) // Let pill/button play its quick Lottie-matched hide animation
                viewModel.setDisplayState(OverlayDisplayState.EXPANDED_PANEL)
                isWindowExpanded = true
                GameOverlayManager.updateLayout(OverlayDisplayState.EXPANDED_PANEL)
                delay(24) // Wait for WindowManager MATCH_PARENT commit before revealing panel
                panelVisible = true
                isTransitioning = false
            }
        }
    }

    val handleDismiss: () -> Unit = {
        if (!isTransitioning && isWindowExpanded) {
            scope.launch {
                isTransitioning = true
                panelVisible = false
                delay(190) // Wait for panel exit spring retraction
                pillVisible = false
                viewModel.setDisplayState(OverlayDisplayState.COLLAPSED_PILL)
                isWindowExpanded = false
                GameOverlayManager.updateLayout(OverlayDisplayState.COLLAPSED_PILL)
                delay(48) // Let WindowManager finish resizing/positioning at savedPillX/Y while pill is invisible
                pillVisible = true
                isTransitioning = false
            }
        }
    }

    LaunchedEffect(uiState.displayState) {
        if (uiState.displayState == OverlayDisplayState.EXPANDED_PANEL && !isWindowExpanded && !isTransitioning) {
            pillVisible = false
            isWindowExpanded = true
            GameOverlayManager.updateLayout(OverlayDisplayState.EXPANDED_PANEL)
            delay(24)
            panelVisible = true
        } else if (uiState.displayState == OverlayDisplayState.COLLAPSED_PILL && isWindowExpanded && !isTransitioning) {
            handleDismiss()
        }
    }

    if (isWindowExpanded) {
        ExpandedPanel(
            state = uiState,
            viewModel = viewModel,
            visible = panelVisible,
            onDismiss = handleDismiss,
            modifier = modifier,
        )
    } else {
        CollapsedPill(
            visible = pillVisible && !uiState.isImeVisible,
            isDockedOnRight = uiState.isDockedOnRight,
            overlayStyle = uiState.overlayStyle,
            pillOpacity = uiState.overlayOpacity,
            showLiveFps = uiState.showLiveFps,
            currentFps = uiState.currentFps,
            overlayColorMode = uiState.overlayColorMode,
            overlayCustomColor = uiState.overlayCustomColor,
            isRecording = uiState.isRecording,
            doubleTapToStopEnabled = uiState.recordDoubleTapStop,
            onExpand = handleExpand,
            onStopRecording = {
                viewModel.stopRecording()
            },
            onDragStart = { GameOverlayManager.startPillDrag() },
            onDrag = { targetX, targetY -> GameOverlayManager.dragPillTo(targetX, targetY) },
            onDragEnd = { finalX, finalY -> GameOverlayManager.endPillDrag(finalX, finalY) },
            modifier = modifier,
        )
    }
}


