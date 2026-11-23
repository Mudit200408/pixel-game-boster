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
    var isWindowExpanded by remember { mutableStateOf(false) }

    val handleDismiss: () -> Unit = {
        scope.launch {
            panelVisible = false
            delay(190) // Wait for exit spring retraction animation to finish
            viewModel.setDisplayState(OverlayDisplayState.COLLAPSED_PILL)
            GameOverlayManager.updateLayout(OverlayDisplayState.COLLAPSED_PILL)
            isWindowExpanded = false
        }
    }

    LaunchedEffect(uiState.displayState) {
        if (uiState.displayState == OverlayDisplayState.EXPANDED_PANEL) {
            isWindowExpanded = true
            GameOverlayManager.updateLayout(OverlayDisplayState.EXPANDED_PANEL)
            // Yield one frame so ExpandedPanel mounts with visible=false first,
            // ensuring AnimatedVisibility triggers the full reveal spring animation
            delay(25)
            panelVisible = true
        } else if (uiState.displayState == OverlayDisplayState.COLLAPSED_PILL && !panelVisible) {
            isWindowExpanded = false
            GameOverlayManager.updateLayout(OverlayDisplayState.COLLAPSED_PILL)
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
            isDockedOnRight = uiState.isDockedOnRight,
            overlayStyle = uiState.overlayStyle,
            isRecording = uiState.isRecording,
            doubleTapToStopEnabled = uiState.recordDoubleTapStop,
            onExpand = {
                viewModel.setDisplayState(OverlayDisplayState.EXPANDED_PANEL)
            },
            onStopRecording = {
                viewModel.stopRecording()
            },
            onDrag = { deltaX, deltaY -> GameOverlayManager.updatePillPosition(deltaX, deltaY) },
            onDragEnd = { GameOverlayManager.snapToNearestEdge() },
            modifier = modifier,
        )
    }
}


