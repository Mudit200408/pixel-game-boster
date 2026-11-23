package com.pixel.gamebooster.overlay

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.pixel.gamebooster.R
import com.pixel.gamebooster.data.model.RecordAudioSource
import com.pixel.gamebooster.data.repository.SettingsRepository

/**
 * Transparent activity that invokes Android's official MediaProjection screen capture prompt.
 * Initiates system screen recording without requiring full app UI foregrounding.
 */
class ScreenRecordTrampolineActivity : Activity() {

    companion object {
        private const val TAG = "ScreenRecordTrampoline"
        private const val REQUEST_MEDIA_PROJECTION = 1001
        private const val REQUEST_RECORD_AUDIO = 1002

        fun launch(context: Context) {
            val intent = Intent(context, ScreenRecordTrampolineActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)

        val repo = SettingsRepository(applicationContext)
        val audioSource = RecordAudioSource.fromValue(repo.recordAudioSource.value)
        val needsMic = audioSource == RecordAudioSource.MIC || audioSource == RecordAudioSource.BOTH
        val hasMicPermission = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        if (needsMic && !hasMicPermission) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO)
        } else {
            requestScreenCapture()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO) {
            requestScreenCapture()
        }
    }

    private fun requestScreenCapture() {
        try {
            val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
            if (mgr != null) {
                val captureIntent = mgr.createScreenCaptureIntent()
                @Suppress("DEPRECATION")
                startActivityForResult(captureIntent, REQUEST_MEDIA_PROJECTION)
            } else {
                Toast.makeText(this, getString(R.string.screen_record_unsupported), Toast.LENGTH_SHORT).show()
                finish()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch screen capture intent", e)
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK && data != null) {
                // User authorized recording: start ScreenRecordService with MediaProjection token
                com.pixel.gamebooster.recording.ScreenRecordService.startRecording(this, resultCode, data)
            } else {
                Toast.makeText(this, getString(R.string.screen_record_cancelled), Toast.LENGTH_SHORT).show()
                GameOverlayManager.overlayViewModel?.setRecording(false)
            }
            finish()
        }
    }
}
