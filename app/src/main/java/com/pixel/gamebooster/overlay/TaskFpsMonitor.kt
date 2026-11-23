package com.pixel.gamebooster.overlay

import android.app.ActivityManager
import android.content.Context
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import com.pixel.gamebooster.utils.ShellUtils
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import kotlin.math.roundToInt

/**
 * Native live FPS monitor using Android's system TaskFpsCallback (same mechanism as
 * Google Game Dashboard and AOSP GameSpace), invoked via ShizukuBinderWrapper since
 * com.android.shell holds android.permission.ACCESS_FPS_COUNTER.
 */
object TaskFpsMonitor {
    private const val TAG = "PixelGameBooster:Fps"
    private const val DESCRIPTOR = "android.window.ITaskFpsCallback"
    private const val TRANSACTION_ON_FPS_REPORTED = IBinder.FIRST_CALL_TRANSACTION

    @Volatile
    private var isExempted = false

    @Volatile
    private var activeIwm: Any? = null

    @Volatile
    private var activeCallbackProxy: Any? = null

    @Volatile
    private var activeTaskId: Int = -1

    private fun ensureHiddenApiBypass() {
        if (!isExempted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching { HiddenApiBypass.addHiddenApiExemptions("") }
            isExempted = true
        }
    }

    @Synchronized
    fun register(
        context: Context,
        packageName: String,
        onFps: (Int) -> Unit,
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        if (!ShellUtils.hasShizukuPermission() || packageName.isBlank()) return false

        ensureHiddenApiBypass()

        val taskId = resolveTaskId(context, packageName)
        if (taskId <= 0) {
            Log.w(TAG, "Could not resolve taskId for $packageName")
            return false
        }

        if (activeTaskId == taskId && activeCallbackProxy != null) {
            return true
        }

        unregister()

        return try {
            val rawWm = SystemServiceHelper.getSystemService("window") ?: return false
            val wmBinder = ShizukuBinderWrapper(rawWm)
            val iwmStub = Class.forName("android.view.IWindowManager\$Stub")
            val iwm = iwmStub.getMethod("asInterface", IBinder::class.java).invoke(null, wmBinder)
                ?: return false

            val localBinder = object : Binder() {
                override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
                    if (code == TRANSACTION_ON_FPS_REPORTED) {
                        data.enforceInterface(DESCRIPTOR)
                        val fps = data.readFloat()
                        if (!fps.isNaN() && fps >= 0f) {
                            onFps(fps.roundToInt())
                        }
                        return true
                    }
                    return super.onTransact(code, data, reply, flags)
                }
            }

            val callbackInterface = Class.forName("android.window.ITaskFpsCallback")
            val callbackStub = Class.forName("android.window.ITaskFpsCallback\$Stub")
            val callbackProxy = callbackStub.getMethod("asInterface", IBinder::class.java)
                .invoke(null, localBinder) ?: return false

            val registerMethod = iwm.javaClass.getMethod(
                "registerTaskFpsCallback",
                Int::class.javaPrimitiveType,
                callbackInterface,
            )
            registerMethod.invoke(iwm, taskId, callbackProxy)

            activeIwm = iwm
            activeCallbackProxy = callbackProxy
            activeTaskId = taskId
            Log.d(TAG, "Registered native TaskFpsCallback for $packageName (taskId=$taskId)")
            true
        } catch (t: Throwable) {
            Log.w(TAG, "registerTaskFpsCallback failed for $packageName", t)
            activeIwm = null
            activeCallbackProxy = null
            activeTaskId = -1
            false
        }
    }

    @Synchronized
    fun unregister() {
        val iwm = activeIwm
        val proxy = activeCallbackProxy
        activeIwm = null
        activeCallbackProxy = null
        activeTaskId = -1
        if (iwm == null || proxy == null) return

        runCatching {
            val callbackInterface = Class.forName("android.window.ITaskFpsCallback")
            val unregisterMethod = iwm.javaClass.getMethod(
                "unregisterTaskFpsCallback",
                callbackInterface,
            )
            unregisterMethod.invoke(iwm, proxy)
            Log.d(TAG, "Unregistered native TaskFpsCallback")
        }
    }

    private fun resolveTaskId(context: Context, packageName: String): Int {
        // 1. Fast Binder query via IActivityTaskManager.getTasks(...)
        val binderTaskId = runCatching {
            val rawAtm = SystemServiceHelper.getSystemService("activity_task") ?: return@runCatching -1
            val atmBinder = ShizukuBinderWrapper(rawAtm)
            val atmStub = Class.forName("android.app.IActivityTaskManager\$Stub")
            val atm = atmStub.getMethod("asInterface", IBinder::class.java).invoke(null, atmBinder)
                ?: return@runCatching -1

            @Suppress("UNCHECKED_CAST")
            val tasks: List<ActivityManager.RunningTaskInfo>? = atm.javaClass.methods
                .firstOrNull { it.name == "getTasks" && it.parameterCount == 1 }
                ?.invoke(atm, 15) as? List<ActivityManager.RunningTaskInfo>
                ?: atm.javaClass.methods
                    .firstOrNull { it.name == "getTasks" && it.parameterCount == 4 }
                    ?.invoke(atm, 15, false, false, -1) as? List<ActivityManager.RunningTaskInfo>

            tasks?.firstOrNull {
                it.topActivity?.packageName == packageName ||
                    it.baseActivity?.packageName == packageName
            }?.taskId ?: -1
        }.getOrDefault(-1)

        if (binderTaskId > 0) return binderTaskId

        // 2. Shell fallback if getTasks signature differs on a specific OS build
        val out = ShellUtils.runCommandWithOutput(
            context,
            "am stack list | grep -F \"$packageName\" | head -n 1",
            notifyOnError = false,
        )
        if (!out.isNullOrBlank()) {
            val match = Regex("""taskId=(\d+)""").find(out)
                ?: Regex("""#(\d+)""").find(out)
            val parsed = match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: -1
            if (parsed > 0) return parsed
        }
        return -1
    }
}
