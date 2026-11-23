package com.pixel.gamebooster.overlay

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.pixel.gamebooster.utils.ShellUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Transparent activity that invokes Android's official MediaProjection screen capture prompt.
 * Initiates system screen recording without requiring full app UI foregrounding.
 */
class ScreenRecordTrampolineActivity : Activity() {

    companion object {
        private const val TAG = "ScreenRecordTrampoline"
        private const val REQUEST_MEDIA_PROJECTION = 1001

        fun launch(context: Context) {
            val intent = Intent(context, ScreenRecordTrampolineActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
            if (mgr != null) {
                val captureIntent = mgr.createScreenCaptureIntent()
                @Suppress("DEPRECATION")
                startActivityForResult(captureIntent, REQUEST_MEDIA_PROJECTION)
            } else {
                Toast.makeText(this, "Screen recording not supported on this device", Toast.LENGTH_SHORT).show()
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
            if (resultCode == RESULT_OK) {
                // User authorized recording: start system recording via shell screenrecord binary
                CoroutineScope(Dispatchers.IO).launch {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val outFile = "/sdcard/Movies/game_record_${timestamp}.mp4"
                    ShellUtils.runCommand(applicationContext, "mkdir -p /sdcard/Movies && nohup screenrecord --bit-rate 16000000 $outFile > /dev/null 2>&1 &")
                }
                Toast.makeText(this, "Screen recording started", Toast.LENGTH_SHORT).show()
                GameOverlayManager.overlayViewModel?.setRecording(true)
            } else {
                Toast.makeText(this, "Screen recording cancelled", Toast.LENGTH_SHORT).show()
                GameOverlayManager.overlayViewModel?.setRecording(false)
            }
            finish()
        }
    }
}
