package com.pixel.gamebooster.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.DataOutputStream
import java.util.concurrent.Executors

object ShellUtils {
    private const val TAG = "PixelGameBooster:Shell"
    private val commandExecutor = Executors.newSingleThreadExecutor { r -> Thread(r, "gamebooster-shell") }

    fun isShizukuAvailable(): Boolean =
        try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }

    fun hasShizukuPermission(): Boolean {
        if (!isShizukuAvailable()) return false
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    fun isRootAvailable(): Boolean =
        try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "which su"))
            process.waitFor() == 0
        } catch (_: Throwable) {
            false
        }

    fun isRootPermissionGranted(): Boolean =
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            process.waitFor() == 0
        } catch (_: Throwable) {
            false
        }

    fun runCommandWithOutput(
        context: Context,
        command: String,
        notifyOnError: Boolean = false,
    ): String? =
        try {
            val process = if (hasShizukuPermission()) {
                com.pixel.gamebooster.shizuku.ShizukuProcessHelper.newProcess(
                    arrayOf("sh", "-c", command),
                    null,
                    "/",
                )
            } else if (isRootPermissionGranted()) {
                Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            } else null

            process?.let {
                val output = it.inputStream.bufferedReader().use { reader -> reader.readText() }.trim()
                it.waitFor()
                it.destroy()
                output
            }
        } catch (e: Exception) {
            Log.e(TAG, "runCommandWithOutput failed for $command", e)
            null
        }

    fun hasPermission(context: Context): Boolean =
        hasShizukuPermission() || isRootPermissionGranted()

    fun runCommand(
        context: Context,
        command: String,
        onComplete: ((Boolean) -> Unit)? = null,
    ) {
        commandExecutor.execute {
            val success = runCommandBlocking(command)
            onComplete?.invoke(success)
        }
    }

    suspend fun execute(command: String): Boolean = withContext(Dispatchers.IO) {
        runCommandBlocking(command)
    }

    fun runCommandBlocking(command: String): Boolean {
        Log.d(TAG, "Executing: $command")
        // Try Shizuku first
        if (hasShizukuPermission()) {
            return try {
                val process = com.pixel.gamebooster.shizuku.ShizukuProcessHelper.newProcess(
                    arrayOf("sh", "-c", command),
                    null,
                    "/",
                )
                val exitCode = process?.waitFor() ?: -1
                if (exitCode != 0 && process != null) {
                    val err = process.errorStream.bufferedReader().readText()
                    Log.e(TAG, "Shizuku error ($exitCode): $err")
                }
                exitCode == 0
            } catch (e: Throwable) {
                Log.e(TAG, "Shizuku execution failed", e)
                runCommandRoot(command)
            }
        }

        // Fallback to Root
        return runCommandRoot(command)
    }

    private fun runCommandRoot(command: String): Boolean {
        var process: Process? = null
        var os: DataOutputStream? = null
        return try {
            process = Runtime.getRuntime().exec("su")
            os = DataOutputStream(process.outputStream)
            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()
            val exit = process.waitFor()
            exit == 0
        } catch (e: Throwable) {
            Log.e(TAG, "Root execution failed", e)
            false
        } finally {
            runCatching { os?.close() }
            runCatching { process?.destroy() }
        }
    }
}
