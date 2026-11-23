package com.pixel.gamebooster.utils

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
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

    // Root support removed: no su binary on test devices, and root is not a
    // supported privilege path for this app. Shizuku is the only privileged backend.

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
        hasShizukuPermission()

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
        // Shizuku is the only supported privileged backend. If the user has not
        // granted Pixel Game Booster permission in Shizuku settings, commands
        // silently fail — surface this so the user knows what's wrong.
        if (!hasShizukuPermission()) {
            Log.w(TAG, "Shizuku permission not granted. Cannot execute privileged command: $command")
            return false
        }

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
            false
        }
    }
}
