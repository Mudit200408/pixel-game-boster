package com.pixel.gamebooster.utils

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object ShellUtils {
    private const val TAG = "PixelGameBooster:Shell"
    private const val COMMAND_TIMEOUT_SECONDS = 10L
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
                try {
                    val output = it.inputStream.bufferedReader().use { reader -> reader.readText() }.trim()
                    runCatching { it.errorStream.close() }
                    it.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    output
                } finally {
                    it.destroy()
                }
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
            ) ?: return false
            try {
                // Drain stdout and stderr before waiting so full pipes cannot deadlock waitFor().
                runCatching { process.inputStream.bufferedReader().use { it.readText() } }
                val err = runCatching { process.errorStream.bufferedReader().use { it.readText() } }.getOrDefault("")
                val finished = process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                if (!finished) {
                    Log.e(TAG, "Shizuku command timed out after ${COMMAND_TIMEOUT_SECONDS}s: $command")
                    return false
                }
                val exitCode = process.exitValue()
                if (exitCode != 0) {
                    Log.e(TAG, "Shizuku error ($exitCode): $err")
                }
                exitCode == 0
            } finally {
                process.destroy()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Shizuku execution failed", e)
            false
        }
    }
}
