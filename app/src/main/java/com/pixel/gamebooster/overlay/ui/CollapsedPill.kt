package com.pixel.gamebooster.overlay.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.pixel.gamebooster.utils.HapticUtil
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CollapsedPill(
    visible: Boolean = true,
    isDockedOnRight: Boolean,
    overlayStyle: Int = 0,
    pillOpacity: Float = 0.94f,
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

    val restingAlpha = pillOpacity.coerceIn(0.20f, 1.0f)
    val pillAlpha by animateFloatAsState(
        targetValue = if (isInteracting) restingAlpha.coerceAtLeast(0.75f) else if (isIdle) restingAlpha else (restingAlpha + 0.06f).coerceAtMost(1.0f),
        animationSpec = tween(durationMillis = 400),
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
        val edgeOrigin = TransformOrigin(if (isDockedOnRight) 1f else 0f, 0.5f)
        val enterAnim = if (isCircle) {
            scaleIn(
                animationSpec = spring(dampingRatio = 0.68f, stiffness = 480f),
                initialScale = 0f,
                transformOrigin = TransformOrigin.Center,
            ) + fadeIn(animationSpec = tween(durationMillis = 150))
        } else {
            slideInHorizontally(
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 480f),
                initialOffsetX = { w -> if (isDockedOnRight) w else -w },
            ) + scaleIn(
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 480f),
                initialScale = 0.2f,
                transformOrigin = edgeOrigin,
            ) + fadeIn(animationSpec = tween(durationMillis = 140))
        }
        val exitAnim = if (isCircle) {
            scaleOut(
                animationSpec = tween(durationMillis = 130, easing = FastOutLinearInEasing),
                targetScale = 0f,
                transformOrigin = TransformOrigin.Center,
            ) + fadeOut(animationSpec = tween(durationMillis = 110))
        } else {
            slideOutHorizontally(
                animationSpec = tween(durationMillis = 130, easing = FastOutLinearInEasing),
                targetOffsetX = { w -> if (isDockedOnRight) w else -w },
            ) + scaleOut(
                animationSpec = tween(durationMillis = 120, easing = FastOutLinearInEasing),
                targetScale = 0.2f,
                transformOrigin = edgeOrigin,
            ) + fadeOut(animationSpec = tween(durationMillis = 100))
        }

        AnimatedVisibility(
            visible = visible,
            enter = enterAnim,
            exit = exitAnim,
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
}



