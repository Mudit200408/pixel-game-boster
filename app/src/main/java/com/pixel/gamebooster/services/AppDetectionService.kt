package com.pixel.gamebooster.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.pixel.gamebooster.R
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.utils.GameDetectionUtils
import com.pixel.gamebooster.utils.GameModeManager
import com.pixel.gamebooster.utils.PackageFilterUtils

class AppDetectionService : Service() {
    private lateinit var settingsRepository: SettingsRepository
    private val handler = Handler(Looper.getMainLooper())
    private var isPolling = false
    private var lastPackageName: String? = null
    private var cachedForegroundPackage: String? = null

    companion object {
        private const val TAG = "PixelGameBooster:Detect"
        private const val CHANNEL_ID = "game_booster_detection_channel"
        private const val NOTIFICATION_ID = 2001
        private const val POLL_INTERVAL = 500L
        var isRunning = false
        var instance: AppDetectionService? = null
            private set

        fun start(context: Context) {
            val intent = Intent(context, AppDetectionService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AppDetectionService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        settingsRepository = SettingsRepository(this)
        GameModeManager.init(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        isRunning = true
        startPolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isPolling = false
        isRunning = false
        handler.removeCallbacksAndMessages(null)
    }

    fun resetLastPackage() {
        lastPackageName = null
        cachedForegroundPackage = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startPolling() {
        isPolling = true
        handler.post(object : Runnable {
            override fun run() {
                if (!isPolling) return
                if (settingsRepository.isBoosterEnabled.value) {
                    val currentPackage = getForegroundPackage()
                    val gameApps = settingsRepository.gameApps.value
                    if (currentPackage != null && !PackageFilterUtils.isPackageIgnored(currentPackage, this@AppDetectionService, gameApps)) {
                        val isGame = GameDetectionUtils.isGame(this@AppDetectionService, currentPackage, gameApps)
                        Log.d(TAG, "Detection poll: pkg=$currentPackage, isGame=$isGame, isModeActive=${GameModeManager.isGameModeActive}")

                        if (currentPackage == lastPackageName) {
                            if (!GameModeManager.isGameModeActive && isGame) {
                                GameModeManager.onGameForeground(
                                    this@AppDetectionService,
                                    currentPackage,
                                    settingsRepository,
                                )
                            } else if (GameModeManager.isGameModeActive && !isGame) {
                                GameModeManager.onGameBackground(
                                    this@AppDetectionService,
                                    GameModeManager.activeGamePackage ?: currentPackage,
                                    null,
                                    settingsRepository,
                                )
                            }
                        } else {
                            val oldPackage = lastPackageName
                            lastPackageName = currentPackage

                            if (isGame) {
                                GameModeManager.onGameForeground(
                                    this@AppDetectionService,
                                    currentPackage,
                                    settingsRepository,
                                )
                            } else if (oldPackage != null && GameDetectionUtils.isGame(this@AppDetectionService, oldPackage, gameApps)) {
                                GameModeManager.onGameBackground(
                                    this@AppDetectionService,
                                    oldPackage,
                                    currentPackage,
                                    settingsRepository,
                                )
                            } else if (GameModeManager.isGameModeActive && !isGame) {
                                GameModeManager.onGameBackground(
                                    this@AppDetectionService,
                                    GameModeManager.activeGamePackage ?: currentPackage,
                                    null,
                                    settingsRepository,
                                )
                            }
                        }
                    }
                }
                handler.postDelayed(this, POLL_INTERVAL)
            }
        })
    }

    private fun getForegroundPackage(): String? {
        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val time = System.currentTimeMillis()

        try {
            val events = usageStatsManager.queryEvents(time - 1000 * 60 * 5, time)
            val event = UsageEvents.Event()
            var lastResumedPackage: String? = null
            var lastEventTime = 0L
            val packagePausedOrStoppedTime = mutableMapOf<String, Long>()

            val gameApps = settingsRepository.gameApps.value
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val type = event.eventType
                val pkg = event.packageName ?: continue
                if (PackageFilterUtils.isPackageIgnored(pkg, this, gameApps)) continue

                if (type == UsageEvents.Event.ACTIVITY_RESUMED) {
                    if (event.timeStamp >= lastEventTime) {
                        lastResumedPackage = pkg
                        lastEventTime = event.timeStamp
                    }
                } else if (type == UsageEvents.Event.ACTIVITY_PAUSED || type == UsageEvents.Event.ACTIVITY_STOPPED) {
                    packagePausedOrStoppedTime[pkg] = maxOf(packagePausedOrStoppedTime[pkg] ?: 0L, event.timeStamp)
                }
            }

            if (lastResumedPackage != null) {
                val pausedTime = packagePausedOrStoppedTime[lastResumedPackage] ?: 0L
                if (pausedTime <= lastEventTime) {
                    cachedForegroundPackage = lastResumedPackage
                    return lastResumedPackage
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying usage events", e)
        }

        return cachedForegroundPackage
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.detection_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.detection_channel_desc)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.detection_notif_title))
            .setContentText(getString(R.string.detection_notif_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
