package com.pixel.gamebooster.overlay

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Rect
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

    @Volatile
    private var isOverlayRequested: Boolean = false

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
    private var isDraggingPill: Boolean = false
    private var appContext: Context? = null
    private var configCallbacks: ComponentCallbacks? = null
    private var activePackageName: String? = null

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
        isOverlayRequested = true
        if (Looper.myLooper() != Looper.getMainLooper()) {
            ContextCompat.getMainExecutor(context).execute {
                if (isOverlayRequested) {
                    show(context, packageName, repo)
                }
            }
            return
        }

        if (!isOverlayRequested) return

        if (composeView != null) {
            if (activePackageName == packageName) {
                Log.d(TAG, "Overlay is already showing for $packageName")
                return
            }
            Log.d(TAG, "Switching overlay from $activePackageName to $packageName")
            hide()
            isOverlayRequested = true
        }

        // T-02-04: Reject unverified package registrations
        if (!GameDetectionUtils.isGame(context, packageName, repo.gameApps.value)) {
            Log.w(TAG, "Rejected overlay show for unverified package: $packageName")
            isOverlayRequested = false
            return
        }

        appContext = context.applicationContext
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        if (wm == null) {
            Log.e(TAG, "Unable to obtain WindowManager")
            isOverlayRequested = false
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
        isRightSide = true
        vm.setDockedOnRight(true)
        savedPillX = 0
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
            gravity = Gravity.TOP or Gravity.END
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            x = 0
            y = initialY
        }
        currentParams = params

        val view = ComposeView(context.applicationContext).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            var lastExclusionWidth = -1
            var lastExclusionHeight = -1
            addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                if (v.width > 0 && v.height > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (v.width != lastExclusionWidth || v.height != lastExclusionHeight) {
                        lastExclusionWidth = v.width
                        lastExclusionHeight = v.height
                        v.setSystemGestureExclusionRects(listOf(Rect(0, 0, v.width, v.height)))
                    }
                }
            }
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
            if (!isDraggingPill && p != null && p.width != WindowManager.LayoutParams.MATCH_PARENT) {
                val minOffset = (16 * v.resources.displayMetrics.density).toInt()
                val maxOffset = (64 * v.resources.displayMetrics.density).toInt()
                val targetGravity = Gravity.TOP or (if (isRightSide) Gravity.END else Gravity.START)
                val clampedY = clampY(
                    rawY = savedPillY,
                    screenHeight = screenHeight,
                    safeTop = cutout.top,
                    safeBottom = cutout.bottom,
                    minOffset = minOffset,
                    maxOffset = maxOffset,
                )
                if (p.gravity != targetGravity || p.x != 0 || p.y != clampedY) {
                    p.gravity = targetGravity
                    p.x = 0
                    p.y = clampedY
                    savedPillX = 0
                    savedPillY = clampedY
                    runCatching { windowManager?.updateViewLayout(v, p) }
                }
            }
            insets
        }

        runCatching {
            wm.addView(view, params)
            isOverlayShowing = true
            activePackageName = packageName
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
        if (isDraggingPill) return
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
        val density = view.resources.displayMetrics.density
        val targetGravity = Gravity.TOP or (if (isRightSide) Gravity.END else Gravity.START)
        val targetY = clampY(
            rawY = savedPillY,
            screenHeight = screenHeight,
            safeTop = cutout.top,
            safeBottom = cutout.bottom,
            minOffset = (16 * density).toInt(),
            maxOffset = (64 * density).toInt(),
        )
        if (params.gravity != targetGravity || params.x != 0 || params.y != targetY) {
            params.gravity = targetGravity
            params.x = 0
            params.y = targetY
            savedPillX = 0
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
                val (_, screenHeight) = getScreenSizePx(ctx, wm)
                val density = view.resources.displayMetrics.density
                params.width = WindowManager.LayoutParams.WRAP_CONTENT
                params.height = WindowManager.LayoutParams.WRAP_CONTENT
                params.gravity = Gravity.TOP or (if (isRightSide) Gravity.END else Gravity.START)
                params.x = 0
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
                savedPillX = 0
                params.gravity = Gravity.TOP or Gravity.START
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

    fun startPillDrag(): Pair<Int, Int> {
        isDraggingPill = true
        hasUserPositionedPill = true
        val params = currentParams ?: return 0 to 0
        val wm = windowManager ?: return 0 to 0
        val view = composeView ?: return 0 to 0

        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        params.gravity = Gravity.TOP or Gravity.START
        params.x = loc[0]
        params.y = loc[1]
        savedPillX = loc[0]
        savedPillY = loc[1]
        runCatching { wm.updateViewLayout(view, params) }
        return loc[0] to loc[1]
    }

    fun dragPillTo(targetX: Int, targetY: Int) {
        val params = currentParams ?: return
        val wm = windowManager ?: return
        val view = composeView ?: return
        hasUserPositionedPill = true

        val (screenWidth, screenHeight) = getScreenSizePx(view.context, wm)
        val density = view.resources.displayMetrics.density
        val minOffset = (16 * density).toInt()
        val maxOffset = (64 * density).toInt()
        val cutout = getCutoutSafeInsets(view)
        val pillWidth = view.width.takeIf { it > 0 } ?: getExpectedPillWidthPx(view.context)
        val maxX = (screenWidth - pillWidth).coerceAtLeast(0)

        val newX = targetX.coerceIn(0, maxX)
        val newY = clampY(
            rawY = targetY,
            screenHeight = screenHeight,
            safeTop = cutout.top,
            safeBottom = cutout.bottom,
            minOffset = minOffset,
            maxOffset = maxOffset,
        )
        if (params.x == newX && params.y == newY) return
        params.x = newX
        params.y = newY
        savedPillX = newX
        savedPillY = newY

        runCatching {
            wm.updateViewLayout(view, params)
        }.onFailure { e ->
            Log.e(TAG, "Failed to drag pill", e)
        }
    }

    fun endPillDrag(finalX: Int, finalY: Int) {
        isDraggingPill = false
        val view = composeView ?: return
        val (screenWidth, screenHeight) = getScreenSizePx(view.context, windowManager)
        val density = view.resources.displayMetrics.density
        val cutout = getCutoutSafeInsets(view)
        val pillWidth = view.width.takeIf { it > 0 } ?: getExpectedPillWidthPx(view.context)
        val centerX = finalX + (pillWidth / 2)
        val toRight = centerX >= (screenWidth / 2)

        savedPillY = clampY(
            rawY = finalY,
            screenHeight = screenHeight,
            safeTop = cutout.top,
            safeBottom = cutout.bottom,
            minOffset = (16 * density).toInt(),
            maxOffset = (64 * density).toInt(),
        )
        snapToSide(toRight)
    }

    fun updatePillPosition(deltaX: Float, deltaY: Float, safeTop: Int = 0, safeBottom: Int = 0, screenHeight: Int = 0) {
        val params = currentParams ?: return
        dragPillTo(
            targetX = kotlin.math.round(params.x + deltaX).toInt(),
            targetY = kotlin.math.round(params.y + deltaY).toInt(),
        )
    }

    fun snapToNearestEdge() {
        val params = currentParams ?: return
        val view = composeView ?: return
        hasUserPositionedPill = true

        val (screenWidth, _) = getScreenSizePx(view.context, windowManager)
        val pillWidth = view.width.takeIf { it > 0 } ?: getExpectedPillWidthPx(view.context)
        val centerX = params.x + (pillWidth / 2)
        val toRight = centerX >= screenWidth / 2

        snapToSide(toRight)
    }

    fun snapToSide(toRight: Boolean) {
        val params = currentParams ?: return
        val wm = windowManager ?: return
        val view = composeView ?: return

        val (_, screenHeight) = getScreenSizePx(view.context, wm)
        val density = view.resources.displayMetrics.density
        val cutout = getCutoutSafeInsets(view)

        isRightSide = toRight
        params.gravity = Gravity.TOP or (if (toRight) Gravity.END else Gravity.START)
        params.x = 0
        params.y = clampY(
            rawY = savedPillY,
            screenHeight = screenHeight,
            safeTop = cutout.top,
            safeBottom = cutout.bottom,
            minOffset = (16 * density).toInt(),
            maxOffset = (64 * density).toInt(),
        )
        savedPillX = 0
        savedPillY = params.y

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
        isOverlayRequested = false
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
        activePackageName = null
        hasUserPositionedPill = false
        isDraggingPill = false
        appContext = null
        Log.d(TAG, "Overlay hidden and destroyed successfully")
    }
}
