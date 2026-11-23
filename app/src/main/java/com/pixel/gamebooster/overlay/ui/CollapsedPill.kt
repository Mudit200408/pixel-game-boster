package com.pixel.gamebooster.overlay.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.pixel.gamebooster.utils.HapticUtil
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CollapsedPill(
    isDockedOnRight: Boolean,
    overlayStyle: Int = 0,
    isRecording: Boolean = false,
    doubleTapToStopEnabled: Boolean = true,
    onExpand: () -> Unit,
    onStopRecording: () -> Unit = {},
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val isCircle = overlayStyle == 1
    var isInteracting by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isIdle by remember { mutableStateOf(false) }

    LaunchedEffect(lastInteractionTime, isInteracting) {
        if (!isInteracting) {
            delay(3500)
            isIdle = true
        } else {
            isIdle = false
        }
    }

    val pillAlpha by animateFloatAsState(
        targetValue = if (isIdle) 0.40f else 0.94f,
        animationSpec = tween(durationMillis = 500),
        label = "pill_idle_alpha",
    )

    Box(
        modifier = modifier
            .padding(vertical = 4.dp)
            .size(
                width = if (isCircle) 52.dp else 28.dp,
                height = 56.dp,
            )
            .alpha(pillAlpha)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isInteracting = true
                        isIdle = false
                        lastInteractionTime = System.currentTimeMillis()
                        HapticUtil.performLightHaptic(view)
                    },
                    onDragEnd = {
                        isInteracting = false
                        lastInteractionTime = System.currentTimeMillis()
                        onDragEnd()
                    },
                    onDragCancel = {
                        isInteracting = false
                        lastInteractionTime = System.currentTimeMillis()
                        onDragEnd()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        lastInteractionTime = System.currentTimeMillis()
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                )
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    HapticUtil.performUIHaptic(view)
                    onExpand()
                },
                onDoubleClick = if (isRecording && doubleTapToStopEnabled) {
                    {
                        HapticUtil.performToggle(view)
                        onStopRecording()
                    }
                } else null,
            ),
        contentAlignment = if (isCircle) Alignment.Center else if (isDockedOnRight) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        if (isCircle) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.SportsEsports,
                    contentDescription = "Game Booster Overlay",
                    tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(width = 8.dp, height = 48.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        (if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                            .copy(alpha = 0.88f)
                    )
            )
        }
    }
}



