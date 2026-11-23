package com.pixel.gamebooster.overlay

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.overlay.model.OverlayDisplayState
import com.pixel.gamebooster.overlay.ui.GameOverlayHost
import com.pixel.gamebooster.ui.theme.PixelGameBoosterTheme
import com.pixel.gamebooster.utils.GameDetectionUtils
import com.pixel.gamebooster.utils.HapticUtil

object GameOverlayManager {
    private const val TAG = "PixelGameBooster:Overlay"

    @Volatile
    var isOverlayShowing: Boolean = false
        private set

    var windowManager: WindowManager? = null
        private set

    var composeView: ComposeView? = null
        private set

    var lifecycleOwner: OverlayLifecycleOwner? = null
        private set

    var currentParams: WindowManager.LayoutParams? = null
        private set

    var overlayViewModel: OverlayViewModel? = null
        private set

    var isRightSide: Boolean = false
        private set

    var savedPillX: Int = 0
        private set

    var savedPillY: Int = 0
        private set

    private var hasUserPositionedPill: Boolean = false
    private var appContext: Context? = null
    private var configCallbacks: ComponentCallbacks? = null

    fun clampY(
        rawY: Int,
        screenHeight: Int,
        safeTop: Int = 0,
        safeBottom: Int = 0,
        minOffset: Int = 16,
        maxOffset: Int = 64,
    ): Int {
        val minAllowed = safeTop + minOffset
        val maxAllowed = (screenHeight - safeBottom - maxOffset).coerceAtLeast(minAllowed)
        return rawY.coerceIn(minAllowed, maxAllowed)
    }

    fun show(context: Context, packageName: String, repo: SettingsRepository) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            ContextCompat.getMainExecutor(context).execute {
                show(context, packageName, repo)
            }
            return
        }

        if (composeView != null) {
            Log.d(TAG, "Overlay is already showing for $packageName")
            return
        }

        // T-02-04: Reject unverified package registrations
        if (!GameDetectionUtils.isGame(context, packageName, repo.gameApps.value)) {
            Log.w(TAG, "Rejected overlay show for unverified package: $packageName")
            return
        }

        appContext = context.applicationContext
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        if (wm == null) {
            Log.e(TAG, "Unable to obtain WindowManager")
            return
        }
        windowManager = wm

        val owner = OverlayLifecycleOwner().apply {
            onCreate()
            onResume()
        }
        lifecycleOwner = owner

        val vm = OverlayViewModel(context.applicationContext, packageName, repo)
        overlayViewModel = vm

        val density = context.resources.displayMetrics.density
        val initialY = (100 * density).toInt()
        hasUserPositionedPill = false
        val (screenWidth, _) = getScreenSizePx(context, wm)
        isRightSide = true
        vm.setDockedOnRight(true)
        val pillWidth = getExpectedPillWidthPx(context)
        savedPillX = (screenWidth - pillWidth).coerceAtLeast(0)
        savedPillY = initialY

        val params = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
            gravity = Gravity.TOP or Gravity.START
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            x = savedPillX
            y = initialY
        }
        currentParams = params

        val view = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setContent {
                PixelGameBoosterTheme(
                    dynamicColor = true,
                    darkTheme = isSystemInDarkTheme(),
                ) {
                    GameOverlayHost(
                        viewModel = vm,
                        onUpdateWindowParams = { block ->
                            updateWindowParams(block)
                        },
                        onDismiss = {
                            hide()
                        }
                    )
                }
            }
        }
        composeView = view
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            vm.setImeVisible(imeVisible)
            val (screenWidth, screenHeight) = getScreenSizePx(v.context, windowManager)
            if (!hasUserPositionedPill) {
                val cutoutOnLeft = isCutoutOnLeft(insets.displayCutout, screenWidth, screenHeight)
                if (cutoutOnLeft != null && isRightSide != cutoutOnLeft) {
                    isRightSide = cutoutOnLeft
                    vm.setDockedOnRight(cutoutOnLeft)
                }
            }
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val p = currentParams
            if (p != null && p.width != WindowManager.LayoutParams.MATCH_PARENT) {
                val pillWidth = getExpectedPillWidthPx(v.context)
                val minOffset = (16 * v.resources.displayMetrics.density).toInt()
                val maxOffset = (64 * v.resources.displayMetrics.density).toInt()
                val clampedX = if (isRightSide) {
                    (screenWidth - pillWidth - cutout.right).coerceAtLeast(cutout.left)
                } else {
                    cutout.left
                }
                val clampedY = clampY(
                    rawY = savedPillY,
                    screenHeight = screenHeight,
                    safeTop = cutout.top,
                    safeBottom = cutout.bottom,
                    minOffset = minOffset,
                    maxOffset = maxOffset,
                )
                if (p.x != clampedX || p.y != clampedY) {
                    p.x = clampedX
                    p.y = clampedY
                    savedPillX = clampedX
                    savedPillY = clampedY
                    runCatching { windowManager?.updateViewLayout(v, p) }
                }
            }
            insets
        }

        runCatching {
            wm.addView(view, params)
            isOverlayShowing = true
            val callbacks = object : ComponentCallbacks {
                override fun onConfigurationChanged(newConfig: Configuration) {
                    view.post { realignCollapsedToEdge() }
                }
                override fun onLowMemory() {}
            }
            appContext?.registerComponentCallbacks(callbacks)
            configCallbacks = callbacks
            view.postDelayed({ realignCollapsedToEdge() }, 250L)
            view.postDelayed({ realignCollapsedToEdge() }, 650L)
            Log.d(TAG, "Overlay shown for $packageName")
        }.onFailure { e ->
            Log.e(TAG, "Failed to add overlay view to WindowManager", e)
            hide()
        }
    }

    private fun getExpectedPillWidthPx(context: Context): Int {
        val density = context.resources.displayMetrics.density
        val state = overlayViewModel?.uiState?.value
        val isCircle = state?.overlayStyle == 1
        val showLiveFps = state?.showLiveFps == true
        return when {
            isCircle && showLiveFps -> (60 * density).toInt()
            isCircle -> (52 * density).toInt()
            showLiveFps -> (58 * density).toInt()
            else -> (28 * density).toInt()
        }
    }

    private fun isCutoutOnLeft(
        cutout: androidx.core.view.DisplayCutoutCompat?,
        screenWidth: Int,
        screenHeight: Int,
    ): Boolean? {
        if (cutout == null) return null
        if (screenWidth > screenHeight) {
            if (cutout.safeInsetLeft > 0 && cutout.safeInsetRight == 0) return true
            if (cutout.safeInsetRight > 0 && cutout.safeInsetLeft == 0) return false
        }
        val rect = cutout.boundingRects.firstOrNull() ?: return null
        val centerTolerance = (12 * (appContext?.resources?.displayMetrics?.density ?: 2.5f)).toInt()
        return when {
            rect.centerX() < (screenWidth / 2) - centerTolerance -> true
            rect.centerX() > (screenWidth / 2) + centerTolerance -> false
            else -> null
        }
    }

    private fun getScreenSizePx(context: Context, wm: WindowManager?): Pair<Int, Int> {
        val manager = wm ?: (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && manager != null) {
            val bounds = manager.currentWindowMetrics.bounds
            return bounds.width() to bounds.height()
        }
        val dm = context.resources.displayMetrics
        return dm.widthPixels to dm.heightPixels
    }

    private fun realignCollapsedToEdge() {
        val params = currentParams ?: return
        val wm = windowManager ?: return
        val view = composeView ?: return
        val vm = overlayViewModel ?: return
        if (params.width == WindowManager.LayoutParams.MATCH_PARENT) return

        val (screenWidth, screenHeight) = getScreenSizePx(view.context, wm)
        val rootInsets = ViewCompat.getRootWindowInsets(view)
        val displayCutout = rootInsets?.displayCutout
        if (!hasUserPositionedPill) {
            val cutoutOnLeft = isCutoutOnLeft(displayCutout, screenWidth, screenHeight)
            if (cutoutOnLeft != null && isRightSide != cutoutOnLeft) {
                isRightSide = cutoutOnLeft
                vm.setDockedOnRight(cutoutOnLeft)
            }
        }
        val cutout = rootInsets?.getInsets(WindowInsetsCompat.Type.displayCutout()) ?: Insets.NONE
        val pillWidth = getExpectedPillWidthPx(view.context)
        val density = view.resources.displayMetrics.density
        val targetX = if (isRightSide) {
            (screenWidth - pillWidth - cutout.right).coerceAtLeast(cutout.left)
        } else {
            cutout.left
        }
        val targetY = clampY(
            rawY = savedPillY,
            screenHeight = screenHeight,
            safeTop = cutout.top,
            safeBottom = cutout.bottom,
            minOffset = (16 * density).toInt(),
            maxOffset = (64 * density).toInt(),
        )
        if (params.x != targetX || params.y != targetY) {
            params.x = targetX
            params.y = targetY
            savedPillX = targetX
            savedPillY = targetY
            runCatching { wm.updateViewLayout(view, params) }
        }
    }

    private fun getCutoutSafeInsets(view: View): Insets {
        return ViewCompat.getRootWindowInsets(view)
            ?.getInsets(WindowInsetsCompat.Type.displayCutout())
            ?: Insets.NONE
    }

    fun updateLayout(newDisplayState: OverlayDisplayState, blurRadiusPx: Int = 30) {
        val params = currentParams ?: return
        val wm = windowManager ?: return
        val view = composeView ?: return
        val ctx = appContext ?: view.context
        val cutout = getCutoutSafeInsets(view)

        when (newDisplayState) {
            OverlayDisplayState.COLLAPSED_PILL,
            OverlayDisplayState.TOOLBAR -> {
                val (screenWidth, screenHeight) = getScreenSizePx(ctx, wm)
                val density = view.resources.displayMetrics.density
                val pillWidth = getExpectedPillWidthPx(ctx)
                params.width = WindowManager.LayoutParams.WRAP_CONTENT
                params.height = WindowManager.LayoutParams.WRAP_CONTENT
                params.x = if (isRightSide) {
                    (screenWidth - pillWidth - cutout.right).coerceAtLeast(cutout.left)
                } else {
                    cutout.left
                }
                params.y = clampY(
                    rawY = savedPillY,
                    screenHeight = screenHeight,
                    safeTop = cutout.top,
                    safeBottom = cutout.bottom,
                    minOffset = (16 * density).toInt(),
                    maxOffset = (64 * density).toInt(),
                )
                params.flags = (params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    params.flags = params.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
                    params.setBlurBehindRadius(0)
                }
            }
            OverlayDisplayState.EXPANDED_PANEL -> {
                val (screenWidth, _) = getScreenSizePx(ctx, wm)
                val pillWidth = getExpectedPillWidthPx(ctx)
                savedPillX = if (isRightSide) (screenWidth - pillWidth) else 0
                params.x = 0
                params.y = 0
                params.width = WindowManager.LayoutParams.MATCH_PARENT
                params.height = WindowManager.LayoutParams.MATCH_PARENT
                params.flags = (params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()) or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    params.flags = params.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
                    params.setBlurBehindRadius(0)
                }
            }
        }

        runCatching {
            wm.updateViewLayout(view, params)
        }.onFailure { e ->
            Log.e(TAG, "Failed to update overlay layout params", e)
        }
    }

    fun updatePillPosition(deltaX: Float, deltaY: Float, safeTop: Int = 0, safeBottom: Int = 0, screenHeight: Int = 0) {
        val params = currentParams ?: return
        val wm = windowManager ?: return
        val view = composeView ?: return
        hasUserPositionedPill = true

        val (screenWidth, fallbackHeight) = getScreenSizePx(view.context, wm)
        val actualScreenHeight = if (screenHeight > 0) screenHeight else fallbackHeight
        val density = view.resources.displayMetrics.density
        val minOffset = (16 * density).toInt()
        val maxOffset = (64 * density).toInt()
        val cutout = getCutoutSafeInsets(view)
        val resolvedSafeTop = if (safeTop > 0) safeTop else cutout.top
        val resolvedSafeBottom = if (safeBottom > 0) safeBottom else cutout.bottom
        val pillWidth = getExpectedPillWidthPx(view.context)
        val maxX = (screenWidth - pillWidth - cutout.right).coerceAtLeast(cutout.left)

        params.x = (params.x + deltaX.toInt()).coerceIn(cutout.left, maxX)
        params.y = clampY(
            rawY = params.y + deltaY.toInt(),
            screenHeight = actualScreenHeight,
            safeTop = resolvedSafeTop,
            safeBottom = resolvedSafeBottom,
            minOffset = minOffset,
            maxOffset = maxOffset,
        )
        savedPillX = params.x
        savedPillY = params.y

        runCatching {
            wm.updateViewLayout(view, params)
        }.onFailure { e ->
            Log.e(TAG, "Failed to update pill position", e)
        }
    }

    fun snapToNearestEdge() {
        val params = currentParams ?: return
        val view = composeView ?: return
        hasUserPositionedPill = true

        val (screenWidth, _) = getScreenSizePx(view.context, windowManager)
        val pillWidth = getExpectedPillWidthPx(view.context)
        val centerX = params.x + (pillWidth / 2)
        val toRight = centerX >= screenWidth / 2

        snapToSide(toRight)
    }

    fun snapToSide(toRight: Boolean) {
        val params = currentParams ?: return
        val wm = windowManager ?: return
        val view = composeView ?: return

        val (screenWidth, _) = getScreenSizePx(view.context, wm)
        val pillWidth = getExpectedPillWidthPx(view.context)
        val cutout = getCutoutSafeInsets(view)

        isRightSide = toRight
        params.x = if (toRight) (screenWidth - pillWidth - cutout.right).coerceAtLeast(cutout.left) else cutout.left
        savedPillX = params.x
        if (params.width != WindowManager.LayoutParams.MATCH_PARENT && params.y > 0) {
            savedPillY = params.y
        }

        overlayViewModel?.setDockedOnRight(toRight)
        HapticUtil.performLightHaptic(view)

        runCatching {
            wm.updateViewLayout(view, params)
        }.onFailure { e ->
            Log.e(TAG, "Failed to snap pill to side $toRight", e)
        }
    }

    fun updatePillY(deltaY: Float, safeTop: Int = 0, safeBottom: Int = 0, screenHeight: Int = 0) {
        updatePillPosition(0f, deltaY, safeTop, safeBottom, screenHeight)
    }

    fun updateWindowParams(block: (WindowManager.LayoutParams) -> Unit) {
        val params = currentParams ?: return
        val wm = windowManager ?: return
        val view = composeView ?: return
        block(params)
        runCatching {
            wm.updateViewLayout(view, params)
        }.onFailure { e ->
            Log.e(TAG, "Failed to update custom window params", e)
        }
    }

    fun hide() {
        val ctx = appContext
        if (Looper.myLooper() != Looper.getMainLooper()) {
            if (ctx != null) {
                ContextCompat.getMainExecutor(ctx).execute { hide() }
            }
            return
        }

        val view = composeView ?: return
        val wm = windowManager
        val owner = lifecycleOwner
        val vm = overlayViewModel

        vm?.onCleared()
        owner?.onPause()
        owner?.onDestroy()

        runCatching {
            configCallbacks?.let { cb -> appContext?.unregisterComponentCallbacks(cb) }
        }
        configCallbacks = null

        runCatching {
            wm?.removeView(view)
        }.onFailure { e ->
            Log.e(TAG, "Failed to remove overlay view from WindowManager", e)
        }

        composeView = null
        windowManager = null
        lifecycleOwner = null
        currentParams = null
        overlayViewModel = null
        isOverlayShowing = false
        hasUserPositionedPill = false
        appContext = null
        Log.d(TAG, "Overlay hidden and destroyed successfully")
    }
}
