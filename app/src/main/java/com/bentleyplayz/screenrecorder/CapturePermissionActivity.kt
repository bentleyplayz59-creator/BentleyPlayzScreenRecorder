package com.bentleyplayz.screenrecorder

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast

/**
 * Transparent trampoline used by the floating control. Android requires an
 * Activity to obtain MediaProjection consent, but the recorder UI is never shown.
 */
class CapturePermissionActivity : Activity() {
    private val captureRequest = 4201
    private val microphoneRequest = 4202
    private var waitingForMicrophone = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.setDimAmount(0f)
        window.attributes = window.attributes.apply {
            dimAmount = 0f
        }

        if (getSharedPreferences("recorder", MODE_PRIVATE).getBoolean("is_recording", false)) {
            Toast.makeText(this, "A recording is already running.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED) {
            waitingForMicrophone = true
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), microphoneRequest)
        } else {
            requestScreenCaptureConsent()
        }
    }

    private fun requestScreenCaptureConsent() {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(manager.createScreenCaptureIntent(), captureRequest)
    }

    @Deprecated("MediaProjection consent callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != captureRequest) return

        if (resultCode == RESULT_OK && data != null) {
            val serviceIntent = Intent(this, RecorderService::class.java)
                .setAction(RecorderService.ACTION_START)
                .putExtra(RecorderService.EXTRA_RESULT_CODE, resultCode)
                .putExtra(RecorderService.EXTRA_DATA, data)

            try {
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(serviceIntent)
                else startService(serviceIntent)
            } catch (error: Exception) {
                Toast.makeText(this, "Could not start the recorder: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Screen capture permission was not granted.", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != microphoneRequest || !waitingForMicrophone) return
        waitingForMicrophone = false
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestScreenCaptureConsent()
        } else {
            Toast.makeText(this, "Microphone permission is required for this recorder.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}