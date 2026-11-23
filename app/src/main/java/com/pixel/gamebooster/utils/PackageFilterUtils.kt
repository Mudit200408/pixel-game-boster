package com.pixel.gamebooster.utils

import android.content.Context
import android.media.AudioManager
import android.view.inputmethod.InputMethodManager

object PackageFilterUtils {

    // Exact package names to ignore unconditionally
    val EXACT_IGNORED_PACKAGES = setOf(
        "android",
        "com.android.systemui",
        "com.google.android.gms",
        "com.android.vending",
        "com.google.android.inputmethod.latin",
        "dev.jason.com.google.android.inputmethod.latin",
        "com.android.pixeldisplayservice",
        "android.service.games",
        "com.google.android.play.games",
    )

    // Case-insensitive substring matches for overlay, phone, dialog, and keyboard windows
    val IGNORED_SUBSTRINGS = listOf(
        "systemui",
        "keyguard",
        "volume",
        "soundassistant",
        "dialer",
        "telecom",
        "phone",
        "incallui",
        "packageinstaller",
        "permissioncontroller",
        "displayservice",
        "pixeldisplay",
        "inputmethod",
        "keyboard",
        "honeyboard",
        "autofill",
    )

    // Prefixes that indicate transient system or game overlay services
    val IGNORED_PREFIXES = listOf(
        "com.android.inputmethod",
        "com.google.android.inputmethod",
        "android.service.games",
    )

    // Cached set of enabled IMEs (5-second TTL to avoid repeated IPC binder calls)
    @Volatile private var cachedImePackages: Set<String>? = null
    @Volatile private var cachedImeTimestamp: Long = 0L

    // Cached audio mode (100ms TTL to avoid repeated IPC binder calls)
    @Volatile private var cachedAudioMode: Int = AudioManager.MODE_NORMAL
    @Volatile private var cachedAudioModeTimestamp: Long = 0L

    fun isPackageIgnored(
        packageName: String?,
        context: Context? = null,
        configuredGamePackages: Set<String> = emptySet(),
    ): Boolean {
        if (packageName.isNullOrBlank()) return true

        // Immunity Rule: If the package is explicitly registered by the user as a game,
        // it must NEVER be ignored, even if its name contains a keyword like "keyboard" or "games".
        if (configuredGamePackages.contains(packageName)) return false

        // Check active call state via AudioManager mode (100ms TTL cache)
        if (context != null) {
            val now = System.currentTimeMillis()
            val audioMode = if (now - cachedAudioModeTimestamp > 100L) {
                runCatching {
                    (context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.mode
                        ?: AudioManager.MODE_NORMAL
                }.getOrDefault(AudioManager.MODE_NORMAL).also {
                    cachedAudioMode = it
                    cachedAudioModeTimestamp = now
                }
            } else {
                cachedAudioMode
            }
            if (audioMode == AudioManager.MODE_IN_CALL ||
                audioMode == AudioManager.MODE_IN_COMMUNICATION ||
                audioMode == AudioManager.MODE_RINGTONE
            ) {
                return true
            }
        }

        // 1. Exact match check
        if (EXACT_IGNORED_PACKAGES.contains(packageName)) return true

        // 2. Case-insensitive substring and prefix matching
        val lower = packageName.lowercase()
        if (IGNORED_SUBSTRINGS.any { lower.contains(it) }) return true
        if (IGNORED_PREFIXES.any { lower.startsWith(it) }) return true

        // 3. Dynamic IME lookup from InputMethodManager (if context available)
        if (context != null) {
            val now = System.currentTimeMillis()
            val imes = if (now - cachedImeTimestamp > 5000L || cachedImePackages == null) {
                runCatching {
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.enabledInputMethodList?.map { it.packageName }?.toSet() ?: emptySet()
                }.getOrDefault(emptySet()).also {
                    cachedImePackages = it
                    cachedImeTimestamp = now
                }
            } else {
                cachedImePackages ?: emptySet()
            }
            if (imes.contains(packageName)) return true
        }

        return false
    }
}
