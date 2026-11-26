package com.pixel.gamebooster.overlay.ui

import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixel.gamebooster.R
import com.pixel.gamebooster.ui.theme.GoogleSansFlexRounded
import com.pixel.gamebooster.utils.HapticUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CollapsedPill(
    visible: Boolean = true,
    isDockedOnRight: Boolean,
    overlayStyle: Int = 0,
    pillOpacity: Float = 0.94f,
    showLiveFps: Boolean = false,
    currentFps: Int = 60,
    overlayColorMode: Int = 0,
    overlayCustomColor: Int = 0xFF4285F4.toInt(),
    isRecording: Boolean = false,
    doubleTapToStopEnabled: Boolean = true,
    doubleTapAction: com.pixel.gamebooster.data.model.OverlayGestureAction = com.pixel.gamebooster.data.model.OverlayGestureAction.NONE,
    longTapAction: com.pixel.gamebooster.data.model.OverlayGestureAction = com.pixel.gamebooster.data.model.OverlayGestureAction.NONE,
    doubleTapDelayMs: Int = 200,
    onExpand: () -> Unit,
    onStopRecording: () -> Unit = {},
    onGestureAction: (com.pixel.gamebooster.data.model.OverlayGestureAction) -> Unit = {},
    onDragStart: () -> Pair<Int, Int> = { 0 to 0 },
    onDrag: (Int, Int) -> Unit,
    onDragEnd: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val isCircle = overlayStyle == 1
    var isInteracting by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isIdle by remember { mutableStateOf(false) }

    val dragState = remember {
        object {
            var downRawX = 0f
            var downRawY = 0f
            var startWinX = 0
            var startWinY = 0
            var lastTargetX = 0
            var lastTargetY = 0
            var isDragging = false
            var longPressTriggered = false
            var lastTapUpTime = 0L
            var pendingTapJob: Job? = null
            var longPressJob: Job? = null
        }
    }
    val dragThresholdPx = with(density) { 8.dp.toPx() }
    val longPressTimeoutMs = remember { ViewConfiguration.getLongPressTimeout().toLong() }

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
                width = when {
                    isCircle && showLiveFps -> 60.dp
                    isCircle -> 52.dp
                    showLiveFps -> 58.dp
                    else -> 28.dp
                },
                height = if (isCircle && showLiveFps) 60.dp else 56.dp,
            )
            .alpha(pillAlpha)
            .pointerInteropFilter { event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        dragState.downRawX = event.rawX
                        dragState.downRawY = event.rawY
                        dragState.isDragging = false
                        dragState.longPressTriggered = false
                        dragState.longPressJob?.cancel()
                        dragState.longPressJob = if (longTapAction != com.pixel.gamebooster.data.model.OverlayGestureAction.NONE) {
                            scope.launch {
                                delay(longPressTimeoutMs)
                                if (!dragState.isDragging) {
                                    dragState.longPressTriggered = true
                                    dragState.pendingTapJob?.cancel()
                                    dragState.pendingTapJob = null
                                    dragState.lastTapUpTime = 0L
                                    HapticUtil.performToggle(view)
                                    if (longTapAction == com.pixel.gamebooster.data.model.OverlayGestureAction.REVEAL_OVERLAY) {
                                        onExpand()
                                    } else {
                                        onGestureAction(longTapAction)
                                    }
                                }
                            }
                        } else {
                            null
                        }
                        lastInteractionTime = System.currentTimeMillis()
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (dragState.longPressTriggered) {
                            return@pointerInteropFilter true
                        }
                        val dx = event.rawX - dragState.downRawX
                        val dy = event.rawY - dragState.downRawY
                        if (!dragState.isDragging && (abs(dx) > dragThresholdPx || abs(dy) > dragThresholdPx)) {
                            dragState.longPressJob?.cancel()
                            dragState.longPressJob = null
                            dragState.isDragging = true
                            dragState.pendingTapJob?.cancel()
                            dragState.pendingTapJob = null
                            val pos = onDragStart()
                            dragState.startWinX = pos.first
                            dragState.startWinY = pos.second
                            dragState.lastTargetX = pos.first
                            dragState.lastTargetY = pos.second
                            isInteracting = true
                            isIdle = false
                            HapticUtil.performLightHaptic(view)
                        }
                        if (dragState.isDragging) {
                            dragState.lastTargetX = (dragState.startWinX + dx).roundToInt()
                            dragState.lastTargetY = (dragState.startWinY + dy).roundToInt()
                            onDrag(dragState.lastTargetX, dragState.lastTargetY)
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        dragState.longPressJob?.cancel()
                        dragState.longPressJob = null
                        lastInteractionTime = System.currentTimeMillis()
                        if (dragState.longPressTriggered) {
                            dragState.longPressTriggered = false
                        } else if (dragState.isDragging) {
                            dragState.isDragging = false
                            isInteracting = false
                            onDragEnd(dragState.lastTargetX, dragState.lastTargetY)
                        } else {
                            val isStopRecordingDoubleTap = isRecording && doubleTapToStopEnabled
                            val hasDoubleTap = isStopRecordingDoubleTap ||
                                doubleTapAction != com.pixel.gamebooster.data.model.OverlayGestureAction.NONE
                            if (hasDoubleTap) {
                                val now = System.currentTimeMillis()
                                val timeoutMs = doubleTapDelayMs.toLong()
                                if (now - dragState.lastTapUpTime <= timeoutMs) {
                                    dragState.pendingTapJob?.cancel()
                                    dragState.pendingTapJob = null
                                    dragState.lastTapUpTime = 0L
                                    HapticUtil.performToggle(view)
                                    when {
                                        isStopRecordingDoubleTap -> onStopRecording()
                                        doubleTapAction == com.pixel.gamebooster.data.model.OverlayGestureAction.REVEAL_OVERLAY -> onExpand()
                                        else -> onGestureAction(doubleTapAction)
                                    }
                                } else {
                                    dragState.lastTapUpTime = now
                                    dragState.pendingTapJob?.cancel()
                                    if (!isStopRecordingDoubleTap &&
                                        doubleTapAction == com.pixel.gamebooster.data.model.OverlayGestureAction.REVEAL_OVERLAY
                                    ) {
                                        // Single tap intentionally ignored when Reveal Overlay is bound to double tap
                                        dragState.pendingTapJob = null
                                    } else {
                                        dragState.pendingTapJob = scope.launch {
                                            delay(timeoutMs)
                                            HapticUtil.performUIHaptic(view)
                                            onExpand()
                                        }
                                    }
                                }
                            } else {
                                HapticUtil.performUIHaptic(view)
                                onExpand()
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        dragState.longPressJob?.cancel()
                        dragState.longPressJob = null
                        dragState.longPressTriggered = false
                        lastInteractionTime = System.currentTimeMillis()
                        if (dragState.isDragging) {
                            dragState.isDragging = false
                            isInteracting = false
                            onDragEnd(dragState.lastTargetX, dragState.lastTargetY)
                        }
                        true
                    }
                    else -> false
                }
            },
        contentAlignment = if (isCircle) Alignment.Center else if (isDockedOnRight) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        val edgeOrigin = remember(isDockedOnRight) {
            TransformOrigin(if (isDockedOnRight) 1f else 0f, 0.5f)
        }
        val enterAnim = remember(isCircle, isDockedOnRight) {
            if (isCircle) {
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
        }
        val exitAnim = remember(isCircle, isDockedOnRight) {
            if (isCircle) {
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
        }

        AnimatedVisibility(
            visible = visible,
            enter = enterAnim,
            exit = exitAnim,
        ) {
            val triggerColor = if (overlayColorMode == 1) Color(overlayCustomColor) else MaterialTheme.colorScheme.primary
            val accentTint = if (isRecording) MaterialTheme.colorScheme.error else triggerColor
            val fpsTextColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            if (isCircle) {
                Box(
                    modifier = Modifier
                        .size(if (showLiveFps) 54.dp else 46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (showLiveFps) {
                        Text(
                            text = "$currentFps",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = GoogleSansFlexRounded,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                            ),
                            color = fpsTextColor,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.SportsEsports,
                            contentDescription = stringResource(R.string.overlay_cd_trigger),
                            tint = accentTint,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            } else if (showLiveFps) {
                val edgePillShape = if (isDockedOnRight) {
                    RoundedCornerShape(topStart = 19.dp, bottomStart = 19.dp, topEnd = 8.dp, bottomEnd = 8.dp)
                } else {
                    RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 19.dp, bottomEnd = 19.dp)
                }
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(edgePillShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$currentFps",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = GoogleSansFlexRounded,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        ),
                        color = fpsTextColor,
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(width = 8.dp, height = 48.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(accentTint.copy(alpha = 0.88f))
                )
            }
        }
    }
}



