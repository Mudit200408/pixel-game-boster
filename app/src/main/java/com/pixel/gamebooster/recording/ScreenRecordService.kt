package com.pixel.gamebooster.recording

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.pixel.gamebooster.MainActivity
import com.pixel.gamebooster.R
import com.pixel.gamebooster.data.model.RecordAudioSource
import com.pixel.gamebooster.data.model.RecordResolution
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.utils.ShellUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Foreground Service responsible for in-game screen recording with high-quality audio options.
 * Supports Mute, Microphone, Device Audio (AudioPlaybackCapture), and Device + Mic mixed capture.
 */
class ScreenRecordService : Service() {

    companion object {
        private const val TAG = "ScreenRecordService"
        private const val NOTIFICATION_ID = 2001
        private const val COMPLETED_NOTIFICATION_ID = 2002
        private const val CHANNEL_ID = "screen_recording_channel"

        const val ACTION_START = "com.pixel.gamebooster.recording.START"
        const val ACTION_STOP = "com.pixel.gamebooster.recording.STOP"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_DATA_INTENT = "extra_data_intent"

        val isRecording = MutableStateFlow(false)
        val recordingDurationSeconds = MutableStateFlow(0)

        fun startRecording(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, ScreenRecordService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_DATA_INTENT, data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopRecording(context: Context) {
            val intent = Intent(context, ScreenRecordService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var durationJob: Job? = null

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var internalAudioRecorder: InternalAudioRecorder? = null

    private var finalOutputFile: File? = null
    private var tempVideoFile: File? = null
    private var tempAudioFile: File? = null

    private var activeAudioSource: RecordAudioSource = RecordAudioSource.DEVICE
    private var originalShowTouches: Int = 0
    private var didChangeShowTouches: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val dataIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_DATA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_DATA_INTENT)
                }

                if (resultCode != 0 && dataIntent != null) {
                    startRecordingInternal(resultCode, dataIntent)
                } else {
                    Log.e(TAG, "Missing MediaProjection data, aborting")
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                stopRecordingInternal()
            }
        }
        return START_NOT_STICKY
    }

    private fun startRecordingInternal(resultCode: Int, data: Intent) {
        val repo = SettingsRepository(applicationContext)
        val selectedAudio = RecordAudioSource.fromValue(repo.recordAudioSource.value)
        val hasMicPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        // If mic is chosen but permission is not granted, gracefully fall back to device audio or mute
        activeAudioSource = if ((selectedAudio == RecordAudioSource.MIC || selectedAudio == RecordAudioSource.BOTH) && !hasMicPermission) {
            Log.w(TAG, "RECORD_AUDIO permission missing; falling back to device audio / mute")
            if (selectedAudio == RecordAudioSource.BOTH) RecordAudioSource.DEVICE else RecordAudioSource.MUTE
        } else {
            selectedAudio
        }

        val resolution = RecordResolution.fromValue(repo.recordResolution.value)
        val showTouches = repo.recordShowTouches.value

        // Only request FOREGROUND_SERVICE_TYPE_MICROPHONE if mic is needed AND permission is granted
        val needsMicFgs = (activeAudioSource == RecordAudioSource.MIC || activeAudioSource == RecordAudioSource.BOTH) && hasMicPermission

        // 1. Enter Foreground immediately (mandatory on Android 14+ before MediaProjection operations)
        try {
            val initialNotification = buildRecordingNotification(0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    if (needsMicFgs) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    } else {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                    }
                } else {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                }
                startForeground(NOTIFICATION_ID, initialNotification, foregroundServiceType)
            } else {
                startForeground(NOTIFICATION_ID, initialNotification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service for screen recording", e)
            cleanup()
            stopSelf()
            return
        }

        // 2. Prepare file locations
        val moviesDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "PixelGameBooster"
        ).apply { mkdirs() }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        finalOutputFile = File(moviesDir, "game_record_$timestamp.mp4")

        val isMuxingRequired = activeAudioSource == RecordAudioSource.DEVICE || activeAudioSource == RecordAudioSource.BOTH
        val targetVideoFile = if (isMuxingRequired) {
            File(cacheDir, "temp_vid_$timestamp.mp4").also { tempVideoFile = it }
        } else {
            finalOutputFile!!
        }

        // 3. Configure MediaRecorder
        try {
            val displayMetrics = resources.displayMetrics
            val densityDpi = displayMetrics.densityDpi
            val width = if (resolution.width > 0) resolution.width else displayMetrics.widthPixels
            val height = if (resolution.height > 0) resolution.height else displayMetrics.heightPixels

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                if (activeAudioSource == RecordAudioSource.MIC) {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                }
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

                if (activeAudioSource == RecordAudioSource.MIC) {
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(192000)
                    setAudioSamplingRate(44100)
                }

                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoSize(width, height)
                setVideoFrameRate(resolution.fps)
                setVideoEncodingBitRate(resolution.bitrate)
                setOutputFile(targetVideoFile.absolutePath)
                prepare()
            }

            // 4. Initialize MediaProjection & VirtualDisplay
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val projection = projectionManager.getMediaProjection(resultCode, data)
            if (projection == null) {
                Log.e(TAG, "Failed to obtain MediaProjection instance")
                cleanup()
                stopSelf()
                return
            }
            mediaProjection = projection

            // Android 14+ requires registering callback BEFORE creating virtual display
            projection.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.i(TAG, "MediaProjection stopped by system")
                    stopRecordingInternal()
                }
            }, null)

            virtualDisplay = projection.createVirtualDisplay(
                "PixelGameBoosterDisplay",
                width,
                height,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder?.surface,
                null,
                null
            )

            mediaRecorder?.start()

            // 5. Start Internal Audio Recording if requested
            if (isMuxingRequired) {
                tempAudioFile = File(cacheDir, "temp_aud_$timestamp.mp4")
                internalAudioRecorder = InternalAudioRecorder(
                    mediaProjection = projection,
                    outputFile = tempAudioFile!!,
                    includeMic = activeAudioSource == RecordAudioSource.BOTH,
                ).apply {
                    start()
                }
            }

            // 6. Handle visual touches option
            if (showTouches) {
                try {
                    originalShowTouches = Settings.System.getInt(contentResolver, "show_touches", 0)
                    ShellUtils.runCommand(this, "settings put system show_touches 1")
                    didChangeShowTouches = true
                } catch (e: Exception) {
                    Log.w(TAG, "Unable to enable show_touches", e)
                }
            }

            isRecording.value = true
            recordingDurationSeconds.value = 0
            startDurationCounter()
            Toast.makeText(this, "Screen recording started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start screen recording pipeline", e)
            Toast.makeText(this, "Failed to start recording: ${e.message}", Toast.LENGTH_LONG).show()
            cleanup()
            stopSelf()
        }
    }

    private fun stopRecordingInternal() {
        if (!isRecording.value && mediaRecorder == null) {
            stopSelf()
            return
        }

        isRecording.value = false
        durationJob?.cancel()

        // Revert show touches
        if (didChangeShowTouches) {
            try {
                ShellUtils.runCommand(this, "settings put system show_touches $originalShowTouches")
            } catch (e: Exception) {
                Log.w(TAG, "Error reverting show_touches", e)
            }
        }

        // Stop internal audio recorder
        internalAudioRecorder?.stop()
        internalAudioRecorder = null

        // Stop video recorder
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping mediaRecorder", e)
        }
        mediaRecorder?.release()
        mediaRecorder = null

        virtualDisplay?.release()
        virtualDisplay = null

        mediaProjection?.stop()
        mediaProjection = null

        // Post-Mux if audio was recorded separately
        val targetFile = finalOutputFile
        val tempVid = tempVideoFile
        val tempAud = tempAudioFile

        serviceScope.launch(Dispatchers.IO) {
            if (targetFile != null) {
                if (tempVid != null && tempAud != null && tempVid.exists() && tempAud.exists()) {
                    ScreenRecordingMuxer.mux(tempVid, tempAud, targetFile)
                }

                if (targetFile.exists()) {
                    MediaScannerConnection.scanFile(
                        applicationContext,
                        arrayOf(targetFile.absolutePath),
                        arrayOf("video/mp4"),
                        null
                    )
                    launch(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "Recording saved: ${targetFile.name}", Toast.LENGTH_SHORT).show()
                        showCompletedNotification(targetFile)
                    }
                }
            }
            cleanup()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun cleanup() {
        tempVideoFile?.delete()
        tempAudioFile?.delete()
        isRecording.value = false
        recordingDurationSeconds.value = 0
    }

    private fun startDurationCounter() {
        durationJob = serviceScope.launch {
            var seconds = 0
            while (isActive) {
                delay(1000)
                seconds++
                recordingDurationSeconds.value = seconds
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, buildRecordingNotification(seconds))
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "In-Game Screen Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Status and controls for in-game screen capture"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildRecordingNotification(seconds: Int): Notification {
        val stopIntent = Intent(this, ScreenRecordService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        val timeString = String.format(Locale.US, "%02d:%02d", minutes, remainingSeconds)

        val audioLabel = when (activeAudioSource) {
            RecordAudioSource.MUTE -> "Mute"
            RecordAudioSource.MIC -> "Mic"
            RecordAudioSource.DEVICE -> "Device Audio"
            RecordAudioSource.BOTH -> "Game + Mic"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Recording Screen • $timeString")
            .setContentText("Capturing gameplay ($audioLabel)")
            .setSmallIcon(android.R.drawable.presence_video_busy)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Stop Recording", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showCompletedNotification(file: File) {
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            val uri = FileProvider.getUriForFile(
                applicationContext,
                "${applicationContext.packageName}.provider",
                file
            )
            setDataAndType(uri, "video/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val viewPendingIntent = PendingIntent.getActivity(
            this,
            2,
            viewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Recording Saved")
            .setContentText(file.name)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setContentIntent(viewPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(COMPLETED_NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecordingInternal()
    }
}
